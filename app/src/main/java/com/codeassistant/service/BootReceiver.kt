package com.codeassistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.codeassistant.CodeAssistantApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            Log.d("BootReceiver", "Device booted, rescheduling tasks...")
            
            val app = context.applicationContext as CodeAssistantApp
            app.taskScheduler?.scheduleAllTasks()
        }
    }
}