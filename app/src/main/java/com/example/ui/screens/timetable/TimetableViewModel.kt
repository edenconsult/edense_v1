package com.example.ui.screens.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.repository.EdenseRepository
import com.example.domain.schedule.ClassBlock
import com.example.domain.schedule.SchedulingEngine
import com.example.domain.schedule.SchoolPeriodSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    val profile = repository.teacherProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val periodSlots: StateFlow<List<SchoolPeriodSlot>> = profile.map { settings ->
        SchedulingEngine.periodSlots(
            settings?.breakfastBreakStart ?: "09:00",
            settings?.breakfastBreakEnd ?: "09:30",
            settings?.lunchBreakStart ?: "12:30",
            settings?.lunchBreakEnd ?: "13:00"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchedulingEngine.periodSlots)

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
            val endTime = validatePeriodChanges(
                subjectId, dayOfWeek, startTime, isDoublePeriod
            ) ?: return@launch
            repository.addPeriod(
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                className = className,
                classroom = classroom,
                isDoublePeriod = isDoublePeriod,
                breakfastBreakStart = profile.value?.breakfastBreakStart ?: "09:00",
                breakfastBreakEnd = profile.value?.breakfastBreakEnd ?: "09:30",
                lunchBreakStart = profile.value?.lunchBreakStart ?: "12:30",
                lunchBreakEnd = profile.value?.lunchBreakEnd ?: "13:00"
            )
            _conflictWarning.value = null
        }
    }

    fun updatePeriodBlock(
        block: ClassBlock,
        subjectId: String,
        dayOfWeek: Int,
        startTime: String,
        className: String,
        classroom: String,
        isDoublePeriod: Boolean
    ) {
        viewModelScope.launch {
            if (validatePeriodChanges(subjectId, dayOfWeek, startTime, isDoublePeriod, block.blockId) == null) {
                return@launch
            }
            repository.updatePeriodBlock(
                blockId = block.blockId,
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                className = className,
                classroom = classroom,
                isDoublePeriod = isDoublePeriod,
                breakfastBreakStart = profile.value?.breakfastBreakStart ?: "09:00",
                breakfastBreakEnd = profile.value?.breakfastBreakEnd ?: "09:30",
                lunchBreakStart = profile.value?.lunchBreakStart ?: "12:30",
                lunchBreakEnd = profile.value?.lunchBreakEnd ?: "13:00"
            )
            _conflictWarning.value = null
        }
    }

    private fun validatePeriodChanges(
        subjectId: String,
        dayOfWeek: Int,
        startTime: String,
        isDoublePeriod: Boolean,
        ignoredBlockId: String? = null
    ): String? {
        if (selectedSubjects.value.none { it.id == subjectId }) {
            _conflictWarning.value = "Choose one of your selected subjects."
            return null
        }
        val slots = periodSlots.value
        val startSlotIndex = slots.indexOfFirst { it.startTime == startTime }
        if (startSlotIndex == -1) {
            _conflictWarning.value = "Choose a valid one-hour school period."
            return null
        }
        val startSlot = slots[startSlotIndex]
        val nextSlot = slots.getOrNull(startSlotIndex + 1)
        if (isDoublePeriod && (nextSlot == null || nextSlot.startMinute != startSlot.endMinute)) {
            _conflictWarning.value = "A double period must use two consecutive periods without a break."
            return null
        }
        val endTime = if (isDoublePeriod) nextSlot!!.endTime else startSlot.endTime
        val newStartMin = startSlot.startMinute
        val newEndMin = SchedulingEngine.parseTimeToMinutes(endTime)
        val hasOverlap = allPeriods.value.any { existing ->
            existing.dayOfWeek == dayOfWeek && existing.blockId != ignoredBlockId &&
                newStartMin < SchedulingEngine.parseTimeToMinutes(existing.endTime) &&
                SchedulingEngine.parseTimeToMinutes(existing.startTime) < newEndMin
        }
        if (hasOverlap) {
            _conflictWarning.value = "Warning: This class overlaps with an already scheduled period."
            return null
        }
        return endTime
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
