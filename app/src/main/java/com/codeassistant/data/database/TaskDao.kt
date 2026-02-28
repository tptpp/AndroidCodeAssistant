package com.codeassistant.data.database

import androidx.room.*
import com.codeassistant.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY nextRunAt ASC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE type = :type ORDER BY nextRunAt ASC")
    fun getTasksByType(type: TaskType): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY nextRunAt ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("UPDATE tasks SET status = :status WHERE id = :id")
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
}