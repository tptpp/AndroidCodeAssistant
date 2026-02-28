package com.codeassistant.data.database

import androidx.room.*
import com.codeassistant.data.model.TaskExecution
import com.codeassistant.data.model.ScheduledTask
import com.codeassistant.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY nextRunAt ASC")
    fun getAllTasks(): Flow<List<ScheduledTask>>
    
    @Query("SELECT * FROM scheduled_tasks WHERE status = :status ORDER BY nextRunAt ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<ScheduledTask>>
    
    @Query("SELECT * FROM scheduled_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): ScheduledTask?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTask): Long
    
    @Update
    suspend fun updateTask(task: ScheduledTask)
    
    @Delete
    suspend fun deleteTask(task: ScheduledTask)
    
    @Query("UPDATE scheduled_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, status: TaskStatus)
}

@Dao
interface TaskExecutionDao {
    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY executedAt DESC")
    fun getExecutionsByTask(taskId: Long): Flow<List<TaskExecution>>
    
    @Query("SELECT * FROM task_executions ORDER BY executedAt DESC LIMIT :limit")
    fun getRecentExecutions(limit: Int = 50): Flow<List<TaskExecution>>
    
    @Query("SELECT * FROM task_executions ORDER BY executedAt DESC")
    fun getAllExecutions(): Flow<List<TaskExecution>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: TaskExecution): Long
    
    @Query("DELETE FROM task_executions WHERE taskId = :taskId")
    suspend fun deleteExecutionsByTask(taskId: Long)
    
    @Query("DELETE FROM task_executions WHERE executedAt < :before")
    suspend fun deleteOldExecutions(before: Long)
}