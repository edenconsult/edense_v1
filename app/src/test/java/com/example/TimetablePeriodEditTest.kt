package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.PendingDeletionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.repository.EdenseRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimetablePeriodEditTest {
    private lateinit var database: EdenseDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EdenseDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun editingDoubleBlockToSinglePeriodKeepsBlockAndQueuesRemovedPeriodDeletion() = runBlocking {
        val subject = SubjectEntity(
            id = "math",
            name = "Mathematics",
            isSelected = true,
            assignedClasses = "SHS 2B",
            defaultClassroom = "Room 14"
        )
        val blockId = "math-block"
        val existingPeriods = listOf(
            PeriodEntity(
                id = "first-period",
                subjectId = subject.id,
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "08:00",
                blockId = blockId
            ),
            PeriodEntity(
                id = "second-period",
                subjectId = subject.id,
                dayOfWeek = 1,
                startTime = "08:00",
                endTime = "09:00",
                blockId = blockId
            )
        )
        database.subjectDao().insertOrUpdate(subject)
        database.periodDao().insertPeriods(existingPeriods)

        val repository = EdenseRepository(
            database = database,
            appContext = ApplicationProvider.getApplicationContext()
        )
        repository.updatePeriodBlock(
            blockId = blockId,
            subjectId = subject.id,
            dayOfWeek = 2,
            startTime = "10:30",
            className = "SHS 2B",
            classroom = "Room 14",
            isDoublePeriod = false
        )

        val updated = database.periodDao().getAllPeriodsDirect()
        assertEquals(1, updated.size)
        assertEquals("first-period", updated.single().id)
        assertEquals(2, updated.single().dayOfWeek)
        assertEquals("10:30", updated.single().startTime)
        assertEquals("11:30", updated.single().endTime)
        assertEquals(blockId, updated.single().blockId)
        assertEquals(
            listOf(PendingDeletionEntity("periods", "second-period")),
            database.pendingDeletionDao().getPendingDeletions()
        )
        assertTrue(updated.none { it.id == "second-period" })
    }
}
