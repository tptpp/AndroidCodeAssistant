package com.codeassistant.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.*

// OpenAI 兼容的 API 请求/响应格式

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage? = null
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessageResponse,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class ChatMessageResponse(
    val role: String,
    val content: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

// 流式响应
data class StreamResponse(
    val id: String,
    val choices: List<StreamChoice>
)

data class StreamChoice(
    val index: Int,
    val delta: StreamDelta,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class StreamDelta(
    val role: String? = null,
    val content: String? = null
)

// API 接口
interface ChatApiService {
    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
    
    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    @Streaming
    suspend fun chatStream(@Body request: ChatRequest): ResponseBody
    
    @GET("models")
    suspend fun listModels(): ModelsResponse
}

data class ModelsResponse(
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val owned_by: String
)