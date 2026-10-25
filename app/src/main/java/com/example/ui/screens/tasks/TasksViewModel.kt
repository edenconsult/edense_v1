package com.example.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.EdenseRepository
import com.example.domain.task.TaskPriorityFilter
import com.example.domain.task.TaskSortOrder
import com.example.domain.task.TaskUrgency
import com.example.domain.task.priorityRank
import com.example.domain.task.taskUrgency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskFilter {
    ALL, PENDING, COMPLETED
}

data class TaskFilterAndSortState(
    val status: TaskFilter = TaskFilter.ALL,
    val type: String = "ALL",
    val subjectId: String = "ALL",
    val priority: TaskPriorityFilter = TaskPriorityFilter.ALL,
    val sortOrder: TaskSortOrder = TaskSortOrder.DUE_DATE_ASC
) {
    val hasActiveSecondaryFilters: Boolean
        get() = type != "ALL" || subjectId != "ALL" || priority != TaskPriorityFilter.ALL
}

class TasksViewModel(private val repository: EdenseRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(TaskFilterAndSortState())
    val filterState: StateFlow<TaskFilterAndSortState> = _filterState.asStateFlow()

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _subjectFilter = MutableStateFlow("ALL")
    val subjectFilter: StateFlow<String> = _subjectFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow(TaskPriorityFilter.ALL)
    val priorityFilter: StateFlow<TaskPriorityFilter> = _priorityFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(TaskSortOrder.DUE_DATE_ASC)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder.asStateFlow()

    val selectedSubjects: StateFlow<List<SubjectEntity>> = repository.selectedSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _filterState,
        selectedSubjects
    ) { tasks, state, subjects ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val subjectMap = subjects.associateBy { it.id }

        tasks.filter { task ->
            val statusMatches = when (state.status) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
            val typeMatches = if (state.type == "ALL") true else task.type.equals(state.type, ignoreCase = true)
            val subjectMatches = if (state.subjectId == "ALL") true else task.subjectId == state.subjectId
            val priorityMatches = when (state.priority) {
                TaskPriorityFilter.ALL -> true
                TaskPriorityFilter.OVERDUE -> taskUrgency(task.dueDate, today) == TaskUrgency.OVERDUE
                TaskPriorityFilter.DUE_TODAY -> taskUrgency(task.dueDate, today) == TaskUrgency.DUE_TODAY
                TaskPriorityFilter.DUE_SOON -> taskUrgency(task.dueDate, today) == TaskUrgency.DUE_SOON
                TaskPriorityFilter.UPCOMING -> taskUrgency(task.dueDate, today) == TaskUrgency.UPCOMING
            }
            statusMatches && typeMatches && subjectMatches && priorityMatches
        }.sortedWith { a, b ->
            when (state.sortOrder) {
                TaskSortOrder.DUE_DATE_ASC -> {
                    val cmp = a.dueDate.compareTo(b.dueDate)
                    if (cmp != 0) cmp else a.reminderTime.compareTo(b.reminderTime)
                }
                TaskSortOrder.DUE_DATE_DESC -> {
                    val cmp = b.dueDate.compareTo(a.dueDate)
                    if (cmp != 0) cmp else b.reminderTime.compareTo(a.reminderTime)
                }
                TaskSortOrder.PRIORITY -> {
                    val rankA = taskUrgency(a.dueDate, today).priorityRank()
                    val rankB = taskUrgency(b.dueDate, today).priorityRank()
                    val cmp = rankA.compareTo(rankB)
                    if (cmp != 0) cmp else a.dueDate.compareTo(b.dueDate)
                }
                TaskSortOrder.SUBJECT -> {
                    val subA = subjectMap[a.subjectId]?.name ?: ""
                    val subB = subjectMap[b.subjectId]?.name ?: ""
                    val cmp = subA.compareTo(subB, ignoreCase = true)
                    if (cmp != 0) cmp else a.dueDate.compareTo(b.dueDate)
                }
                TaskSortOrder.TITLE -> {
                    a.title.compareTo(b.title, ignoreCase = true)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
        _filterState.value = _filterState.value.copy(status = filter)
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
        _filterState.value = _filterState.value.copy(type = type)
    }

    fun setSubjectFilter(subjectId: String) {
        _subjectFilter.value = subjectId
        _filterState.value = _filterState.value.copy(subjectId = subjectId)
    }

    fun setPriorityFilter(priority: TaskPriorityFilter) {
        _priorityFilter.value = priority
        _filterState.value = _filterState.value.copy(priority = priority)
    }

    fun setSortOrder(sortOrder: TaskSortOrder) {
        _sortOrder.value = sortOrder
        _filterState.value = _filterState.value.copy(sortOrder = sortOrder)
    }

    fun clearFilters() {
        _typeFilter.value = "ALL"
        _subjectFilter.value = "ALL"
        _priorityFilter.value = TaskPriorityFilter.ALL
        _filterState.value = _filterState.value.copy(
            type = "ALL",
            subjectId = "ALL",
            priority = TaskPriorityFilter.ALL
        )
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
