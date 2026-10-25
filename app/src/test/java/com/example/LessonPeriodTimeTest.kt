package com.example

import com.example.data.alarm.LessonLogPromptScheduler
import com.example.data.alarm.TaskReminderScheduler
import com.example.data.local.entity.PeriodEntity
import com.example.domain.schedule.lessonEndTimeMillis
import com.example.domain.schedule.isLessonPeriodPast
import com.example.domain.schedule.periodsForSessionDate
import com.example.domain.schedule.sessionDateDayOfWeek
import com.example.domain.schedule.sessionPeriodMatchesDate
import com.example.domain.task.PeriodReminderTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class LessonPeriodTimeTest {
    @Test
    fun sessionDateMustMatchTheSelectedTimetableWeekday() {
        assertEquals(7, sessionDateDayOfWeek("2026-10-25"))
        assertEquals(1, sessionDateDayOfWeek("2026-10-26"))
        assertTrue(sessionPeriodMatchesDate("2026-10-26", 1))
        assertFalse(sessionPeriodMatchesDate("2026-10-25", 1))
        assertFalse(sessionPeriodMatchesDate("2026-02-30", 1))
    }

    @Test
    fun changingFromSundayToMondayLoadsTheSubjectsMondayPeriod() {
        val mondayPeriod = PeriodEntity(
            id = "monday-period",
            subjectId = "math",
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "09:00",
            blockId = "monday-block",
            className = "3A"
        )
        val tuesdayPeriod = mondayPeriod.copy(
            id = "tuesday-period",
            dayOfWeek = 2,
            blockId = "tuesday-block"
        )

        assertTrue(periodsForSessionDate(listOf(mondayPeriod, tuesdayPeriod), "math", "2026-10-25").isEmpty())
        assertEquals(
            listOf(mondayPeriod),
            periodsForSessionDate(listOf(mondayPeriod, tuesdayPeriod), "math", "2026-10-26")
        )
        assertTrue(periodsForSessionDate(listOf(mondayPeriod), "english", "2026-10-26").isEmpty())
    }

    @Test
    fun sessionPeriodTimeIsAValidLocalTimestamp() {
        assertNotNull(lessonEndTimeMillis("2026-10-26", "08:00"))
        assertNull(lessonEndTimeMillis("2026-10-26", "25:00"))
        assertNull(lessonEndTimeMillis("2026-02-30", "08:00"))
    }

    @Test
    fun aSessionCanBeLoggedOneMinuteAfterThePeriodEndsButNotBefore() {
        val end = lessonEndTimeMillis("2026-10-26", "08:00")!!
        assertFalse(isLessonPeriodPast("2026-10-26", "08:00", end))
        assertTrue(isLessonPeriodPast("2026-10-26", "08:00", end + 60_000))
    }

    @Test
    fun periodTaskRemindersUseFiveMinuteBoundariesAndNextOccurrence() {
        val period = PeriodEntity(
            id = "period",
            subjectId = "math",
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "09:00",
            blockId = "block"
        )
        val monday = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 26, 7, 0)
        }.timeInMillis

        assertEquals(
            Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.OCTOBER, 26, 7, 55)
            }.timeInMillis,
            TaskReminderScheduler.periodReminderTimeMillis(
                period,
                PeriodReminderTiming.BEFORE_START.name,
                monday
            )
        )
        assertEquals(
            Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.OCTOBER, 26, 8, 55)
            }.timeInMillis,
            TaskReminderScheduler.periodReminderTimeMillis(
                period,
                PeriodReminderTiming.BEFORE_END.name,
                monday
            )
        )
        assertEquals(
            Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.OCTOBER, 26, 9, 0)
            }.timeInMillis,
            TaskReminderScheduler.periodReminderTimeMillis(
                period,
                PeriodReminderTiming.AFTER_END.name,
                monday
            )
        )
    }

    @Test
    fun lessonLogPromptUsesTheEndOfTheSingleOrDoublePeriodBlock() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 26, 8, 0)
        }.timeInMillis

        val nextEnd = LessonLogPromptScheduler.nextLessonEndTimeMillis(
            nowMillis = now,
            dayOfWeek = 1,
            endTime = "10:00",
            academicCalendar = null,
            academicTerms = emptyList()
        )

        assertEquals(
            Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.OCTOBER, 26, 10, 0)
            }.timeInMillis,
            nextEnd
        )
    }
}
