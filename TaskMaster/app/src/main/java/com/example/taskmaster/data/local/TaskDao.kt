package com.example.taskmaster.data.local

import androidx.room.*
import com.example.taskmaster.data.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTasksForUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<Task?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}