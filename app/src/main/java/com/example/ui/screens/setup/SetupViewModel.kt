package com.example.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.remote.FirebaseSyncManager
import com.example.domain.schedule.ClassroomAssignments
import com.example.data.repository.EdenseRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SetupViewModel(
    private val repository: EdenseRepository,
    val authManager: AuthManager,
    val syncManager: FirebaseSyncManager
) : ViewModel() {

    val profile: StateFlow<TeacherProfileEntity?> = repository.teacherProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calendar: StateFlow<AcademicCalendarEntity?> = repository.academicCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val academicTerms: StateFlow<List<AcademicTermEntity>> = repository.academicTerms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSubjects: StateFlow<List<SubjectEntity>> = repository.selectedSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCount: StateFlow<Int> = repository.selectedSubjectsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = syncManager.syncLocalDataToCloud()
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Successfully backed up and synced to Cloud Firestore!"
            } else {
                _errorMessage.value = "Cloud sync failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun saveProfile(name: String, schoolName: String, startTime: String, endTime: String) {
        val current = profile.value ?: TeacherProfileEntity()
        viewModelScope.launch {
            repository.saveTeacherProfile(
                current.copy(
                    name = name.ifBlank { "Teacher" },
                    schoolName = schoolName.ifBlank { "Senior High School" },
                    schoolStartTime = startTime.ifBlank { "07:00" },
                    schoolEndTime = endTime.ifBlank { "15:00" }
                )
            )
        }
    }

    fun saveCalendar(
        academicYear: String,
        systemType: String,
        termName: String,
        startDate: String,
        endDate: String,
        pauseOnVacation: Boolean,
        manualVacation: Boolean,
        terms: List<AcademicTermEntity>
    ) {
        val current = calendar.value ?: AcademicCalendarEntity()
        if (!current.copy(
                termStartDate = startDate,
                termEndDate = endDate
            ).hasValidTerms(terms)
        ) {
            _errorMessage.value = "Enter valid, non-overlapping term dates in chronological order."
            return
        }
        viewModelScope.launch {
            repository.saveAcademicCalendar(
                current.copy(
                    academicYear = academicYear.ifBlank { "2026/2027" },
                    systemType = systemType,
                    activeTermName = terms.firstOrNull()?.name?.ifBlank { termName } ?: termName,
                    termStartDate = terms.firstOrNull()?.startDate ?: startDate,
                    termEndDate = terms.firstOrNull()?.endDate ?: endDate,
                    pauseAlarmsDuringVacation = pauseOnVacation,
                    isManualVacationMode = manualVacation
                )
            )
            repository.saveAcademicTerms(terms)
        }
    }

    fun selectSubject(
        subjectId: String,
        assignedClasses: String,
        defaultClassroom: String,
        assignedClassrooms: String,
        targetPeriods: Int
    ) {
        val classes = ClassroomAssignments.parse(assignedClasses)
        val classrooms = ClassroomAssignments.parse(assignedClassrooms)
        if (classes.isEmpty() || classes.size != classrooms.size) {
            _errorMessage.value = "Enter one classroom for each assigned class."
            return
        }
        viewModelScope.launch {
            val success = repository.selectSubject(
                subjectId = subjectId,
                assignedClasses = classes.joinToString(", "),
                defaultClassroom = defaultClassroom.ifBlank { classrooms.first() },
                assignedClassrooms = classrooms.joinToString(", "),
                targetPeriodsPerWeek = if (targetPeriods > 0) targetPeriods else 6
            )
            if (!success) {
                _errorMessage.value = "You can only select up to 2 subjects for this academic year."
            }
        }
    }

    fun deselectSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deselectSubject(subjectId)
        }
    }

    fun completeOnboarding() {
        val current = profile.value ?: TeacherProfileEntity()
        viewModelScope.launch {
            repository.saveTeacherProfile(current.copy(isOnboardingCompleted = true))
        }
    }
}

class SetupViewModelFactory(
    private val repository: EdenseRepository,
    private val authManager: AuthManager,
    private val syncManager: FirebaseSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(repository, authManager, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
