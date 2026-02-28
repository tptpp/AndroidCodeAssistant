package com.codeassistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeassistant.data.database.ConversationDao
import com.codeassistant.data.database.MessageDao
import com.codeassistant.data.model.Conversation
import com.codeassistant.data.model.Message
import com.codeassistant.data.model.ModelConfig
import com.codeassistant.data.repository.ChatRepository
import com.codeassistant.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // 当前对话ID
    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId
    
    // 所有对话
    val conversations: Flow<List<Conversation>> = conversationDao.getAllConversations()
    
    // 当前对话的消息
    val messages: Flow<List<Message>> = _currentConversationId.flatMapLatest { id ->
        if (id != null) {
            messageDao.getMessagesByConversation(id)
        } else {
            flowOf(emptyList())
        }
    }
    
    // 当前模型配置
    val modelConfig: Flow<ModelConfig> = settingsRepository.modelConfig
    
    // 输入状态
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // 当前响应（流式）
    private val _currentResponse = MutableStateFlow("")
    
    fun setInputText(text: String) {
        _inputText.value = text
    }
    
    fun selectConversation(id: Long) {
        _currentConversationId.value = id
    }
    
    fun createNewConversation() {
        viewModelScope.launch {
            val conversation = Conversation(title = "新对话")
            val id = conversationDao.insertConversation(conversation)
            _currentConversationId.value = id
        }
    }
    
    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            messageDao.deleteMessagesByConversation(id)
            conversationDao.deleteConversationById(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
            }
        }
    }
    
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isLoading.value) return
        
        val conversationId = _currentConversationId.value
        
        viewModelScope.launch {
            _isLoading.value = true
            _inputText.value = ""
            
            // 如果没有当前对话，创建一个新的
            val currentConvId = conversationId ?: run {
                val newConv = Conversation(title = text.take(30))
                conversationDao.insertConversation(newConv)
            }
            
            // 保存用户消息
            val userMessage = Message(
                conversationId = currentConvId,
                content = text,
                role = com.codeassistant.data.model.MessageRole.USER
            )
            messageDao.insertMessage(userMessage)
            
            // 获取当前配置
            val config = settingsRepository.modelConfig.first()
            
            // 获取所有消息作为上下文
            val allMessages = messageDao.getMessagesByConversation(currentConvId).first()
            
            // 发送到 API
            val result = chatRepository.sendMessage(
                messages = allMessages,
                config = config
            ) { chunk ->
                _currentResponse.value += chunk
            }
            
            // 保存 AI 响应
            result.onSuccess { content ->
                val assistantMessage = Message(
                    conversationId = currentConvId,
                    content = content,
                    role = com.codeassistant.data.model.MessageRole.ASSISTANT
                )
                messageDao.insertMessage(assistantMessage)
                
                // 更新对话标题
                val conv = conversationDao.getConversationById(currentConvId)
                if (conv != null && conv.title == "新对话") {
                    conversationDao.updateConversation(conv.copy(title = text.take(30)))
                }
            }
            
            _currentResponse.value = ""
            _isLoading.value = false
        }
    }
}