package com.codeassistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codeassistant.data.model.*

@Database(
    entities = [
        Message::class,
        Conversation::class,
        Task::class,
        TaskExecution::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun taskDao(): TaskDao
    abstract fun taskExecutionDao(): TaskExecutionDao
}