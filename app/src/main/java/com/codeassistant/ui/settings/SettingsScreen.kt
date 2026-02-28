package com.codeassistant.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.codeassistant.data.model.ModelConfig
import com.codeassistant.data.model.PresetModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: ModelConfig,
    onSave: (ModelConfig) -> Unit,
    onBack: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(0) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var temperature by remember { mutableStateOf(currentConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(currentConfig.maxTokens.toString()) }
    var showApiKey by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 预设选择
            Text(
                text = "选择预设",
                style = MaterialTheme.typography.titleMedium
            )
            
            PresetModels.presets.forEachIndexed { index, preset ->
                FilterChip(
                    selected = selectedPreset == index,
                    onClick = { 
                        selectedPreset = index
                        baseUrl = preset.baseUrl
                        model = preset.model
                    },
                    label = { Text(preset.name) }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showApiKey) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "隐藏" else "显示")
                    }
                },
                singleLine = true
            )
            
            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如: https://api.openai.com/v1") },
                singleLine = true
            )
            
            // Model
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如: gpt-4, claude-3-opus") },
                singleLine = true
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 高级设置
            Text(
                text = "高级设置",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Temperature
            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("Temperature") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("控制响应的随机性 (0-2)") }
            )
            
            // Max Tokens
            OutlinedTextField(
                value = maxTokens,
                onValueChange = { maxTokens = it },
                label = { Text("Max Tokens") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("响应的最大 token 数") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 保存按钮
            Button(
                onClick = {
                    onSave(
                        ModelConfig(
                            name = PresetModels.presets[selectedPreset].name,
                            provider = PresetModels.presets[selectedPreset].provider,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model,
                            temperature = temperature.toFloatOrNull() ?: 0.7f,
                            maxTokens = maxTokens.toIntOrNull() ?: 4096
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存配置")
            }
            
            // 测试按钮
            OutlinedButton(
                onClick = { /* TODO: 测试连接 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("测试连接")
            }
        }
    }
}