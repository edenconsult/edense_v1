package com.example.domain.schedule

import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity

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
    val periodSlots: List<SchoolPeriodSlot> = buildList {
        var start = 7 * 60
        while (start + 60 <= 18 * 60) {
            if (start == 9 * 60) start = 9 * 60 + 30
            if (start == 12 * 60 + 30) start = 13 * 60
            add(SchoolPeriodSlot(formatMinutesToTime(start), formatMinutesToTime(start + 60)))
            start += 60
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
