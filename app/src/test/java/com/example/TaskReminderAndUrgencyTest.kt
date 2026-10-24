package com.example

import com.example.data.alarm.TaskReminderScheduler
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
    fun urgencyIdentifiesOverdueTodaySoonAndUpcomingTasks() {
        assertEquals(TaskUrgency.OVERDUE, taskUrgency("2026-10-24", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_TODAY, taskUrgency("2026-10-25", "2026-10-25"))
        assertEquals(TaskUrgency.DUE_SOON, taskUrgency("2026-10-27", "2026-10-25"))
        assertEquals(TaskUrgency.UPCOMING, taskUrgency("2026-10-28", "2026-10-25"))
        assertEquals(TaskUrgency.UNKNOWN, taskUrgency("invalid", "2026-10-25"))
    }
}
