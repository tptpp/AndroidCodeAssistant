package com.codeassistant.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codeassistant.data.model.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    task: Task?,
    onSave: (Task) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var prompt by remember { mutableStateOf(task?.prompt ?: "") }
    var taskType by remember { mutableStateOf(task?.type ?: TaskType.SCHEDULED) }
    var frequency by remember { mutableStateOf(task?.frequency ?: ScheduleFrequency.DAILY) }
    var hour by remember { mutableStateOf(task?.scheduledTime?.let { 
        Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) 
    } ?: 9) }
    var minute by remember { mutableStateOf(task?.scheduledTime?.let { 
        Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) 
    } ?: 0) }
    var selectedDays by remember { mutableStateOf(
        task?.daysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf(1,2,3,4,5)
    )}
    var oneTimeDate by remember { mutableStateOf(task?.scheduledTime ?: System.currentTimeMillis() + 86400000) }
    
    val scrollState = rememberScrollState()
    val weekDays = listOf("一" to 2, "二" to 3, "三" to 4, "四" to 5, "五" to 6, "六" to 7, "日" to 1)
    
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
                            val calendar = Calendar.getInstance()
                            if (taskType == TaskType.ONE_TIME) {
                                calendar.timeInMillis = oneTimeDate
                            } else {
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                            }
                            
                            val newTask = Task(
                                id = task?.id ?: 0,
                                title = title,
                                prompt = prompt,
                                type = taskType,
                                frequency = if (taskType == TaskType.SCHEDULED) frequency else null,
                                scheduledTime = calendar.timeInMillis,
                                daysOfWeek = selectedDays.joinToString(","),
                                status = task?.status ?: TaskStatus.ACTIVE
                            )
                            onSave(newTask)
                        },
                        enabled = title.isNotBlank() && prompt.isNotBlank()
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务名称") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：每日新闻摘要") },
                singleLine = true
            )
            
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("执行内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = { Text("AI 将执行的内容...") }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("任务类型", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = taskType == TaskType.SCHEDULED,
                    onClick = { taskType = TaskType.SCHEDULED },
                    label = { Text("定时任务") }
                )
                FilterChip(
                    selected = taskType == TaskType.ONE_TIME,
                    onClick = { taskType = TaskType.ONE_TIME },
                    label = { Text("一次性") }
                )
            }
            
            if (taskType == TaskType.SCHEDULED) {
                Text("执行频率", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
                
                Text("执行时间", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                        label = { Text("时") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                        label = { Text("分") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }
                
                if (frequency == ScheduleFrequency.WEEKLY) {
                    Text("执行日期", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEach { (label, day) ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(day)) 
                                        selectedDays - day 
                                    else 
                                        selectedDays + day
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("快速模板", style = MaterialTheme.typography.titleMedium)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(
                    onClick = {
                        title = "每日新闻摘要"
                        prompt = "请总结今天的科技新闻，列出5条最重要的"
                        taskType = TaskType.SCHEDULED
                        frequency = ScheduleFrequency.DAILY
                        hour = 9
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("每日新闻", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                OutlinedCard(
                    onClick = {
                        title = "提醒事项"
                        prompt = "提醒我完成待办事项"
                        taskType = TaskType.ONE_TIME
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("提醒事项", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}