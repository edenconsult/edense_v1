package com.example.data.remote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.PendingDeletionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseSyncWorkAndDeletionTest {
    private lateinit var database: EdenseDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EdenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun backgroundSyncRequiresInternetAndUsesTheSyncWorker() {
        val request = FirebaseSyncWorkScheduler.createRequest("teacher-a")

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(FirebaseSyncWorker::class.java.name, request.workSpec.workerClassName)
        assertEquals(
            "teacher-a",
            request.workSpec.input.getString(FirebaseSyncWorkScheduler.ACCOUNT_UID_INPUT_KEY)
        )
    }

    @Test
    fun successfulRemoteDeletionRemovesItsPendingTombstone() = runBlocking {
        val deletion = PendingDeletionEntity("tasks", "task-1")
        val dao = database.pendingDeletionDao()
        dao.insert(deletion)
        val applied = mutableListOf<PendingDeletionEntity>()

        PendingDeletionSync(dao).sync { applied.add(it) }

        assertEquals(listOf(deletion), applied)
        assertTrue(dao.getPendingDeletions().isEmpty())
    }

    @Test
    fun failedRemoteDeletionKeepsTombstoneForRetry() = runBlocking {
        val deletion = PendingDeletionEntity("sessions", "session-1")
        val dao = database.pendingDeletionDao()
        dao.insert(deletion)
        var failureObserved = false

        try {
            PendingDeletionSync(dao).sync {
                throw IllegalStateException("Firestore unavailable")
            }
        } catch (_: IllegalStateException) {
            failureObserved = true
        }

        assertTrue(failureObserved)
        assertEquals(listOf(deletion), dao.getPendingDeletions())
    }
}
