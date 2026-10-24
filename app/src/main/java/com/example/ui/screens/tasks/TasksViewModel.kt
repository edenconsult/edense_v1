package com.example.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.EdenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter {
    ALL, PENDING, COMPLETED
}

class TasksViewModel(private val repository: EdenseRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    val selectedSubjects: StateFlow<List<SubjectEntity>> = repository.selectedSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        filter,
        typeFilter
    ) { tasks, filterStatus, filterType ->
        tasks.filter { task ->
            val statusMatches = when (filterStatus) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
            val typeMatches = if (filterType == "ALL") true else task.type == filterType
            statusMatches && typeMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId, isCompleted)
        }
    }

    fun addTask(
        title: String,
        subjectId: String,
        type: String,
        dueDate: String,
        reminderTime: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addTask(title, subjectId, type, dueDate, reminderTime, notes)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }
}

class TasksViewModelFactory(private val repository: EdenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
