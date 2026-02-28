package com.codeassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

// 定时任务频率
enum class ScheduleFrequency {
    ONCE,       // 一次性
    DAILY,      // 每天
    WEEKLY,     // 每周
    HOURLY,     // 每小时
    CUSTOM      // 自定义 cron
}

// 任务状态
enum class TaskStatus {
    ACTIVE,     // 启用
    PAUSED,     // 暂停
    DISABLED    // 禁用
}

// 定时任务
@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                           // 任务名称
    val prompt: String,                         // 要执行的提示词
    val frequency: ScheduleFrequency,           // 频率
    val hour: Int,                              // 执行时间 - 小时
    val minute: Int,                            // 执行时间 - 分钟
    val daysOfWeek: String = "",                // 周几执行 (1-7, 逗号分隔)
    val cronExpression: String = "",            // 自定义 cron 表达式
    val status: TaskStatus = TaskStatus.ACTIVE, // 状态
    val lastRunAt: Long? = null,                // 上次执行时间
    val nextRunAt: Long? = null,                // 下次执行时间
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 计算下次执行时间
    fun calculateNextRunTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val now = System.currentTimeMillis()
        
        when (frequency) {
            ScheduleFrequency.ONCE -> {
                if (calendar.timeInMillis <= now) {
                    // 如果时间已过，设为明天
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            ScheduleFrequency.DAILY -> {
                while (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            ScheduleFrequency.WEEKLY -> {
                val days = daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
                if (days.isNotEmpty()) {
                    while (calendar.timeInMillis <= now || 
                           !days.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }
            ScheduleFrequency.HOURLY -> {
                while (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                }
            }
            ScheduleFrequency.CUSTOM -> {
                // 自定义 cron 暂时按小时处理
                while (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                }
            }
        }
        
        return calendar.timeInMillis
    }
}

// 任务执行记录
@Entity(tableName = "task_executions")
data class TaskExecution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,                           // 任务ID
    val taskName: String,                       // 任务名称（快照）
    val executedAt: Long = System.currentTimeMillis(), // 执行时间
    val prompt: String,                         // 发送的提示词
    val response: String,                       // AI 响应
    val success: Boolean,                       // 是否成功
    val errorMessage: String? = null            // 错误信息
)