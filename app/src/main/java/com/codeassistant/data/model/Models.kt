package com.codeassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// 消息角色
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

// 对话消息
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 对话
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// API 配置
data class ModelConfig(
    val name: String,
    val provider: String,
    val baseUrl: String,
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096
)

// 预设模型配置
object PresetModels {
    val OPENAI = ModelConfig(
        name = "OpenAI",
        provider = "openai",
        baseUrl = "https://api.openai.com/v1",
        model = "gpt-4"
    )
    
    val ANTHROPIC = ModelConfig(
        name = "Anthropic",
        provider = "anthropic",
        baseUrl = "https://api.anthropic.com/v1",
        model = "claude-3-opus-20240229"
    )
    
    val CUSTOM = ModelConfig(
        name = "自定义",
        provider = "custom",
        baseUrl = "",
        model = ""
    )
    
    val presets = listOf(OPENAI, ANTHROPIC, CUSTOM)
}