package com.example

import com.example.data.export.EdenseDataExport
import com.example.data.export.EdenseDataExporter
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class UserDataExportTest {
    @Test
    fun jsonExportContainsEveryUserDataCategory() {
        val export = EdenseDataExport(
            exportedAtUtc = "2026-10-25T00:00:00Z",
            profile = TeacherProfileEntity(name = "Teacher"),
            academicCalendar = AcademicCalendarEntity(academicYear = "2026/2027"),
            academicTerms = listOf(
                AcademicTermEntity("term-1", "Semester 1", "2026-09-01", "2026-12-18", 0)
            ),
            subjects = listOf(SubjectEntity(id = "subject-1", name = "Mathematics")),
            periods = listOf(
                PeriodEntity(
                    id = "period-1",
                    subjectId = "subject-1",
                    dayOfWeek = 1,
                    startTime = "07:00",
                    endTime = "08:00",
                    blockId = "block-1"
                )
            ),
            teachingSessions = listOf(
                TeachingSessionEntity(
                    id = "session-1",
                    subjectId = "subject-1",
                    sessionDate = "2026-10-25",
                    lastTopicCovered = "Algebra",
                    nextTopicPlanned = "Geometry"
                )
            ),
            tasks = listOf(
                TaskEntity(
                    id = "task-1",
                    subjectId = "subject-1",
                    title = "Assignment",
                    dueDate = "2026-10-30"
                )
            )
        )

        val adapter = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter(EdenseDataExport::class.java)
        val decoded = adapter.fromJson(EdenseDataExporter.toJson(export))

        assertEquals(export, decoded)
    }
}
