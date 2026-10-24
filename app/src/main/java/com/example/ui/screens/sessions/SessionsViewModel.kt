package com.example.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.example.data.repository.EdenseRepository
import com.example.domain.schedule.ClassroomAssignments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClassCarryForward(
    val subject: SubjectEntity,
    val className: String,
    val latestSession: TeachingSessionEntity?
)

internal fun buildClassCarryForwards(
    subjects: List<SubjectEntity>,
    sessions: List<TeachingSessionEntity>
): List<ClassCarryForward> = subjects.flatMap { subject ->
    val classes = ClassroomAssignments.parse(subject.assignedClasses).ifEmpty { listOf("") }
    classes.map { className ->
        val latest = sessions
            .filter { session ->
                session.subjectId == subject.id &&
                    (session.className == className ||
                        (session.className.isBlank() && classes.size == 1))
            }
            .maxWithOrNull(compareBy({ it.sessionDate }, { it.createdAt }))
        ClassCarryForward(subject, className, latest)
    }
}

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

    val carryForwardSummaries: StateFlow<List<ClassCarryForward>> = combine(
        selectedSubjects,
        allSessions
    ) { subjects, sessions ->
        buildClassCarryForwards(subjects, sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSubjectFilter(subjectId: String?) {
        _filterSubjectId.value = subjectId
    }

    fun logSession(
        subjectId: String,
        className: String,
        sessionDate: String,
        lastTopic: String,
        nextTopic: String,
        status: String,
        notes: String = "",
        createFollowUpTask: Boolean = false,
        followUpDueDate: String = ""
    ) {
        viewModelScope.launch {
            repository.logSession(
                subjectId,
                className,
                sessionDate,
                lastTopic,
                nextTopic,
                status,
                notes,
                createFollowUpTask,
                followUpDueDate
            )
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
