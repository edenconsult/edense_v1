package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.repository.EdenseRepository
import com.example.domain.schedule.DayScheduleStatus
import com.example.domain.schedule.SchedulingEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeViewModel(private val repository: EdenseRepository) : ViewModel() {

    private val _currentTimeStr = MutableStateFlow(getCurrentTimeFormatted())
    val currentTimeStr: StateFlow<String> = _currentTimeStr.asStateFlow()

    private val _currentDateStr = MutableStateFlow(getCurrentDateFormatted())
    val currentDateStr: StateFlow<String> = _currentDateStr.asStateFlow()

    private val _currentDateFormatted = MutableStateFlow(getCurrentDisplayDate())
    val currentDateFormatted: StateFlow<String> = _currentDateFormatted.asStateFlow()

    val teacherProfile: StateFlow<TeacherProfileEntity?> = repository.teacherProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val academicCalendar: StateFlow<AcademicCalendarEntity?> = repository.academicCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedSubjects: StateFlow<List<SubjectEntity>> = repository.selectedSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasks: StateFlow<List<TaskEntity>> = repository.pendingTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time schedule state for today
    val scheduleStatus: StateFlow<DayScheduleStatus> = combine(
        currentTimeStr,
        repository.allPeriods,
        selectedSubjects,
        academicCalendar,
        repository.academicTerms
    ) { time, periods, subjects, calendar, terms ->
        val subjectMap = subjects.associateBy { it.id }
        val dayOfWeek = getDayOfWeekInt()
        val periodsToday = periods.filter { it.dayOfWeek == dayOfWeek }

        val todayDate = getCurrentDateFormatted()
        val isVacation = calendar?.isVacation(todayDate, terms) ?: false
        val resumesDate = calendar?.nextTermStartDate(todayDate, terms).orEmpty()

        SchedulingEngine.resolveDaySchedule(
            currentTimeStr = time,
            periodsToday = periodsToday,
            subjectMap = subjectMap,
            isVacation = isVacation,
            vacationResumesDate = resumesDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayScheduleStatus.Empty("Loading schedule..."))

    init {
        // Ticker updates time around minute boundaries without waking every second
        viewModelScope.launch {
            while (isActive) {
                _currentTimeStr.value = getCurrentTimeFormatted()
                _currentDateStr.value = getCurrentDateFormatted()
                _currentDateFormatted.value = getCurrentDisplayDate()
                delay(30_000) // check every 30 seconds
            }
        }
    }

    fun logSession(
        subjectId: String,
        className: String,
        sessionDate: String,
        lastTopicCovered: String,
        nextTopicPlanned: String,
        status: String,
        notes: String = "",
        createFollowUpTask: Boolean = false,
        followUpDueDate: String = ""
    ) {
        viewModelScope.launch {
            repository.logSession(
                subjectId = subjectId,
                className = className,
                sessionDate = sessionDate.ifBlank { getCurrentDateFormatted() },
                lastTopicCovered = lastTopicCovered,
                nextTopicPlanned = nextTopicPlanned,
                status = status,
                notes = notes,
                createFollowUpTask = createFollowUpTask,
                followUpDueDate = followUpDueDate
            )
        }
    }

    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId, isCompleted)
        }
    }

    private fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDateFormatted(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDisplayDate(): String {
        return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    private fun getDayOfWeekInt(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}

class HomeViewModelFactory(private val repository: EdenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
