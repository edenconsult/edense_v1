package com.example

import com.example.data.alarm.TaskReminderScheduler
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.task.TaskUrgency
import com.example.domain.task.taskUrgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class TaskReminderAndUrgencyTest {
    @Test
    fun reminderUsesTaskDueDateAndReminderTime() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2099, Calendar.JANUARY, 4, 12, 0)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2099, Calendar.JANUARY, 5, 6, 0)
        }
        val task = TaskEntity(
            id = "task",
            title = "Submit assignment",
            dueDate = "2099-01-05",
            reminderTime = "06:00"
        )

        assertEquals(
            expected.timeInMillis,
            TaskReminderScheduler.reminderTimeMillis(task, now.timeInMillis)
        )
    }

    @Test
    fun reminderDoesNotSchedulePastOrInvalidTimes() {
        val task = TaskEntity(
            id = "task",
            title = "Submit assignment",
            dueDate = "2000-01-05",
            reminderTime = "06:00"
        )
        assertNull(TaskReminderScheduler.reminderTimeMillis(task, System.currentTimeMillis()))
        assertNull(
            TaskReminderScheduler.reminderTimeMillis(
                task.copy(dueDate = "2099-01-05", reminderTime = "25:00"),
                System.currentTimeMillis()
            )
        )
    }

    @Test
    fun dueDateReminderDuringVacationMovesToTheNextActiveDate() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 25, 12, 0)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 2, 6, 0)
        }
        val calendar = AcademicCalendarEntity(
            termStartDate = "2026-11-02",
            termEndDate = "2026-12-18",
            pauseAlarmsDuringVacation = true
        )
        val task = TaskEntity(
            id = "vacation-task",
            title = "Submit assignment",
            dueDate = "2026-10-26",
            reminderTime = "06:00"
        )

        assertEquals(
            expected.timeInMillis,
            TaskReminderScheduler.reminderTimeMillis(task, now.timeInMillis, calendar)
        )
    }

    @Test
    fun overdueReminderIsHeldDuringVacationUntilTheNextActiveDate() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 25, 12, 0)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 2, 6, 0)
        }
        val calendar = AcademicCalendarEntity(
            pauseAlarmsDuringVacation = true
        )
        val terms = listOf(
            AcademicTermEntity("term-1", "Term 1", "2026-09-01", "2026-10-20", 0),
            AcademicTermEntity("term-2", "Term 2", "2026-11-02", "2026-12-18", 1)
        )
        val task = TaskEntity(
            id = "overdue-task",
            title = "Submit assignment",
            dueDate = "2026-10-20",
            reminderTime = "06:00"
        )

        assertEquals(
            expected.timeInMillis,
            TaskReminderScheduler.reminderTimeMillis(task, now.timeInMillis, calendar, terms)
        )
    }

    @Test
    fun linkedPeriodReminderSkipsVacationUntilNextActiveOccurrence() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 25, 12, 0)
        }
        val expected = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 2, 6, 55)
        }
        val calendar = AcademicCalendarEntity(
            termStartDate = "2026-11-02",
            termEndDate = "2026-12-18",
            pauseAlarmsDuringVacation = true
        )
        val mondayPeriod = PeriodEntity(
            id = "period",
            subjectId = "math",
            dayOfWeek = 1,
            startTime = "07:00",
            endTime = "08:00",
            blockId = "math-block"
        )

        assertEquals(
            expected.timeInMillis,
            TaskReminderScheduler.periodReminderTimeMillis(
                mondayPeriod,
                "BEFORE_START",
                now.timeInMillis,
                calendar
            )
        )
    }

    @Test
    fun manualVacationPausesTaskRemindersUntilCalendarIsRescheduled() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 25, 12, 0)
        }
        val task = TaskEntity(
            id = "vacation-task",
            title = "Submit assignment",
            dueDate = "2026-10-26",
            reminderTime = "06:00"
        )
        val calendar = AcademicCalendarEntity(isManualVacationMode = true)

        assertNull(TaskReminderScheduler.reminderTimeMillis(task, now.timeInMillis, calendar))
        assertNull(
            TaskReminderScheduler.reminderTimeMillis(
                task,
                now.timeInMillis,
                calendar.copy(pauseAlarmsDuringVacation = false)
            )
        )
    }

    @Test
    fun urgencyIdentifiesOverdueTodaySoonAndUpcomingTasks() {
        assertEquals(TaskUrgency.OVERDUE, taskUrgency("2026-10-24", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_TODAY, taskUrgency("2026-10-25", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_SOON, taskUrgency("2026-10-26", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_SOON, taskUrgency("2026-10-27", "2026-10-25"))
        assertEquals(TaskUrgency.UPCOMING, taskUrgency("2026-10-28", "2026-10-25"))
        assertEquals(TaskUrgency.UNKNOWN, taskUrgency("invalid", "2026-10-25"))
    }

    @Test
    fun dueSoonIsTheNextTwoCalendarDatesAndBecomesTodayWhenDateAdvances() {
        assertEquals(TaskUrgency.DUE_SOON, taskUrgency("2026-10-26", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_SOON, taskUrgency("2026-10-27", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_TODAY, taskUrgency("2026-10-26", "2026-10-26"))
        assertEquals(TaskUrgency.UPCOMING, taskUrgency("2026-10-28", "2026-10-25"))
    }
}
