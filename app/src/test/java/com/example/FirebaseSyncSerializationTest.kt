package com.example.data.remote

import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeachingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseSyncSerializationTest {
    @Test
    fun teachingSessionCloudFieldsIncludeClassAndCarryForwardContent() {
        val session = TeachingSessionEntity(
            id = "session-1",
            subjectId = "math",
            sessionDate = "2026-10-25",
            lastTopicCovered = "Algebra",
            nextTopicPlanned = "Equations",
            status = "PARTIAL",
            notes = "Continue exercise",
            className = "3A9",
            periodBlockId = "block-1",
            createdAt = 42L
        )

        val fields = session.toFirestoreFields("teacher-1")

        assertEquals("teacher-1", fields["userId"])
        assertEquals("3A9", fields["className"])
        assertEquals("Algebra", fields["lastTopicCovered"])
        assertEquals("Equations", fields["nextTopicPlanned"])
        assertEquals("PARTIAL", fields["status"])
        assertEquals("block-1", fields["periodBlockId"])
        assertEquals(42L, fields["createdAt"])
    }

    @Test
    fun taskCloudFieldsIncludeCompletionReminderAndClassData() {
        val task = TaskEntity(
            id = "task-1",
            subjectId = "math",
            title = "Prepare next lesson",
            type = "FOLLOW_UP",
            dueDate = "2026-10-28",
            reminderTime = "06:00",
            isCompleted = true,
            notes = "Created from lesson log",
            className = "3A9",
            createdAt = 84L
        )

        val fields = task.toFirestoreFields("teacher-1")

        assertEquals("teacher-1", fields["userId"])
        assertEquals("FOLLOW_UP", fields["type"])
        assertEquals("3A9", fields["className"])
        assertEquals("2026-10-28", fields["dueDate"])
        assertEquals("06:00", fields["reminderTime"])
        assertTrue(fields["isCompleted"] as Boolean)
        assertEquals(84L, fields["createdAt"])
        assertFalse(fields.isEmpty())
    }
}
