package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.EdenseDataExport
import com.example.data.export.EdenseDataExporter
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserDataExportDatabaseTest {
    private lateinit var database: EdenseDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EdenseDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun exportReadsEveryLocalTable() = runBlocking {
        val profile = TeacherProfileEntity(name = "Teacher")
        val calendar = AcademicCalendarEntity(academicYear = "2026/2027")
        val term = AcademicTermEntity("term-1", "Semester 1", "2026-09-01", "2026-12-18", 0)
        val subject = SubjectEntity(id = "subject-1", name = "Mathematics")
        val period = PeriodEntity(
            id = "period-1",
            subjectId = subject.id,
            dayOfWeek = 1,
            startTime = "07:00",
            endTime = "08:00",
            blockId = "block-1"
        )
        val session = TeachingSessionEntity(
            id = "session-1",
            subjectId = subject.id,
            sessionDate = "2026-10-25",
            lastTopicCovered = "Algebra",
            nextTopicPlanned = "Geometry"
        )
        val task = TaskEntity(
            id = "task-1",
            subjectId = subject.id,
            title = "Assignment",
            dueDate = "2026-10-30"
        )

        database.teacherProfileDao().insertOrUpdate(profile)
        database.academicCalendarDao().insertOrUpdate(calendar)
        database.academicTermDao().replaceAll(listOf(term))
        database.subjectDao().insertOrUpdate(subject)
        database.periodDao().insertPeriod(period)
        database.teachingSessionDao().insertSession(session)
        database.taskDao().insertTask(task)

        val adapter = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter(EdenseDataExport::class.java)
        val export = adapter.fromJson(EdenseDataExporter.export(database))

        assertEquals(profile, export?.profile)
        assertEquals(calendar, export?.academicCalendar)
        assertEquals(listOf(term), export?.academicTerms)
        assertEquals(listOf(subject), export?.subjects)
        assertEquals(listOf(period), export?.periods)
        assertEquals(listOf(session), export?.teachingSessions)
        assertEquals(listOf(task), export?.tasks)
    }
}
