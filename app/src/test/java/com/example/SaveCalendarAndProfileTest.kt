package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.repository.EdenseRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaveCalendarAndProfileTest {
    private lateinit var database: EdenseDatabase
    private lateinit var repository: EdenseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EdenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EdenseRepository(database, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveTeacherProfilePersistsChangesCorrectly() = runBlocking {
        val profile = TeacherProfileEntity(
            id = "local_teacher",
            name = "Mad. Grace Owusu",
            schoolName = "Prempeh College",
            schoolStartTime = "07:30",
            schoolEndTime = "14:45",
            reminderLeadMinutes = 10
        )
        repository.saveTeacherProfile(profile)

        val retrieved = database.teacherProfileDao().getProfileDirect()
        assertNotNull(retrieved)
        assertEquals("Mad. Grace Owusu", retrieved?.name)
        assertEquals("Prempeh College", retrieved?.schoolName)
        assertEquals("07:30", retrieved?.schoolStartTime)
        assertEquals("14:45", retrieved?.schoolEndTime)
    }

    @Test
    fun saveAcademicCalendarAndTermsPersistsAndOrdersCorrectly() = runBlocking {
        val calendar = AcademicCalendarEntity(
            id = "active_calendar",
            academicYear = "2026/2027",
            systemType = "SEMESTER",
            activeTermName = "Semester 1",
            termStartDate = "2026-09-01",
            termEndDate = "2026-12-18",
            pauseAlarmsDuringVacation = true,
            isManualVacationMode = false
        )
        val terms = listOf(
            AcademicTermEntity(
                id = "term_1",
                name = "Semester 1",
                startDate = "2026-09-01",
                endDate = "2026-12-18",
                orderIndex = 0
            ),
            AcademicTermEntity(
                id = "term_2",
                name = "Semester 2",
                startDate = "2027-01-10",
                endDate = "2027-06-20",
                orderIndex = 1
            )
        )

        repository.saveAcademicCalendar(calendar)
        repository.saveAcademicTerms(terms)

        val savedCalendar = database.academicCalendarDao().getCalendarDirect()
        val savedTerms = database.academicTermDao().getTermsDirect()

        assertNotNull(savedCalendar)
        assertEquals("2026/2027", savedCalendar?.academicYear)
        assertEquals("Semester 1", savedCalendar?.activeTermName)
        assertEquals(2, savedTerms.size)
        assertEquals("Semester 1", savedTerms[0].name)
        assertEquals("Semester 2", savedTerms[1].name)
    }
}
