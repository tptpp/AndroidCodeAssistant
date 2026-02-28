package com.codeassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

// 任务类型
enum class TaskType {
    SCHEDULED,  // 定时任务
    ONE_TIME    // 一次性任务
}

// 任务状态
enum class TaskStatus {
    ACTIVE,     // 启用
    PAUSED,     // 暂停
    COMPLETED,  // 已完成（一次性任务）
    DISABLED    // 禁用
}

// 执行频率（仅定时任务）
enum class ScheduleFrequency {
    DAILY,      // 每天
    WEEKLY,     // 每周
    HOURLY,     // 每小时
}

// 任务
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                          // 任务标题
    val prompt: String,                         // 执行内容
    val type: TaskType,                         // 类型
    val frequency: ScheduleFrequency? = null,   // 频率（定时任务）
    val scheduledTime: Long,                    // 执行时间戳
    val daysOfWeek: String = "",                // 周几（周任务）
    val status: TaskStatus = TaskStatus.ACTIVE,
    val lastRunAt: Long? = null,
    val nextRunAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "manual"               // 来源：manual/dialog
) {
    fun getDisplayTime(): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = scheduledTime
        return String.format("%02d:%02d", 
            cal.get(Calendar.HOUR_OF_DAY), 
            cal.get(Calendar.MINUTE))
    }
    
    fun getDisplayDate(): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = scheduledTime
        return String.format("%02d/%02d",
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH))
    }
}

// 任务执行记录
@Entity(tableName = "task_executions")
data class TaskExecution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val taskTitle: String,
    val executedAt: Long = System.currentTimeMillis(),
    val prompt: String,
    val response: String,
    val success: Boolean,
    val errorMessage: String? = null
)