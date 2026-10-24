package com.example.ui.screens.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.repository.EdenseRepository
import com.example.domain.schedule.ClassBlock
import com.example.domain.schedule.SchedulingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SubjectQuota(
    val subject: SubjectEntity,
    val scheduledPeriodsCount: Int,
    val targetPeriods: Int
)

class TimetableViewModel(private val repository: EdenseRepository) : ViewModel() {

    private val _selectedDay = MutableStateFlow(getInitialDayOfWeek())
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    val selectedSubjects: StateFlow<List<SubjectEntity>> = repository.selectedSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPeriods: StateFlow<List<PeriodEntity>> = repository.allPeriods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Blocks for the currently selected day, grouped using SchedulingEngine
    val blocksForSelectedDay: StateFlow<List<ClassBlock>> = combine(
        selectedDay,
        allPeriods,
        selectedSubjects
    ) { day, periods, subjects ->
        val subjectMap = subjects.associateBy { it.id }
        val dayPeriods = periods.filter { it.dayOfWeek == day }
        SchedulingEngine.groupPeriodsIntoBlocks(dayPeriods, subjectMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quotas for the teacher's selected subjects
    val subjectQuotas: StateFlow<List<SubjectQuota>> = combine(
        selectedSubjects,
        allPeriods
    ) { subjects, periods ->
        subjects.map { subject ->
            val count = periods
                .asSequence()
                .filter { it.subjectId == subject.id }
                .sumOf { SchedulingEngine.periodUnitCount(it.startTime, it.endTime) }
            SubjectQuota(
                subject = subject,
                scheduledPeriodsCount = count,
                targetPeriods = subject.targetPeriodsPerWeek
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _conflictWarning = MutableStateFlow<String?>(null)
    val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun addPeriod(
        subjectId: String,
        dayOfWeek: Int,
        startTime: String,
        className: String,
        classroom: String,
        isDoublePeriod: Boolean
    ) {
        viewModelScope.launch {
            val startSlotIndex = SchedulingEngine.periodSlots.indexOfFirst { it.startTime == startTime }
            if (startSlotIndex == -1) {
                _conflictWarning.value = "Choose a valid one-hour school period."
                return@launch
            }
            val startSlot = SchedulingEngine.periodSlots[startSlotIndex]
            val nextSlot = SchedulingEngine.periodSlots.getOrNull(startSlotIndex + 1)
            if (isDoublePeriod && (nextSlot == null || nextSlot.startMinute != startSlot.endMinute)) {
                _conflictWarning.value = "A double period must use two consecutive periods without a break."
                return@launch
            }
            val endTime = if (isDoublePeriod) nextSlot!!.endTime else startSlot.endTime
            val newStartMin = startSlot.startMinute
            val newEndMin = SchedulingEngine.parseTimeToMinutes(endTime)

            // Check if there is an overlapping period on the same day
            val existingToday = allPeriods.value.filter { it.dayOfWeek == dayOfWeek }
            val hasOverlap = existingToday.any { existing ->
                val exStart = SchedulingEngine.parseTimeToMinutes(existing.startTime)
                val exEnd = SchedulingEngine.parseTimeToMinutes(existing.endTime)
                newStartMin < exEnd && exStart < newEndMin
            }

            if (hasOverlap) {
                _conflictWarning.value = "Warning: This class overlaps with an already scheduled period."
                return@launch
            } else {
                _conflictWarning.value = null
            }

            repository.addPeriod(
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                className = className,
                classroom = classroom,
                isDoublePeriod = isDoublePeriod
            )
        }
    }

    fun deleteBlock(block: ClassBlock) {
        viewModelScope.launch {
            // Find all period entities with this blockId or matching start/end
            val matching = allPeriods.value.filter { it.blockId == block.blockId }
            for (m in matching) {
                repository.deletePeriod(m.id, m.blockId)
            }
        }
    }

    fun clearWarning() {
        _conflictWarning.value = null
    }

    private fun getInitialDayOfWeek(): Int {
        val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            else -> 1 // Default to Monday on weekends
        }
    }
}

class TimetableViewModelFactory(private val repository: EdenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
