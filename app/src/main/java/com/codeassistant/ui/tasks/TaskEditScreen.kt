package com.codeassistant.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.codeassistant.data.model.ScheduleFrequency
import com.codeassistant.data.model.ScheduledTask
import com.codeassistant.data.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    task: ScheduledTask?,
    onSave: (ScheduledTask) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(task?.name ?: "") }
    var prompt by remember { mutableStateOf(task?.prompt ?: "") }
    var frequency by remember { mutableStateOf(task?.frequency ?: ScheduleFrequency.DAILY) }
    var hour by remember { mutableStateOf(task?.hour ?: 9) }
    var minute by remember { mutableStateOf(task?.minute ?: 0) }
    var selectedDays by remember { mutableStateOf(
        task?.daysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf(1, 2, 3, 4, 5)
    )}
    
    val scrollState = rememberScrollState()
    val weekDays = listOf("周一" to 2, "周二" to 3, "周三" to 4, "周四" to 5, "周五" to 6, "周六" to 7, "周日" to 1)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (task == null) "新建任务" else "编辑任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val newTask = ScheduledTask(
                                id = task?.id ?: 0,
                                name = name,
                                prompt = prompt,
                                frequency = frequency,
                                hour = hour,
                                minute = minute,
                                daysOfWeek = selectedDays.joinToString(","),
                                status = task?.status ?: TaskStatus.ACTIVE
                            )
                            onSave(newTask)
                        },
                        enabled = name.isNotBlank() && prompt.isNotBlank()
                    ) {
                        Text("保存")
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
            // 任务名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("任务名称") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：每日新闻摘要") },
                singleLine = true
            )
            
            // 提示词
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("提示词") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("输入要发送给 AI 的提示词...\n\n例如：请总结今天的科技新闻，列出5条最重要的") },
                supportingText = { Text("AI 将按时执行此提示词") }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 执行频率
            Text(
                text = "执行频率",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = frequency == ScheduleFrequency.DAILY,
                    onClick = { frequency = ScheduleFrequency.DAILY },
                    label = { Text("每天") }
                )
                FilterChip(
                    selected = frequency == ScheduleFrequency.WEEKLY,
                    onClick = { frequency = ScheduleFrequency.WEEKLY },
                    label = { Text("每周") }
                )
                FilterChip(
                    selected = frequency == ScheduleFrequency.HOURLY,
                    onClick = { frequency = ScheduleFrequency.HOURLY },
                    label = { Text("每小时") }
                )
                FilterChip(
                    selected = frequency == ScheduleFrequency.ONCE,
                    onClick = { frequency = ScheduleFrequency.ONCE },
                    label = { Text("一次性") }
                )
            }
            
            // 执行时间
            Text(
                text = "执行时间",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 小时选择
                Column {
                    Text("小时", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { 
                            hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour 
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                
                Text(":", style = MaterialTheme.typography.headlineMedium)
                
                // 分钟选择
                Column {
                    Text("分钟", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = { 
                            minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute 
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                
                // 快捷时间按钮
                Column {
                    Text("快捷", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AssistChip(onClick = { hour = 9; minute = 0 }, label = { Text("9:00") })
                        AssistChip(onClick = { hour = 12; minute = 0 }, label = { Text("12:00") })
                        AssistChip(onClick = { hour = 18; minute = 0 }, label = { Text("18:00") })
                    }
                }
            }
            
            // 周几执行（仅周频率时显示）
            if (frequency == ScheduleFrequency.WEEKLY) {
                Text(
                    text = "执行日期",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekDays.forEach { (label, day) ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 预设模板
            Text(
                text = "预设模板",
                style = MaterialTheme.typography.titleMedium
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetTemplate(
                    name = "每日新闻摘要",
                    prompt = "请总结今天的科技新闻，列出5条最重要的，每条用一句话概括。",
                    onClick = {
                        name = "每日新闻摘要"
                        prompt = "请总结今天的科技新闻，列出5条最重要的，每条用一句话概括。"
                        frequency = ScheduleFrequency.DAILY
                        hour = 9
                        minute = 0
                    }
                )
                
                PresetTemplate(
                    name = "周报生成",
                    prompt = "请帮我整理本周的工作内容，生成一份周报格式。",
                    onClick = {
                        name = "周报生成"
                        prompt = "请帮我整理本周的工作内容，生成一份周报格式。"
                        frequency = ScheduleFrequency.WEEKLY
                        hour = 18
                        minute = 0
                    }
                )
                
                PresetTemplate(
                    name = "代码审查提醒",
                    prompt = "检查我最近的代码提交，列出可能存在的问题和改进建议。",
                    onClick = {
                        name = "代码审查提醒"
                        prompt = "检查我最近的代码提交，列出可能存在的问题和改进建议。"
                        frequency = ScheduleFrequency.DAILY
                        hour = 18
                        minute = 30
                    }
                )
            }
        }
    }
}

@Composable
fun PresetTemplate(
    name: String,
    prompt: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}