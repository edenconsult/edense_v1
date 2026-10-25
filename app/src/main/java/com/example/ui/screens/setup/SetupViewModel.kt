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
import kotlinx.coroutines.flow.first
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun showError(message: String) {
        _successMessage.value = null
        _errorMessage.value = message
    }

    fun showSuccess(message: String) {
        _errorMessage.value = null
        _successMessage.value = message
    }

    fun onGoogleSignInSuccess() {
        val user = authManager.currentUser.value ?: return
        viewModelScope.launch {
            try {
                syncManager.prepareLocalAccount(user.uid)
                val accountName = user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                val current = repository.teacherProfile.first() ?: TeacherProfileEntity()
                repository.saveTeacherProfile(
                    current.copy(
                        name = accountName ?: current.name,
                        email = user.email ?: current.email,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                showSuccess("Signed in as ${accountName ?: user.email ?: "your Google account"}.")
            } catch (error: Exception) {
                showError(
                    "Couldn't isolate local data for this account: " +
                        (error.localizedMessage ?: "Unknown error")
                )
            }
        }
    }

    suspend fun syncAuthenticatedUser(uid: String) {
        val user = authManager.currentUser.value?.takeIf { it.uid == uid } ?: return

        try {
            syncManager.prepareLocalAccount(uid)
            if (!syncManager.isOnline) return
            val accountName = user.displayName?.trim()?.takeIf { it.isNotEmpty() }
            val current = repository.teacherProfile.first() ?: TeacherProfileEntity()
            repository.saveTeacherProfile(
                current.copy(
                    name = accountName ?: current.name,
                    email = user.email ?: current.email,
                    updatedAt = System.currentTimeMillis()
                )
            )
            val result = syncManager.performBidirectionalSync(uid)
            result.exceptionOrNull()?.let { error ->
                showError("Automatic sync failed: ${error.localizedMessage ?: "Unknown error"}")
                return
            }
            if (accountName != null) showSuccess("Signed in as $accountName.")
        } catch (error: Exception) {
            showError("Automatic sync failed: ${error.localizedMessage ?: "Unknown error"}")
        }
    }

    fun saveProfile(
        name: String,
        schoolName: String,
        startTime: String,
        endTime: String,
        breakfastStart: String,
        breakfastEnd: String,
        lunchStart: String,
        lunchEnd: String
    ) {
        val current = profile.value ?: TeacherProfileEntity()
        _errorMessage.value = null
        _successMessage.value = null

        if (startTime.isNotBlank() && endTime.isNotBlank() && startTime >= endTime) {
            _errorMessage.value = "School start time ($startTime) must be before closing time ($endTime)."
            return
        }
        val times = listOf(startTime, endTime, breakfastStart, breakfastEnd, lunchStart, lunchEnd)
        if (times.any { !it.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")) }) {
            _errorMessage.value = "Enter valid school and break times."
            return
        }
        val schoolStartMinutes = com.example.domain.schedule.SchedulingEngine.parseTimeToMinutes(startTime)
        val schoolEndMinutes = com.example.domain.schedule.SchedulingEngine.parseTimeToMinutes(endTime)
        val breakfastStartMinutes = com.example.domain.schedule.SchedulingEngine.parseTimeToMinutes(breakfastStart)
        val breakfastEndMinutes = com.example.domain.schedule.SchedulingEngine.parseTimeToMinutes(breakfastEnd)
        val lunchStartMinutes = com.example.domain.schedule.SchedulingEngine.parseTimeToMinutes(lunchStart)
        val lunchEndMinutes = com.example.domain.schedule.SchedulingEngine.parseTimeToMinutes(lunchEnd)
        if (schoolStartMinutes >= schoolEndMinutes ||
            breakfastStartMinutes >= breakfastEndMinutes ||
            lunchStartMinutes >= lunchEndMinutes ||
            breakfastStartMinutes < schoolStartMinutes ||
            breakfastEndMinutes > schoolEndMinutes ||
            lunchStartMinutes < schoolStartMinutes ||
            lunchEndMinutes > schoolEndMinutes ||
            breakfastStartMinutes < lunchEndMinutes && lunchStartMinutes < breakfastEndMinutes
        ) {
            _errorMessage.value = "Breaks must have a valid duration, fit within school hours, and not overlap."
            return
        }

        viewModelScope.launch {
            try {
                val breaksChanged =
                    breakfastStart != current.breakfastBreakStart ||
                        breakfastEnd != current.breakfastBreakEnd ||
                        lunchStart != current.lunchBreakStart ||
                        lunchEnd != current.lunchBreakEnd
                if (breaksChanged) {
                    val breaks = listOf(
                        breakfastStartMinutes to breakfastEndMinutes,
                        lunchStartMinutes to lunchEndMinutes
                    )
                    val overlappingPeriod = repository.allPeriods.first().firstOrNull { period ->
                        val periodStart = com.example.domain.schedule.SchedulingEngine
                            .parseTimeToMinutes(period.startTime)
                        val periodEnd = com.example.domain.schedule.SchedulingEngine
                            .parseTimeToMinutes(period.endTime)
                        breaks.any { (breakStart, breakEnd) ->
                            periodStart < breakEnd && breakStart < periodEnd
                        }
                    }
                    if (overlappingPeriod != null) {
                        showError(
                            "A scheduled class overlaps the new break time " +
                                "(${overlappingPeriod.startTime}–${overlappingPeriod.endTime}). " +
                                "Move or remove that class before changing breaks."
                        )
                        return@launch
                    }
                }
                repository.saveTeacherProfile(
                    current.copy(
                        name = name.ifBlank { "Teacher" },
                        schoolName = schoolName.ifBlank { "Senior High School" },
                        schoolStartTime = startTime,
                        schoolEndTime = endTime,
                        breakfastBreakStart = breakfastStart,
                        breakfastBreakEnd = breakfastEnd,
                        lunchBreakStart = lunchStart,
                        lunchBreakEnd = lunchEnd,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                showSuccess("Profile saved successfully.")
            } catch (error: Exception) {
                showError("Couldn't save profile: ${error.localizedMessage ?: "Unknown error"}")
            }
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
        _errorMessage.value = null
        _successMessage.value = null

        val current = calendar.value ?: AcademicCalendarEntity()
        val cleanedTerms = terms.filter { it.name.isNotBlank() || it.startDate.isNotBlank() || it.endDate.isNotBlank() }

        // Validate each term that has dates
        for (term in cleanedTerms) {
            if (term.startDate.isNotBlank() && term.endDate.isNotBlank() && term.startDate > term.endDate) {
                _errorMessage.value = "Start date cannot be after end date for ${term.name} (${term.startDate} to ${term.endDate})."
                return
            }
        }

        val termsToSave = if (cleanedTerms.isEmpty()) {
            listOf(
                AcademicTermEntity(
                    id = "active_term_1",
                    name = termName.ifBlank { if (systemType == "TERM") "Term 1" else "Semester 1" },
                    startDate = startDate.ifBlank { "2026-09-01" },
                    endDate = endDate.ifBlank { "2026-12-18" },
                    orderIndex = 0
                )
            )
        } else {
            cleanedTerms.mapIndexed { index, term ->
                term.copy(
                    orderIndex = index,
                    name = term.name.ifBlank { if (systemType == "TERM") "Term ${index + 1}" else "Semester ${index + 1}" },
                    startDate = term.startDate.ifBlank { if (index == 0) startDate.ifBlank { "2026-09-01" } else "" },
                    endDate = term.endDate.ifBlank { if (index == 0) endDate.ifBlank { "2026-12-18" } else "" }
                )
            }
        }

        viewModelScope.launch {
            try {
                val first = termsToSave.first()
                val updatedCalendar = current.copy(
                    academicYear = academicYear.ifBlank { "2026/2027" },
                    systemType = systemType,
                    activeTermName = first.name,
                    termStartDate = first.startDate,
                    termEndDate = first.endDate,
                    pauseAlarmsDuringVacation = pauseOnVacation,
                    isManualVacationMode = manualVacation,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveAcademicCalendar(updatedCalendar)
                repository.saveAcademicTerms(termsToSave)
                showSuccess("Academic calendar saved successfully.")
            } catch (error: Exception) {
                showError("Couldn't save academic calendar: ${error.localizedMessage ?: "Unknown error"}")
            }
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

    fun addCustomSubject(name: String, category: String) {
        viewModelScope.launch {
            try {
                val added = repository.addCustomSubject(name, category)
                if (!added) {
                    showError("A subject with that name already exists.")
                } else {
                    showSuccess("Custom subject added successfully.")
                }
            } catch (error: IllegalArgumentException) {
                showError(error.message ?: "Check the subject name and category.")
            } catch (error: Exception) {
                showError("Couldn't add subject: ${error.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun completeOnboarding() {
        val current = profile.value ?: TeacherProfileEntity()
        viewModelScope.launch {
            repository.saveTeacherProfile(current.copy(isOnboardingCompleted = true))
        }
    }

    suspend fun createDataExport(): String = repository.exportDataAsJson()
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
