package com.example.data.remote

import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseSyncSerializationTest {
    @Test
    fun profileCalendarAndTermFieldsIncludeAllSyncableSettings() {
        val profile = TeacherProfileEntity(
            name = "Amina",
            email = "old@example.com",
            schoolName = "North SHS",
            schoolStartTime = "07:30",
            schoolEndTime = "14:30",
            breakfastBreakStart = "09:15",
            breakfastBreakEnd = "09:45",
            lunchBreakStart = "12:15",
            lunchBreakEnd = "13:00",
            isOnboardingCompleted = true,
            reminderLeadMinutes = 15
        ).toFirestoreFields("teacher-1", "amina@example.com")
        val calendar = AcademicCalendarEntity(
            academicYear = "2026/2027",
            systemType = "TERM",
            activeTermName = "Term 2",
            pauseAlarmsDuringVacation = false,
            isManualVacationMode = true
        ).toFirestoreFields("teacher-1")
        val term = AcademicTermEntity(
            id = "term-2",
            name = "Term 2",
            startDate = "2027-01-10",
            endDate = "2027-04-15",
            orderIndex = 1
        ).toFirestoreFields("teacher-1")

        assertEquals("amina@example.com", profile["email"])
        assertEquals("Amina", profile["name"])
        assertEquals("North SHS", profile["schoolName"])
        assertEquals("09:15", profile["breakfastBreakStart"])
        assertEquals("09:45", profile["breakfastBreakEnd"])
        assertEquals("12:15", profile["lunchBreakStart"])
        assertEquals("13:00", profile["lunchBreakEnd"])
        assertEquals(15, profile["reminderLeadMinutes"])
        assertEquals(true, profile["isOnboardingCompleted"])
        assertEquals("TERM", calendar["systemType"])
        assertEquals(false, calendar["pauseAlarmsDuringVacation"])
        assertEquals(true, calendar["isManualVacationMode"])
        assertEquals("2027-04-15", term["endDate"])
        assertEquals(1, term["orderIndex"])
    }

    @Test
    fun subjectAndPeriodFieldsIncludeSelectionAndTimetableDetails() {
        val subject = SubjectEntity(
            id = "custom-1",
            name = "Robotics",
            category = "ELECTIVE",
            isSelected = false,
            assignedClasses = "3A",
            defaultClassroom = "Lab 2",
            assignedClassrooms = "Lab 2",
            targetPeriodsPerWeek = 4,
            colorHex = "#123456",
            isArchived = true
        ).toFirestoreFields("teacher-1")
        val period = PeriodEntity(
            id = "period-1",
            subjectId = "custom-1",
            dayOfWeek = 3,
            startTime = "09:00",
            endTime = "10:00",
            blockId = "block-1",
            classroom = "Lab 2",
            className = "3A"
        ).toFirestoreFields("teacher-1")

        assertEquals(false, subject["isSelected"])
        assertEquals(true, subject["isArchived"])
        assertEquals("Lab 2", subject["assignedClassrooms"])
        assertEquals(4, subject["targetPeriodsPerWeek"])
        assertEquals(3, period["dayOfWeek"])
        assertEquals("09:00", period["startTime"])
        assertEquals("block-1", period["blockId"])
        assertEquals("3A", period["className"])
    }

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
