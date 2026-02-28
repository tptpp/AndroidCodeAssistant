package com.codeassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeassistant.ui.chat.*
import com.codeassistant.ui.settings.SettingsScreen
import com.codeassistant.ui.theme.CodeAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeAssistantTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val modelConfig by viewModel.modelConfig.collectAsState(
        initial = com.codeassistant.data.model.ModelConfig(
            name = "OpenAI",
            provider = "openai",
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4"
        )
    )
    
    var showSettings by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    
    if (showSettings) {
        SettingsScreen(
            currentConfig = modelConfig,
            onSave = { config ->
                // 保存配置
                showSettings = false
            },
            onBack = { showSettings = false }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ChatDrawer(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onSelectConversation = { viewModel.selectConversation(it) },
                    onNewConversation = { viewModel.createNewConversation() },
                    onDeleteConversation = { viewModel.deleteConversation(it) }
                )
            }
        ) {
            ChatScreen(
                messages = messages,
                isLoading = isLoading,
                currentInput = inputText,
                onInputChange = { viewModel.setInputText(it) },
                onSend = { viewModel.sendMessage() },
                onOpenDrawer = { /* drawerState.open() */ },
                onOpenSettings = { showSettings = true }
            )
        }
    }
}