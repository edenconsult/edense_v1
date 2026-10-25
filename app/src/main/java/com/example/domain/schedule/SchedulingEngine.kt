package com.example.domain.schedule

import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import java.util.Calendar
import java.util.TimeZone

data class ClassBlock(
    val blockId: String,
    val subjectId: String,
    val subjectName: String,
    val className: String,
    val classroom: String,
    val colorHex: String,
    val startTime: String, // HH:mm
    val endTime: String,   // HH:mm
    val isDoublePeriod: Boolean,
    val durationMinutes: Int,
    val hasConflict: Boolean = false
)

data class SchoolPeriodSlot(
    val startTime: String,
    val endTime: String
) {
    val startMinute: Int get() = SchedulingEngine.parseTimeToMinutes(startTime)
    val endMinute: Int get() = SchedulingEngine.parseTimeToMinutes(endTime)
}

fun List<PeriodEntity>.periodBlock(blockId: String): PeriodEntity? {
    val block = filter { it.blockId == blockId }
    val first = block.minByOrNull { SchedulingEngine.parseTimeToMinutes(it.startTime) } ?: return null
    val last = block.maxByOrNull { SchedulingEngine.parseTimeToMinutes(it.endTime) } ?: return first
    return first.copy(endTime = last.endTime)
}

sealed interface DayScheduleStatus {
    data class Vacation(val reason: String, val resumesDate: String) : DayScheduleStatus
    data class Empty(val message: String) : DayScheduleStatus
    data class Active(
        val currentBlock: ClassBlock?,
        val nextBlock: ClassBlock?,
        val countdownText: String?,
        val allBlocksToday: List<ClassBlock>,
        val conflictingBlocks: List<ClassBlock> = emptyList()
    ) : DayScheduleStatus
}

object SchedulingEngine {
    val periodSlots: List<SchoolPeriodSlot> = periodSlots(
        breakfastBreakStart = "09:00",
        breakfastBreakEnd = "09:30",
        lunchBreakStart = "12:30",
        lunchBreakEnd = "13:00"
    )

    fun periodSlots(
        breakfastBreakStart: String,
        breakfastBreakEnd: String,
        lunchBreakStart: String,
        lunchBreakEnd: String
    ): List<SchoolPeriodSlot> {
        val timePattern = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
        require(
            listOf(
                breakfastBreakStart,
                breakfastBreakEnd,
                lunchBreakStart,
                lunchBreakEnd
            ).all { it.matches(timePattern) }
        ) {
            "School break times must use valid HH:mm values."
        }
        val breaks = listOf(
            parseTimeToMinutes(breakfastBreakStart) to parseTimeToMinutes(breakfastBreakEnd),
            parseTimeToMinutes(lunchBreakStart) to parseTimeToMinutes(lunchBreakEnd)
        ).sortedBy { it.first }
        require(breaks.all { (start, end) -> start < end }) {
            "Each school break must have a start time before its end time."
        }
        require(breaks.zipWithNext().none { (first, second) -> first.second > second.first }) {
            "School breaks cannot overlap."
        }

        return buildList {
            var start = 7 * 60
            while (start + 60 <= 18 * 60) {
                val intersectingBreak = breaks.firstOrNull { (breakStart, breakEnd) ->
                    start < breakEnd && breakStart < start + 60
                }
                if (intersectingBreak != null) {
                    start = intersectingBreak.second
                } else {
                    add(SchoolPeriodSlot(formatMinutesToTime(start), formatMinutesToTime(start + 60)))
                    start += 60
                }
            }
        }
    }

    /**
     * Converts "HH:mm" time string into total minutes from midnight (0..1439).
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return 0
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        return (hours * 60) + minutes
    }

    /**
     * Formats minutes from midnight into "HH:mm".
     */
    fun formatMinutesToTime(minutesOfDay: Int): String {
        val hrs = (minutesOfDay / 60) % 24
        val mins = minutesOfDay % 60
        return String.format("%02d:%02d", hrs, mins)
    }

    fun periodUnitCount(startTime: String, endTime: String): Int {
        val durationMinutes = parseTimeToMinutes(endTime) - parseTimeToMinutes(startTime)
        return ((durationMinutes + 59) / 60).coerceAtLeast(1)
    }

    fun dayOfWeekFromDate(date: String): Int {
        val parts = date.split("-")
        require(parts.size == 3) { "Date must use yyyy-MM-dd format." }
        val year = parts[0].toIntOrNull() ?: throw IllegalArgumentException("Invalid date: $date")
        val month = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid date: $date")
        val day = parts[2].toIntOrNull() ?: throw IllegalArgumentException("Invalid date: $date")
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> error("Unrecognized weekday for date: $date")
        }
    }

    fun resolveDayScheduleForDate(
        date: String,
        currentTimeStr: String,
        allPeriods: List<PeriodEntity>,
        subjectMap: Map<String, SubjectEntity>,
        isVacation: Boolean,
        vacationResumesDate: String = ""
    ): DayScheduleStatus = resolveDaySchedule(
        currentTimeStr = currentTimeStr,
        periodsToday = allPeriods.filter { it.dayOfWeek == dayOfWeekFromDate(date) },
        subjectMap = subjectMap,
        isVacation = isVacation,
        vacationResumesDate = vacationResumesDate
    )

    /**
     * Groups raw PeriodEntity records for a weekday into unified ClassBlocks,
     * merging contiguous periods with the same blockId into a single continuous block.
     */
    fun groupPeriodsIntoBlocks(
        periods: List<PeriodEntity>,
        subjectMap: Map<String, SubjectEntity>
    ): List<ClassBlock> {
        if (periods.isEmpty()) return emptyList()

        // Group periods by blockId
        val groupedByBlock = periods.groupBy { it.blockId }
        val blocks = mutableListOf<ClassBlock>()

        for ((blockId, periodList) in groupedByBlock) {
            val sorted = periodList.sortedBy { parseTimeToMinutes(it.startTime) }
            val first = sorted.first()
            val last = sorted.last()

            val subject = subjectMap[first.subjectId]
            val subjectName = subject?.name ?: "Unknown Subject"
            val className = first.className.ifBlank {
                subject?.assignedClasses?.ifBlank { "SHS" } ?: "SHS"
            }
            val classroom = first.classroom.ifBlank { subject?.defaultClassroom ?: "Classroom" }
            val colorHex = subject?.colorHex ?: "#1E6F5C"

            val startMin = parseTimeToMinutes(first.startTime)
            val endMin = parseTimeToMinutes(last.endTime)
            val duration = (endMin - startMin).coerceAtLeast(40)
            val isDouble = sorted.size > 1 || duration >= 80

            blocks.add(
                ClassBlock(
                    blockId = blockId,
                    subjectId = first.subjectId,
                    subjectName = subjectName,
                    className = className,
                    classroom = classroom,
                    colorHex = colorHex,
                    startTime = first.startTime,
                    endTime = last.endTime,
                    isDoublePeriod = isDouble,
                    durationMinutes = duration
                )
            )
        }

        // Sort blocks chronologically by start time
        blocks.sortBy { parseTimeToMinutes(it.startTime) }

        // Check for conflicts / overlaps between blocks
        val conflicts = mutableSetOf<String>()
        for (i in 0 until blocks.size) {
            for (j in i + 1 until blocks.size) {
                val a = blocks[i]
                val b = blocks[j]
                val aStart = parseTimeToMinutes(a.startTime)
                val aEnd = parseTimeToMinutes(a.endTime)
                val bStart = parseTimeToMinutes(b.startTime)
                val bEnd = parseTimeToMinutes(b.endTime)

                // Overlap condition: not disjoint
                if (aStart < bEnd && bStart < aEnd) {
                    conflicts.add(a.blockId)
                    conflicts.add(b.blockId)
                }
            }
        }

        return blocks.map { block ->
            if (block.blockId in conflicts) block.copy(hasConflict = true) else block
        }
    }

    /**
     * Resolves the real-time schedule state for today.
     * Takes current time string "HH:mm", periods for today, subject catalog map,
     * and vacation state.
     */
    fun resolveDaySchedule(
        currentTimeStr: String,
        periodsToday: List<PeriodEntity>,
        subjectMap: Map<String, SubjectEntity>,
        isVacation: Boolean,
        vacationResumesDate: String = ""
    ): DayScheduleStatus {
        if (isVacation) {
            return DayScheduleStatus.Vacation(
                reason = "School is currently on vacation or term recess.",
                resumesDate = vacationResumesDate
            )
        }

        val blocks = groupPeriodsIntoBlocks(periodsToday, subjectMap)
        if (blocks.isEmpty()) {
            return DayScheduleStatus.Empty("No classes scheduled for today.")
        }

        val currentMinutes = parseTimeToMinutes(currentTimeStr)

        var currentBlock: ClassBlock? = null
        var nextBlock: ClassBlock? = null

        // 1. Check if a block is active now
        for (block in blocks) {
            val startMin = parseTimeToMinutes(block.startTime)
            val endMin = parseTimeToMinutes(block.endTime)
            if (currentMinutes in startMin until endMin) {
                currentBlock = block
                break
            }
        }

        // 2. Find the earliest subsequent block
        for (block in blocks) {
            val startMin = parseTimeToMinutes(block.startTime)
            if (currentBlock != null) {
                // If in class, next block must start at or after current class ends
                val currentEndMin = parseTimeToMinutes(currentBlock.endTime)
                if (startMin >= currentEndMin) {
                    nextBlock = block
                    break
                }
            } else {
                // Not in class, next block is first one in the future
                if (startMin > currentMinutes) {
                    nextBlock = block
                    break
                }
            }
        }

        // 3. Compute human-readable countdown text
        val countdownText = when {
            currentBlock != null -> {
                val remaining = parseTimeToMinutes(currentBlock.endTime) - currentMinutes
                if (remaining <= 1) "Ends in less than a minute"
                else "Ends in $remaining mins"
            }
            nextBlock != null -> {
                val untilStart = parseTimeToMinutes(nextBlock.startTime) - currentMinutes
                when {
                    untilStart <= 0 -> "Starting now"
                    untilStart < 60 -> "Starts in $untilStart mins"
                    else -> {
                        val hrs = untilStart / 60
                        val mins = untilStart % 60
                        if (mins == 0) "Starts in $hrs hr"
                        else "Starts in $hrs hr ${mins} mins"
                    }
                }
            }
            else -> "All classes finished for today"
        }

        val conflicting = blocks.filter { it.hasConflict }

        return DayScheduleStatus.Active(
            currentBlock = currentBlock,
            nextBlock = nextBlock,
            countdownText = countdownText,
            allBlocksToday = blocks,
            conflictingBlocks = conflicting
        )
    }
}
