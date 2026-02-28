package com.codeassistant.data.repository

import com.codeassistant.data.api.*
import com.codeassistant.data.model.Message
import com.codeassistant.data.model.MessageRole
import com.codeassistant.data.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepository {
    
    private var currentConfig: ModelConfig? = null
    private var apiService: ChatApiService? = null
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    fun updateConfig(config: ModelConfig) {
        if (currentConfig != config) {
            currentConfig = config
            apiService = Retrofit.Builder()
                .baseUrl(config.baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ChatApiService::class.java)
        }
    }
    
    suspend fun sendMessageSimple(
        messages: List<Message>,
        config: ModelConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            updateConfig(config)
            
            val apiMessages = messages.map { msg ->
                ChatMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
            }
            
            val request = ChatRequest(
                model = config.model,
                messages = apiMessages,
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stream = false
            )
            
            val service = apiService ?: return@withContext Result.failure(Exception("API not configured"))
            val response = service.chat(request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                Result.success(content)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}