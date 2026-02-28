package com.codeassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codeassistant.data.model.ModelConfig
import com.codeassistant.data.model.ScheduledTask
import com.codeassistant.data.model.TaskExecution
import com.codeassistant.ui.chat.*
import com.codeassistant.ui.settings.SettingsScreen
import com.codeassistant.ui.tasks.*
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

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Chat : Screen("chat", "对话", Icons.Default.Chat)
    object Tasks : Screen("tasks", "定时任务", Icons.Default.Schedule)
    object History : Screen("history", "执行历史", Icons.Default.History)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val modelConfig by viewModel.modelConfig.collectAsState(
        initial = ModelConfig(
            name = "OpenAI",
            provider = "openai",
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4"
        )
    )
    
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val executions by viewModel.executions.collectAsState(initial = emptyList())
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val screens = listOf(Screen.Chat, Screen.Tasks, Screen.History, Screen.Settings)
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationRoute ?: screen.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(padding)
        ) {
            // 对话页面
            composable(Screen.Chat.route) {
                ChatScreen(
                    messages = messages,
                    isLoading = isLoading,
                    currentInput = inputText,
                    onInputChange = { viewModel.setInputText(it) },
                    onSend = { viewModel.sendMessage() },
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onSelectConversation = { viewModel.selectConversation(it) },
                    onNewConversation = { viewModel.createNewConversation() },
                    onDeleteConversation = { viewModel.deleteConversation(it) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            // 定时任务页面
            composable(Screen.Tasks.route) {
                TasksScreen(
                    tasks = tasks,
                    onAddTask = { navController.navigate("task_edit") },
                    onEditTask = { navController.navigate("task_edit/${it.id}") },
                    onToggleTask = { viewModel.toggleTask(it) },
                    onDeleteTask = { viewModel.deleteTask(it) }
                )
            }
            
            // 任务编辑页面
            composable("task_edit") {
                TaskEditScreen(
                    task = null,
                    onSave = { 
                        viewModel.saveTask(it)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("task_edit/{taskId}") { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
                val task = tasks.find { it.id == taskId }
                TaskEditScreen(
                    task = task,
                    onSave = {
                        viewModel.saveTask(it)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            // 执行历史页面
            composable(Screen.History.route) {
                ExecutionHistoryScreen(
                    executions = executions,
                    onBack = { navController.popBackStack() },
                    onViewDetail = { /* 显示详情对话框 */ }
                )
            }
            
            // 设置页面
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentConfig = modelConfig,
                    onSave = { config ->
                        viewModel.saveModelConfig(config)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}