package com.example.taskmaster.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmaster.data.Task
import com.example.taskmaster.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskListViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.getTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        repository.startListeningForRemoteUpdates()
    }

    fun addTask(title: String, description: String?, dueDateMillis: Long?) {
        viewModelScope.launch {
            repository.addTask(title, description, dueDateMillis)
        }
    }

    fun updateTaskStatus(task: Task, isDone: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(task, isDone)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
}