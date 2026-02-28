package com.codeassistant.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codeassistant.R
import com.codeassistant.data.database.TaskDao
import com.codeassistant.data.model.*
import com.codeassistant.data.repository.ChatRepository
import com.codeassistant.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TaskScheduler(
    private val context: Context,
    private val taskDao: TaskDao,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val ACTION_EXECUTE_TASK = "com.codeassistant.EXECUTE_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val CHANNEL_ID = "task_notifications"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "任务通知", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun scheduleAllTasks() {
        scope.launch {
            taskDao.getTasksByStatus(TaskStatus.ACTIVE).first().forEach { scheduleTask(it) }
        }
    }
    
    fun scheduleTask(task: Task) {
        if (task.status != TaskStatus.ACTIVE) {
            cancelTask(task)
            return
        }
        
        val intent = Intent(context, TaskExecutionReceiver::class.java).apply {
            action = ACTION_EXECUTE_TASK
            putExtra(EXTRA_TASK_ID, task.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextRun = calculateNextRunTime(task)
        
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRun, pendingIntent)
            scope.launch { taskDao.updateTask(task.copy(nextRunAt = nextRun)) }
            Log.d("TaskScheduler", "Task ${task.title} scheduled")
        } catch (e: Exception) {
            Log.e("TaskScheduler", "Failed to schedule task", e)
        }
    }
    
    private fun calculateNextRunTime(task: Task): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = task.scheduledTime
        
        val now = System.currentTimeMillis()
        
        when (task.type) {
            TaskType.ONE_TIME -> {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                }
            }
            TaskType.SCHEDULED -> {
                when (task.frequency) {
                    ScheduleFrequency.DAILY -> {
                        while (calendar.timeInMillis <= now) {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    ScheduleFrequency.WEEKLY -> {
                        val days = task.daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
                        while (calendar.timeInMillis <= now || 
                               !days.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    ScheduleFrequency.HOURLY -> {
                        while (calendar.timeInMillis <= now) {
                            calendar.add(Calendar.HOUR_OF_DAY, 1)
                        }
                    }
                    null -> {}
                }
            }
        }
        
        return calendar.timeInMillis
    }
    
    fun cancelTask(task: Task) {
        val intent = Intent(context, TaskExecutionReceiver::class.java).apply { action = ACTION_EXECUTE_TASK }
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    fun executeTask(taskId: Long) {
        scope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            
            try {
                val config = settingsRepository.modelConfig.first()
                val messages = listOf(Message(conversationId = 0, role = MessageRole.USER, content = task.prompt))
                val result = chatRepository.sendMessageSimple(messages, config)
                
                withContext(Dispatchers.Main) {
                    result.onSuccess { response ->
                        // 保存执行记录
                        val execution = TaskExecution(
                            taskId = task.id,
                            taskTitle = task.title,
                            prompt = task.prompt,
                            response = response,
                            success = true
                        )
                        
                        showNotification(task.title, response.take(100) + "...")
                        
                        // 更新任务状态
                        if (task.type == TaskType.ONE_TIME) {
                            taskDao.updateTask(task.copy(status = TaskStatus.COMPLETED))
                        } else {
                            val updated = task.copy(lastRunAt = System.currentTimeMillis())
                            taskDao.updateTask(updated)
                            scheduleTask(updated)
                        }
                    }.onFailure { error ->
                        showNotification(task.title, "执行失败: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskScheduler", "Task execution failed", e)
                withContext(Dispatchers.Main) {
                    showNotification(task.title, "执行出错: ${e.message}")
                }
            }
        }
    }
    
    private fun showNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}