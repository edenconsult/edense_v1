package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.GhanaShsCatalog
import com.example.data.local.EdenseDatabase
import com.example.data.repository.EdenseRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomSubjectTest {
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
    fun defaultCatalogIncludesIslamicReligiousStudies() {
        val irs = GhanaShsCatalog.defaultCatalog.single {
            it.id == "elec_irs"
        }

        assertEquals("Islamic Religious Studies (IRS)", irs.name)
        assertEquals("ELECTIVE", irs.category)
    }

    @Test
    fun customSubjectIsPersistedWithNormalizedNameAndCategory() {
        runBlocking {
            assertTrue(repository.addCustomSubject("  Robotics   and AI  ", "ELECTIVE"))

            val subject = database.subjectDao().getSubjectByNameDirect("Robotics and AI")

            assertNotEquals(null, subject)
            assertEquals("ELECTIVE", subject?.category)
            assertTrue(subject?.id?.startsWith("custom_") == true)
        }
    }

    @Test
    fun duplicateNamesAreRejectedIgnoringCase() {
        runBlocking {
            assertTrue(repository.addCustomSubject("Environmental Science", "ELECTIVE"))
            assertFalse(repository.addCustomSubject(" environmental science ", "CORE"))
        }
    }

    @Test
    fun invalidCustomSubjectInputsAreRejected() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.addCustomSubject("   ", "ELECTIVE") }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.addCustomSubject("Robotics", "OTHER") }
            }
        }
    }
}
