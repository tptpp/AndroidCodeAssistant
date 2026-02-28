package com.codeassistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeassistant.data.database.AppDatabase
import com.codeassistant.data.model.*
import com.codeassistant.data.repository.ChatRepository
import com.codeassistant.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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
        if (id != null) messageDao.getMessagesByConversation(id) else flowOf(emptyList())
    }
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // ========== 设置相关 ==========
    
    val modelConfig: Flow<ModelConfig> = settingsRepository.modelConfig
    
    // ========== 任务相关 ==========
    
    val tasks: Flow<List<Task>> = taskDao.getAllTasks()
    
    val executions: Flow<List<TaskExecution>> = taskExecutionDao.getAllExecutions()
    
    // ========== 对话方法 ==========
    
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
    
    // ========== 设置方法 ==========
    
    fun saveModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            settingsRepository.saveModelConfig(config)
            chatRepository.updateConfig(config)
        }
    }
    
    // ========== 任务方法 ==========
    
    fun saveTask(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val id = taskDao.insertTask(task)
                app.taskScheduler?.scheduleTask(task.copy(id = id))
            } else {
                taskDao.updateTask(task)
                app.taskScheduler?.scheduleTask(task)
            }
        }
    }
    
    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val newStatus = when (task.status) {
                TaskStatus.ACTIVE -> TaskStatus.PAUSED
                else -> TaskStatus.ACTIVE
            }
            val updated = task.copy(status = newStatus)
            taskDao.updateTask(updated)
            
            if (newStatus == TaskStatus.ACTIVE) app.taskScheduler?.scheduleTask(updated)
            else app.taskScheduler?.cancelTask(updated)
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            app.taskScheduler?.cancelTask(task)
            taskDao.deleteTask(task)
            taskExecutionDao.deleteExecutionsByTask(task.id)
        }
    }
    
    // 快速创建任务（从输入框）
    fun quickCreateTask(input: String) {
        viewModelScope.launch {
            // 简单解析：默认创建一次性任务
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, 1) // 默认1小时后
            
            val task = Task(
                title = input.take(30),
                prompt = input,
                type = TaskType.ONE_TIME,
                scheduledTime = calendar.timeInMillis,
                source = "quick"
            )
            
            val id = taskDao.insertTask(task)
            app.taskScheduler?.scheduleTask(task.copy(id = id))
        }
    }
}