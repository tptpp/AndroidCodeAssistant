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
import com.codeassistant.data.model.ScheduledTask
import com.codeassistant.data.model.TaskStatus
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "定时任务通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "定时任务执行通知"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // 调度所有活跃任务
    fun scheduleAllTasks() {
        scope.launch {
            val tasks = taskDao.getTasksByStatus(TaskStatus.ACTIVE).first()
            tasks.forEach { task ->
                scheduleTask(task)
            }
        }
    }
    
    // 调度单个任务
    fun scheduleTask(task: ScheduledTask) {
        if (task.status != TaskStatus.ACTIVE) {
            cancelTask(task)
            return
        }
        
        val nextRun = task.calculateNextRunTime()
        val intent = Intent(context, TaskExecutionReceiver::class.java).apply {
            action = ACTION_EXECUTE_TASK
            putExtra(EXTRA_TASK_ID, task.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextRun,
                pendingIntent
            )
            
            // 更新下次执行时间
            scope.launch {
                taskDao.updateTask(task.copy(nextRunAt = nextRun))
            }
            
            Log.d("TaskScheduler", "Task ${task.name} scheduled for ${java.util.Date(nextRun)}")
        } catch (e: Exception) {
            Log.e("TaskScheduler", "Failed to schedule task: ${task.name}", e)
        }
    }
    
    // 取消任务
    fun cancelTask(task: ScheduledTask) {
        val intent = Intent(context, TaskExecutionReceiver::class.java).apply {
            action = ACTION_EXECUTE_TASK
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    // 执行任务
    fun executeTask(taskId: Long) {
        scope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            
            Log.d("TaskScheduler", "Executing task: ${task.name}")
            
            try {
                // 获取模型配置
                val config = settingsRepository.modelConfig.first()
                
                // 构建消息
                val messages = listOf(
                    com.codeassistant.data.model.Message(
                        conversationId = 0,
                        role = com.codeassistant.data.model.MessageRole.USER,
                        content = task.prompt
                    )
                )
                
                // 调用 AI
                val result = chatRepository.sendMessageSimple(messages, config)
                
                withContext(Dispatchers.Main) {
                    result.onSuccess { response ->
                        // 保存执行记录
                        val execution = com.codeassistant.data.model.TaskExecution(
                            taskId = task.id,
                            taskName = task.name,
                            prompt = task.prompt,
                            response = response,
                            success = true
                        )
                        
                        // 发送通知
                        showNotification(task.name, response.take(100) + "...")
                        
                        // 更新任务状态
                        val updatedTask = task.copy(
                            lastRunAt = System.currentTimeMillis(),
                            nextRunAt = task.calculateNextRunTime()
                        )
                        taskDao.updateTask(updatedTask)
                        
                        // 重新调度
                        scheduleTask(updatedTask)
                    }.onFailure { error ->
                        // 保存失败记录
                        val execution = com.codeassistant.data.model.TaskExecution(
                            taskId = task.id,
                            taskName = task.name,
                            prompt = task.prompt,
                            response = "",
                            success = false,
                            errorMessage = error.message
                        )
                        
                        showNotification(task.name, "执行失败: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskScheduler", "Task execution failed", e)
                withContext(Dispatchers.Main) {
                    showNotification(task.name, "执行出错: ${e.message}")
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