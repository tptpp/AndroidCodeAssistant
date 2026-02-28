package com.codeassistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.codeassistant.CodeAssistantApp

class TaskExecutionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == TaskScheduler.ACTION_EXECUTE_TASK) {
            val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1)
            if (taskId > 0 && context != null) {
                Log.d("TaskExecutionReceiver", "Received task execution request for task: $taskId")
                
                // 获取 Application 并触发任务执行
                val app = context.applicationContext as CodeAssistantApp
                app.taskScheduler?.executeTask(taskId)
            }
        }
    }
}