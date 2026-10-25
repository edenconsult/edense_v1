package com.example

import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.task.TaskPriorityFilter
import com.example.domain.task.TaskSortOrder
import com.example.domain.task.TaskUrgency
import com.example.domain.task.priorityRank
import com.example.domain.task.taskUrgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFilterAndSortTest {

    private val subjects = listOf(
        SubjectEntity(id = "sub1", name = "Core Mathematics", category = "CORE"),
        SubjectEntity(id = "sub2", name = "Biology", category = "ELECTIVE")
    )

    private val sampleTasks = listOf(
        TaskEntity(id = "1", title = "Math Homework", subjectId = "sub1", type = "ASSIGNMENT", dueDate = "2026-10-30", reminderTime = "08:00"),
        TaskEntity(id = "2", title = "Bio Quiz", subjectId = "sub2", type = "TEST", dueDate = "2026-10-24", reminderTime = "07:00"),
        TaskEntity(id = "3", title = "Math Exam", subjectId = "sub1", type = "EXAM", dueDate = "2026-10-25", reminderTime = "09:00"),
        TaskEntity(id = "4", title = "Bio Project", subjectId = "sub2", type = "PROJECT", dueDate = "2026-10-27", reminderTime = "10:00")
    )

    @Test
    fun priorityRankOrdersFromMostUrgentToLeastUrgent() {
        assertTrue(TaskUrgency.OVERDUE.priorityRank() < TaskUrgency.DUE_TODAY.priorityRank())
        assertTrue(TaskUrgency.DUE_TODAY.priorityRank() < TaskUrgency.DUE_SOON.priorityRank())
        assertTrue(TaskUrgency.DUE_SOON.priorityRank() < TaskUrgency.UPCOMING.priorityRank())
    }

    @Test
    fun sortByDueDateAscendingOrdersEarliestFirst() {
        val sorted = sampleTasks.sortedWith { a, b ->
            val cmp = a.dueDate.compareTo(b.dueDate)
            if (cmp != 0) cmp else a.reminderTime.compareTo(b.reminderTime)
        }
        assertEquals("2", sorted[0].id) // 2026-10-24
        assertEquals("3", sorted[1].id) // 2026-10-25
        assertEquals("4", sorted[2].id) // 2026-10-27
        assertEquals("1", sorted[3].id) // 2026-10-30
    }

    @Test
    fun sortByDueDateDescendingOrdersLatestFirst() {
        val sorted = sampleTasks.sortedWith { a, b ->
            val cmp = b.dueDate.compareTo(a.dueDate)
            if (cmp != 0) cmp else b.reminderTime.compareTo(a.reminderTime)
        }
        assertEquals("1", sorted[0].id) // 2026-10-30
        assertEquals("4", sorted[1].id) // 2026-10-27
        assertEquals("3", sorted[2].id) // 2026-10-25
        assertEquals("2", sorted[3].id) // 2026-10-24
    }

    @Test
    fun sortByPriorityOrdersOverdueAndDueTodayFirst() {
        val today = "2026-10-25"
        val sorted = sampleTasks.sortedWith { a, b ->
            val rankA = taskUrgency(a.dueDate, today).priorityRank()
            val rankB = taskUrgency(b.dueDate, today).priorityRank()
            val cmp = rankA.compareTo(rankB)
            if (cmp != 0) cmp else a.dueDate.compareTo(b.dueDate)
        }
        // id 2 is 2026-10-24 (Overdue, rank 1)
        // id 3 is 2026-10-25 (Due Today, rank 2)
        // id 4 is 2026-10-27 (Due Soon, rank 3)
        // id 1 is 2026-10-30 (Upcoming, rank 4)
        assertEquals("2", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("4", sorted[2].id)
        assertEquals("1", sorted[3].id)
    }

    @Test
    fun sortBySubjectOrdersAlphabetically() {
        val subjectMap = subjects.associateBy { it.id }
        val sorted = sampleTasks.sortedWith { a, b ->
            val subA = subjectMap[a.subjectId]?.name ?: ""
            val subB = subjectMap[b.subjectId]?.name ?: ""
            val cmp = subA.compareTo(subB, ignoreCase = true)
            if (cmp != 0) cmp else a.dueDate.compareTo(b.dueDate)
        }
        // "Biology" comes before "Core Mathematics"
        assertEquals("sub2", sorted[0].subjectId)
        assertEquals("sub2", sorted[1].subjectId)
        assertEquals("sub1", sorted[2].subjectId)
        assertEquals("sub1", sorted[3].subjectId)
    }

    @Test
    fun filterBySubjectFiltersCorrectly() {
        val filtered = sampleTasks.filter { it.subjectId == "sub2" }
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.subjectId == "sub2" })
    }

    @Test
    fun filterByPriorityFiltersCorrectly() {
        val today = "2026-10-25"
        val overdueTasks = sampleTasks.filter { taskUrgency(it.dueDate, today) == TaskUrgency.OVERDUE }
        val dueTodayTasks = sampleTasks.filter { taskUrgency(it.dueDate, today) == TaskUrgency.DUE_TODAY }
        assertEquals(1, overdueTasks.size)
        assertEquals("2", overdueTasks[0].id)
        assertEquals(1, dueTodayTasks.size)
        assertEquals("3", dueTodayTasks[0].id)
    }
}
