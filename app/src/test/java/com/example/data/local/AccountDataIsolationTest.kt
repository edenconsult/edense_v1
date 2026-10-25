package com.example.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.PendingDeletionEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeachingSessionEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDataIsolationTest {
    private lateinit var context: android.content.Context
    private lateinit var database: EdenseDatabase
    private lateinit var isolation: AccountDataIsolation

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("edense_local_account", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        File(context.filesDir, "account_data")
            .resolve("legacy_unassigned.json")
            .delete()
        database = Room.inMemoryDatabaseBuilder(context, EdenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        isolation = AccountDataIsolation(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun switchingAccountsRestoresOnlyEachAccountsLocalDataAndDeletionQueue() = runBlocking {
        isolation.switchToAccount("teacher-a", database)
        val subjectA = SubjectEntity(
            id = "subject-a",
            name = "Teacher A subject",
            isSelected = true
        )
        database.subjectDao().insertOrUpdate(subjectA)
        database.teachingSessionDao().insertSession(
            TeachingSessionEntity(
                id = "session-a",
                subjectId = subjectA.id,
                sessionDate = "2026-10-25",
                lastTopicCovered = "",
                nextTopicPlanned = "",
                notes = "Teacher A private notes"
            )
        )
        database.pendingDeletionDao().insert(PendingDeletionEntity("tasks", "deleted-a"))

        isolation.switchToAccount("teacher-b", database)

        assertNull(database.teacherProfileDao().getProfileDirect())
        assertTrue(database.teachingSessionDao().getAllSessionsDirect().isEmpty())
        assertTrue(database.pendingDeletionDao().getPendingDeletions().isEmpty())
        assertFalse(database.subjectDao().getAllSubjectsDirect().any { it.id == subjectA.id })

        val subjectB = SubjectEntity(
            id = "subject-b",
            name = "Teacher B subject",
            isSelected = true
        )
        database.subjectDao().insertOrUpdate(subjectB)
        database.taskDao().insertTask(
            TaskEntity(
                id = "task-b",
                subjectId = subjectB.id,
                title = "Teacher B task",
                dueDate = "2026-10-30"
            )
        )
        database.pendingDeletionDao().insert(PendingDeletionEntity("sessions", "deleted-b"))

        isolation.switchToAccount("teacher-a", database)

        assertEquals(listOf("session-a"), database.teachingSessionDao().getAllSessionsDirect().map { it.id })
        assertEquals(listOf("subject-a"), database.subjectDao().getSelectedSubjectsDirect().map { it.id })
        assertEquals(
            listOf(PendingDeletionEntity("tasks", "deleted-a")),
            database.pendingDeletionDao().getPendingDeletions()
        )
        assertTrue(database.taskDao().getAllTasksDirect().isEmpty())

        isolation.switchToAccount("teacher-b", database)

        assertEquals(listOf("task-b"), database.taskDao().getAllTasksDirect().map { it.id })
        assertEquals(listOf("subject-b"), database.subjectDao().getSelectedSubjectsDirect().map { it.id })
        assertEquals(
            listOf(PendingDeletionEntity("sessions", "deleted-b")),
            database.pendingDeletionDao().getPendingDeletions()
        )
        assertTrue(database.teachingSessionDao().getAllSessionsDirect().isEmpty())
    }

    @Test
    fun unownedLegacyDataIsQuarantinedInsteadOfClaimedByFirstSignedInAccount() = runBlocking {
        val legacySubject = SubjectEntity(
            id = "legacy-subject",
            name = "Unassigned legacy subject",
            isSelected = true
        )
        database.subjectDao().insertOrUpdate(legacySubject)
        database.periodDao().insertPeriod(
            PeriodEntity(
                id = "legacy-period",
                subjectId = legacySubject.id,
                dayOfWeek = 1,
                startTime = "07:00",
                endTime = "08:00",
                blockId = "legacy-block"
            )
        )

        isolation.switchToAccount("new-account", database)

        assertTrue(database.periodDao().getAllPeriodsDirect().isEmpty())
        assertFalse(database.subjectDao().getAllSubjectsDirect().any { it.id == legacySubject.id })
        assertTrue(File(context.filesDir, "account_data")
            .resolve("legacy_unassigned.json").exists())
    }
}
