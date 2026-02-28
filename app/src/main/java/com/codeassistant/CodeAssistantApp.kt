package com.codeassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.codeassistant.data.database.AppDatabase
import com.codeassistant.data.repository.ChatRepository
import com.codeassistant.data.repository.SettingsRepository
import com.codeassistant.service.TaskScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CodeAssistantApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    lateinit var database: AppDatabase
        private set
    
    lateinit var settingsRepository: SettingsRepository
        private set
    
    lateinit var chatRepository: ChatRepository
        private set
    
    var taskScheduler: TaskScheduler? = null
        private set
    
    override fun onCreate() {
        super.onCreate()
        Log.d("CodeAssistantApp", "Application starting...")
        
        // 初始化数据库
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "code_assistant_db"
        )
            .fallbackToDestructiveMigration()
            .build()
        
        // 初始化仓库
        settingsRepository = SettingsRepository(applicationContext)
        chatRepository = ChatRepository()
        
        // 初始化任务调度器
        taskScheduler = TaskScheduler(
            context = applicationContext,
            taskDao = database.taskDao(),
            chatRepository = chatRepository,
            settingsRepository = settingsRepository,
            scope = applicationScope
        )
        
        // 创建通知渠道
        createNotificationChannels()
        
        // 启动时调度所有活跃任务
        applicationScope.launch {
            try {
                val config = settingsRepository.modelConfig.first()
                chatRepository.updateConfig(config)
                taskScheduler?.scheduleAllTasks()
                Log.d("CodeAssistantApp", "Tasks scheduled successfully")
            } catch (e: Exception) {
                Log.e("CodeAssistantApp", "Failed to initialize", e)
            }
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                TaskScheduler.CHANNEL_ID,
                "定时任务",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "定时任务执行通知"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(taskChannel)
        }
    }
}