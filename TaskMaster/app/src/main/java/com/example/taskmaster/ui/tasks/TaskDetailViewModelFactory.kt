package com.example.taskmaster.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.taskmaster.data.TaskRepository

/**
 * Factory for creating a [TaskDetailViewModel] with a constructor that takes a
 * [TaskRepository] and a [taskId].
 */
class TaskDetailViewModelFactory(
    private val repository: TaskRepository,
    private val taskId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is our TaskDetailViewModel
        if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java)) {
            // If it is, create and return an instance of it, passing both the repository and the taskId.
            @Suppress("UNCHECKED_CAST")
            return TaskDetailViewModel(repository, taskId) as T
        }
        // If it's some other ViewModel, throw an error.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}