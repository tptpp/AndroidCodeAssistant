package com.codeassistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codeassistant.data.model.Conversation
import com.codeassistant.data.model.Message

@Database(
    entities = [Message::class, Conversation::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}