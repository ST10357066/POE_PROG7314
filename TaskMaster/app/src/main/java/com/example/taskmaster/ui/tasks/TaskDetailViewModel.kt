package com.example.taskmaster.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmaster.data.Task
import com.example.taskmaster.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val taskId: String
) : ViewModel() {

    val task: StateFlow<Task?> = repository.getTaskById(taskId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateTask(title: String, description: String?, dueDateMillis: Long?) {
        viewModelScope.launch {
            val currentTask = task.value ?: return@launch
            val isoDueDate: String? = dueDateMillis?.let {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.format(Date(it))
            }
            val updatedTask = currentTask.copy(
                title = title,
                description = description,
                dueDate = isoDueDate
            )
            repository.updateTaskDetails(updatedTask)
        }
    }
    // Note: Error handling could be added here from the repository if needed
}