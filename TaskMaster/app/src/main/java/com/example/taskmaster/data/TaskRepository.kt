package com.example.taskmaster.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taskmaster.data.local.TaskDao
import com.example.taskmaster.network.CreateTaskRequest
import com.example.taskmaster.network.FullUpdateTaskRequest
import com.example.taskmaster.network.RetrofitInstance
import com.example.taskmaster.network.UpdateTaskRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val api = RetrofitInstance.api

    // --- READ OPERATIONS (from local DB) ---

    fun getTasks(): Flow<List<Task>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return taskDao.getTasksForUser(userId)
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        return taskDao.getTaskById(taskId)
    }

    // --- LIVE SYNC ---

    fun startListeningForRemoteUpdates() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TaskRepo", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remoteTasks = snapshot.toObjects(Task::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        taskDao.insertAll(remoteTasks)
                    }
                }
            }
    }

    // --- WRITE OPERATIONS (local first, then remote) ---

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addTask(title: String, description: String?, dueDateMillis: Long?) {
        val userId = auth.currentUser?.uid ?: return
        val isoDueDate = dueDateMillis?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(it))
        }
        val temporaryTask = Task(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            description = description,
            isDone = false,
            dueDate = isoDueDate,
            createdAt = com.google.firebase.Timestamp.now().toDate().toInstant().toString(),
            isSynced = false // <-- Mark as not synced
        )
        taskDao.insertOrUpdate(temporaryTask)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getIdToken() ?: throw Exception("Not authenticated")
                val request = CreateTaskRequest(title, description, isoDueDate)

                // --- THIS IS THE KEY CHANGE ---
                // The API call to create a task should return the created task object,
                // including its new permanent ID from Firestore.
                val remoteTask = api.createTask("Bearer $token", request)

                // 4. Now that we have the permanent task from the server,
                // we must clean up our local database.

                // First, DELETE the old temporary task.
                taskDao.deleteTask(temporaryTask)

                // Second, INSERT the final task from the server.
                // It will have the permanent ID and will be seen by the listener,
                // but we insert it here for immediate consistency.
                taskDao.insertOrUpdate(remoteTask.copy(isSynced = true))

                Log.d("TaskRepo", "Successfully synced new task and replaced local version.")

            } catch (e: Exception) {
                Log.e("TaskRepo", "Failed to sync new task. It will remain offline.", e)
                // The task remains in the local DB with isSynced = false.
                // We would need a background worker to find these and retry later.
            }
        }
    }

    suspend fun updateTaskStatus(task: Task, isDone: Boolean) {
        taskDao.insertOrUpdate(task.copy(isDone = isDone))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getIdToken() ?: throw Exception("Not authenticated")
                api.updateTask("Bearer $token", task.id, UpdateTaskRequest(isDone))
            } catch (e: Exception) {
                Log.e("TaskRepo", "Failed to sync status update", e)
            }
        }
    }

    suspend fun updateTaskDetails(task: Task) {
        taskDao.insertOrUpdate(task)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getIdToken() ?: throw Exception("Not authenticated")
                val request = FullUpdateTaskRequest(task.title, task.description, task.dueDate)
                api.updateTaskDetails("Bearer $token", task.id, request)
            } catch (e: Exception) {
                Log.e("TaskRepo", "Failed to sync details update", e)
            }
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getIdToken() ?: throw Exception("Not authenticated")
                api.deleteTask("Bearer $token", task.id)
            } catch (e: Exception) {
                Log.e("TaskRepo", "Failed to sync deletion", e)
            }
        }
    }

    private suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
}