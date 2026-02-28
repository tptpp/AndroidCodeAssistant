package com.codeassistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeassistant.data.database.ConversationDao
import com.codeassistant.data.database.MessageDao
import com.codeassistant.data.model.Conversation
import com.codeassistant.data.model.Message
import com.codeassistant.data.model.MessageRole
import com.codeassistant.data.model.ModelConfig
import com.codeassistant.data.repository.ChatRepository
import com.codeassistant.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId
    
    val conversations: Flow<List<Conversation>> = conversationDao.getAllConversations()
    
    val messages: Flow<List<Message>> = _currentConversationId.flatMapLatest { id ->
        if (id != null) messageDao.getMessagesByConversation(id) else flowOf(emptyList())
    }
    
    val modelConfig: Flow<ModelConfig> = settingsRepository.modelConfig
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun setInputText(text: String) { _inputText.value = text }
    
    fun selectConversation(id: Long) { _currentConversationId.value = id }
    
    fun createNewConversation() {
        viewModelScope.launch {
            val id = conversationDao.insertConversation(Conversation(title = "新对话"))
            _currentConversationId.value = id
        }
    }
    
    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            messageDao.deleteMessagesByConversation(id)
            conversationDao.deleteConversationById(id)
            if (_currentConversationId.value == id) _currentConversationId.value = null
        }
    }
    
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _inputText.value = ""
            
            val convId = _currentConversationId.value ?: run {
                conversationDao.insertConversation(Conversation(title = text.take(30)))
            }
            
            messageDao.insertMessage(Message(conversationId = convId, content = text, role = MessageRole.USER))
            
            val config = settingsRepository.modelConfig.first()
            val allMessages = messageDao.getMessagesByConversation(convId).first()
            val result = chatRepository.sendMessageSimple(allMessages, config)
            
            result.onSuccess { content ->
                messageDao.insertMessage(Message(conversationId = convId, content = content, role = MessageRole.ASSISTANT))
            }
            
            _isLoading.value = false
        }
    }
}