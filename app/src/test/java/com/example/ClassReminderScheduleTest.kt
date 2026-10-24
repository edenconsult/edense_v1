package com.example

import com.example.data.alarm.AlarmScheduler
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ClassReminderScheduleTest {
    @Test
    fun nextReminderUsesNextWeekdayOccurrence() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 25, 12, 0)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 26, 6, 50)
        }

        val actual = AlarmScheduler.nextReminderTimeMillis(
            nowMillis = now.timeInMillis,
            dayOfWeek = 1,
            startTime = "07:00",
            leadMinutes = 10,
            academicCalendar = null,
            academicTerms = emptyList()
        )

        assertEquals(expected.timeInMillis, actual)
    }

    @Test
    fun nextReminderSkipsDatesBetweenTerms() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 26, 0, 1)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 2, 6, 50)
        }
        val calendar = AcademicCalendarEntity(pauseAlarmsDuringVacation = true)
        val terms = listOf(
            AcademicTermEntity("term1", "Term 1", "2026-09-01", "2026-10-20", 0),
            AcademicTermEntity("term2", "Term 2", "2026-11-02", "2026-12-18", 1)
        )

        val actual = AlarmScheduler.nextReminderTimeMillis(
            nowMillis = now.timeInMillis,
            dayOfWeek = 1,
            startTime = "07:00",
            leadMinutes = 10,
            academicCalendar = calendar,
            academicTerms = terms
        )

        assertEquals(expected.timeInMillis, actual)
    }

    @Test
    fun nextReminderIsNotScheduledDuringManualVacation() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 25, 12, 0)
        }

        val actual = AlarmScheduler.nextReminderTimeMillis(
            nowMillis = now.timeInMillis,
            dayOfWeek = 1,
            startTime = "07:00",
            leadMinutes = 10,
            academicCalendar = AcademicCalendarEntity(
                pauseAlarmsDuringVacation = false,
                isManualVacationMode = true
            ),
            academicTerms = emptyList()
        )

        assertNull(actual)
    }
}
