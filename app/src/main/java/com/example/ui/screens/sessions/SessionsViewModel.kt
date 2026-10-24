package com.example.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.example.data.repository.EdenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectCarryForward(
    val subject: SubjectEntity,
    val latestSession: TeachingSessionEntity?
)

class SessionsViewModel(private val repository: EdenseRepository) : ViewModel() {

    private val _filterSubjectId = MutableStateFlow<String?>(null)
    val filterSubjectId: StateFlow<String?> = _filterSubjectId.asStateFlow()

    val selectedSubjects: StateFlow<List<SubjectEntity>> = repository.selectedSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<TeachingSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSessions: StateFlow<List<TeachingSessionEntity>> = combine(
        allSessions,
        filterSubjectId
    ) { sessions, filterId ->
        if (filterId == null) sessions else sessions.filter { it.subjectId == filterId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val carryForwardSummaries: StateFlow<List<SubjectCarryForward>> = combine(
        selectedSubjects,
        allSessions
    ) { subjects, sessions ->
        subjects.map { subject ->
            val latest = sessions.firstOrNull { it.subjectId == subject.id }
            SubjectCarryForward(subject, latest)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSubjectFilter(subjectId: String?) {
        _filterSubjectId.value = subjectId
    }

    fun logSession(
        subjectId: String,
        sessionDate: String,
        lastTopic: String,
        nextTopic: String,
        status: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.logSession(subjectId, sessionDate, lastTopic, nextTopic, status, notes)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.allSessions // triggers through dao
        }
    }
}

class SessionsViewModelFactory(private val repository: EdenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
