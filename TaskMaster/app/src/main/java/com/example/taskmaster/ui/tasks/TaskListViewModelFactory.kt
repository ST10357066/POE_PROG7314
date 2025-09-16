package com.example.taskmaster.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.taskmaster.data.TaskRepository

class TaskListViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is our TaskListViewModel
        if (modelClass.isAssignableFrom(TaskListViewModel::class.java)) {
            // If it is, create and return an instance of it, passing the repository.
            @Suppress("UNCHECKED_CAST")
            return TaskListViewModel(repository) as T
        }
        // If it's some other ViewModel, throw an error.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}