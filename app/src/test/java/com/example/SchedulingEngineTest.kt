package com.example

import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.SubjectEntity
import com.example.domain.schedule.ClassroomAssignments
import com.example.domain.schedule.DayScheduleStatus
import com.example.domain.schedule.SchedulingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingEngineTest {

    private val mathSubject = SubjectEntity(
        id = "math_1",
        name = "Core Mathematics",
        assignedClasses = "SHS 2B",
        defaultClassroom = "Room 14",
        colorHex = "#1E6F5C"
    )

    private val englishSubject = SubjectEntity(
        id = "eng_1",
        name = "English Language",
        assignedClasses = "SHS 2B",
        defaultClassroom = "Room 14",
        colorHex = "#0F1E36"
    )

    private val subjectMap = mapOf(
        "math_1" to mathSubject,
        "eng_1" to englishSubject
    )

    @Test
    fun doublePeriod_groupsIntoSingleContinuousBlock() {
        val periods = listOf(
            PeriodEntity(
                id = "p1",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "08:00",
                blockId = "double_math_block",
                classroom = "Room 14"
            ),
            PeriodEntity(
                id = "p2",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "08:00",
                endTime = "09:00",
                blockId = "double_math_block",
                classroom = "Room 14"
            )
        )

        val blocks = SchedulingEngine.groupPeriodsIntoBlocks(periods, subjectMap)

        assertEquals(1, blocks.size)
        val block = blocks.first()
        assertEquals("07:00", block.startTime)
        assertEquals("09:00", block.endTime)
        assertTrue(block.isDoublePeriod)
        assertEquals(120, block.durationMinutes)
    }

    @Test
    fun schoolPeriodSlots_areOneHourAndSkipBothBreaks() {
        val slots = SchedulingEngine.periodSlots

        assertTrue(slots.all {
            SchedulingEngine.parseTimeToMinutes(it.endTime) -
                SchedulingEngine.parseTimeToMinutes(it.startTime) == 60
        })
        assertTrue(slots.none { it.startTime == "09:00" || it.startTime == "12:30" })
        assertTrue(slots.any { it.startTime == "09:30" })
        assertTrue(slots.any { it.startTime == "13:00" })
    }

    @Test
    fun doublePeriods_canStartAtAnySlotWithAnAdjacentSlotAndCannotCrossBreak() {
        val slots = SchedulingEngine.periodSlots
        val consecutiveStarts = slots.zipWithNext()
            .filter { (first, second) -> first.endMinute == second.startMinute }
            .map { (first, _) -> first.startTime }

        assertTrue("07:00" in consecutiveStarts)
        assertTrue("08:00" !in consecutiveStarts)
        assertTrue("09:30" in consecutiveStarts)
        assertTrue("11:30" !in consecutiveStarts)
        assertTrue("13:00" in consecutiveStarts)
    }

    @Test
    fun schoolPeriodSlots_useConfiguredBreakTimes() {
        val slots = SchedulingEngine.periodSlots(
            breakfastBreakStart = "09:15",
            breakfastBreakEnd = "09:45",
            lunchBreakStart = "12:15",
            lunchBreakEnd = "13:00"
        )

        assertTrue(slots.any { it.startTime == "09:45" })
        assertTrue(slots.any { it.startTime == "13:00" })
        assertTrue(slots.none { slot ->
            val start = SchedulingEngine.parseTimeToMinutes(slot.startTime)
            val end = SchedulingEngine.parseTimeToMinutes(slot.endTime)
            start < 9 * 60 + 45 && 9 * 60 + 15 < end ||
                start < 13 * 60 && 12 * 60 + 15 < end
        })
    }

    @Test(expected = IllegalArgumentException::class)
    fun schoolPeriodSlots_rejectOverlappingBreaks() {
        SchedulingEngine.periodSlots(
            breakfastBreakStart = "09:00",
            breakfastBreakEnd = "10:00",
            lunchBreakStart = "09:30",
            lunchBreakEnd = "10:30"
        )
    }

    @Test
    fun doublePeriods_countAsTwoPeriodsForWeeklyTargets() {
        assertEquals(1, SchedulingEngine.periodUnitCount("07:00", "08:00"))
        assertEquals(2, SchedulingEngine.periodUnitCount("07:00", "09:00"))

        val twoOneHourRows = listOf(
            PeriodEntity(
                id = "p1",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "08:00",
                blockId = "double_math"
            ),
            PeriodEntity(
                id = "p2",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "08:00",
                endTime = "09:00",
                blockId = "double_math"
            )
        )
        assertEquals(2, twoOneHourRows.sumOf {
            SchedulingEngine.periodUnitCount(it.startTime, it.endTime)
        })
    }

    @Test
    fun commaSeparatedClassesPairWithRoomsByPosition_andDefaultToClassNames() {
        val classes = ClassroomAssignments.parse(" 3A9, 3A11, 3HE2, 3HE3 ")

        assertEquals(
            listOf("3A9", "3A11", "3HE2", "3HE3"),
            classes
        )
        assertEquals(classes, ClassroomAssignments.defaultRooms(classes))
        assertEquals("Room 11", ClassroomAssignments.roomForClass(
            "3A11",
            classes,
            listOf("Room 9", "Room 11", "Room HE2", "Room HE3")
        ))
        assertEquals("3HE2", ClassroomAssignments.roomForClass("3HE2", classes, emptyList()))
    }

    @Test
    fun academicCalendar_handlesGapsBetweenTermsAndFindsNextResumption() {
        val calendar = AcademicCalendarEntity(
            termStartDate = "2026-09-01",
            termEndDate = "2026-12-18"
        )
        val terms = listOf(
            AcademicTermEntity("term1", "Semester 1", "2026-09-01", "2026-12-18", 0),
            AcademicTermEntity("term2", "Semester 2", "2027-01-10", "2027-04-20", 1)
        )

        assertTrue(calendar.isVacation("2026-12-25", terms))
        assertTrue(!calendar.isVacation("2027-02-01", terms))
        assertEquals("2027-01-10", calendar.nextTermStartDate("2026-12-25", terms))
        assertTrue(calendar.hasValidTerms(terms))
    }

    @Test
    fun academicCalendar_rejectsOverlappingOrInvalidTermDates() {
        val calendar = AcademicCalendarEntity()
        assertTrue(
            !calendar.hasValidTerms(
                listOf(
                    AcademicTermEntity("term1", "Term 1", "2026-09-01", "2026-12-18", 0),
                    AcademicTermEntity("term2", "Term 2", "2026-12-18", "2027-03-01", 1)
                )
            )
        )
        assertTrue(
            !calendar.hasValidTerms(
                listOf(AcademicTermEntity("term1", "Term 1", "bad-date", "2027-03-01", 0))
            )
        )
    }

    @Test
    fun blocks_useTheClassAndClassroomSavedForThatMeeting() {
        val periods = listOf(
            PeriodEntity(
                id = "p1",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "09:30",
                endTime = "10:30",
                blockId = "class_block",
                classroom = "Room 22",
                className = "SHS 3A"
            )
        )

        val block = SchedulingEngine.groupPeriodsIntoBlocks(periods, subjectMap).single()

        assertEquals("SHS 3A", block.className)
        assertEquals("Room 22", block.classroom)
    }

    @Test
    fun doublePeriod_middleOfBlockRemainsActive_neverFalseNextClass() {
        val periods = listOf(
            PeriodEntity(
                id = "p1",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "09:00",
                blockId = "math_block",
                classroom = "Room 14"
            ),
            PeriodEntity(
                id = "p2",
                subjectId = "eng_1",
                dayOfWeek = 1,
                startTime = "09:00",
                endTime = "10:00",
                blockId = "eng_block",
                classroom = "Room 14"
            )
        )

        // At 08:15 (middle of the double period)
        val status = SchedulingEngine.resolveDaySchedule(
            currentTimeStr = "08:15",
            periodsToday = periods,
            subjectMap = subjectMap,
            isVacation = false
        )

        assertTrue(status is DayScheduleStatus.Active)
        val active = status as DayScheduleStatus.Active
        assertNotNull(active.currentBlock)
        assertEquals("Core Mathematics", active.currentBlock?.subjectName)
        assertEquals("07:00", active.currentBlock?.startTime)
        assertEquals("09:00", active.currentBlock?.endTime)

        // Next block must be English at 09:00, NOT a second Math class
        assertNotNull(active.nextBlock)
        assertEquals("English Language", active.nextBlock?.subjectName)
        assertEquals("09:00", active.nextBlock?.startTime)
    }

    @Test
    fun countdownCalculation_accurateBeforeClass() {
        val periods = listOf(
            PeriodEntity(
                id = "p1",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "08:00",
                blockId = "math_single",
                classroom = "Room 14"
            )
        )

        val status = SchedulingEngine.resolveDaySchedule(
            currentTimeStr = "06:45",
            periodsToday = periods,
            subjectMap = subjectMap,
            isVacation = false
        )

        assertTrue(status is DayScheduleStatus.Active)
        val active = status as DayScheduleStatus.Active
        assertNull(active.currentBlock)
        assertNotNull(active.nextBlock)
        assertEquals("Starts in 15 mins", active.countdownText)
    }

    @Test
    fun vacationStatus_suppressesSchedule() {
        val periods = listOf(
            PeriodEntity(
                id = "p1",
                subjectId = "math_1",
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "08:00",
                blockId = "math_single",
                classroom = "Room 14"
            )
        )

        val status = SchedulingEngine.resolveDaySchedule(
            currentTimeStr = "07:30",
            periodsToday = periods,
            subjectMap = subjectMap,
            isVacation = true,
            vacationResumesDate = "2027-01-15"
        )

        assertTrue(status is DayScheduleStatus.Vacation)
        val vacation = status as DayScheduleStatus.Vacation
        assertEquals("2027-01-15", vacation.resumesDate)
    }
}
