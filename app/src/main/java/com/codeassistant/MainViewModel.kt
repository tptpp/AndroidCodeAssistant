package com.codeassistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeassistant.data.database.AppDatabase
import com.codeassistant.data.model.*
import com.codeassistant.data.repository.ChatRepository
import com.codeassistant.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as CodeAssistantApp
    private val database = app.database
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val taskDao = database.taskDao()
    private val taskExecutionDao = database.taskExecutionDao()
    private val chatRepository = app.chatRepository
    private val settingsRepository = app.settingsRepository
    
    // ========== 对话相关 ==========
    
    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId
    
    val conversations: Flow<List<Conversation>> = conversationDao.getAllConversations()
    
    val messages: Flow<List<Message>> = _currentConversationId.flatMapLatest { id ->
        if (id != null) {
            messageDao.getMessagesByConversation(id)
        } else {
            flowOf(emptyList())
        }
    }
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // ========== 设置相关 ==========
    
    val modelConfig: Flow<ModelConfig> = settingsRepository.modelConfig
    
    // ========== 任务相关 ==========
    
    val tasks: Flow<List<ScheduledTask>> = taskDao.getAllTasks()
    
    val executions: Flow<List<TaskExecution>> = taskExecutionDao.getAllExecutions()
    
    // ========== 对话方法 ==========
    
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
                role = MessageRole.USER
            )
            messageDao.insertMessage(userMessage)
            
            // 获取当前配置
            val config = settingsRepository.modelConfig.first()
            
            // 获取所有消息作为上下文
            val allMessages = messageDao.getMessagesByConversation(currentConvId).first()
            
            // 发送到 API
            val result = chatRepository.sendMessageSimple(
                messages = allMessages,
                config = config
            )
            
            // 保存 AI 响应
            result.onSuccess { content ->
                val assistantMessage = Message(
                    conversationId = currentConvId,
                    content = content,
                    role = MessageRole.ASSISTANT
                )
                messageDao.insertMessage(assistantMessage)
                
                // 更新对话标题
                val conv = conversationDao.getConversationById(currentConvId)
                if (conv != null && conv.title == "新对话") {
                    conversationDao.updateConversation(conv.copy(title = text.take(30)))
                }
            }
            
            _isLoading.value = false
        }
    }
    
    // ========== 设置方法 ==========
    
    fun saveModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            settingsRepository.saveModelConfig(config)
            chatRepository.updateConfig(config)
        }
    }
    
    // ========== 任务方法 ==========
    
    fun saveTask(task: ScheduledTask) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val id = taskDao.insertTask(task)
                val newTask = task.copy(id = id)
                app.taskScheduler?.scheduleTask(newTask)
            } else {
                taskDao.updateTask(task)
                app.taskScheduler?.scheduleTask(task)
            }
        }
    }
    
    fun toggleTask(task: ScheduledTask) {
        viewModelScope.launch {
            val newStatus = if (task.status == TaskStatus.ACTIVE) {
                TaskStatus.PAUSED
            } else {
                TaskStatus.ACTIVE
            }
            val updatedTask = task.copy(status = newStatus)
            taskDao.updateTask(updatedTask)
            
            if (newStatus == TaskStatus.ACTIVE) {
                app.taskScheduler?.scheduleTask(updatedTask)
            } else {
                app.taskScheduler?.cancelTask(updatedTask)
            }
        }
    }
    
    fun deleteTask(task: ScheduledTask) {
        viewModelScope.launch {
            app.taskScheduler?.cancelTask(task)
            taskDao.deleteTask(task)
            taskExecutionDao.deleteExecutionsByTask(task.id)
        }
    }
}