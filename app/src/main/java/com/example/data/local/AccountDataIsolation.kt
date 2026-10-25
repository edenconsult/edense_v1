package com.example.data.local

import android.content.Context
import androidx.room.withTransaction
import com.example.data.catalog.GhanaShsCatalog
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PendingDeletionEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.security.MessageDigest

internal data class AccountLocalSnapshot(
    val profile: TeacherProfileEntity?,
    val calendar: AcademicCalendarEntity?,
    val terms: List<AcademicTermEntity>,
    val subjects: List<SubjectEntity>,
    val periods: List<PeriodEntity>,
    val sessions: List<TeachingSessionEntity>,
    val tasks: List<TaskEntity>,
    val pendingDeletions: List<PendingDeletionEntity>
)

internal class AccountDataIsolation(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val snapshotAdapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(AccountLocalSnapshot::class.java)
    private val accountSnapshotDirectory = File(appContext.filesDir, SNAPSHOT_DIRECTORY)

    suspend fun switchToAccount(uid: String, database: EdenseDatabase): Boolean {
        require(uid.isNotBlank()) { "Cannot bind local data to an empty account ID." }
        val currentOwner = preferences.getString(OWNER_UID_KEY, null)
        if (currentOwner == uid) return false

        val existingSnapshot = readSnapshot(database)
        if (currentOwner == null) {
            saveLegacySnapshot(existingSnapshot)
        } else {
            saveSnapshot(currentOwner, existingSnapshot)
        }
        val targetSnapshot = readSnapshot(uid)

        database.withTransaction {
            database.pendingDeletionDao().deleteAll()
            database.taskDao().deleteAll()
            database.teachingSessionDao().deleteAll()
            database.periodDao().deleteAll()
            database.academicTermDao().deleteAll()
            database.academicCalendarDao().deleteAll()
            database.teacherProfileDao().deleteAll()
            database.subjectDao().deleteAll()
            database.subjectDao().insertCatalog(GhanaShsCatalog.defaultCatalog)
            targetSnapshot?.let { restoreSnapshot(it, database) }
        }

        check(
            preferences.edit().putString(OWNER_UID_KEY, uid).commit()
        ) { "Could not save the local account ownership state." }
        return true
    }

    private suspend fun readSnapshot(database: EdenseDatabase): AccountLocalSnapshot =
        database.withTransaction {
            AccountLocalSnapshot(
                profile = database.teacherProfileDao().getProfileDirect(),
                calendar = database.academicCalendarDao().getCalendarDirect(),
                terms = database.academicTermDao().getTermsDirect(),
                subjects = database.subjectDao().getAllSubjectsDirect(),
                periods = database.periodDao().getAllPeriodsDirect(),
                sessions = database.teachingSessionDao().getAllSessionsDirect(),
                tasks = database.taskDao().getAllTasksDirect(),
                pendingDeletions = database.pendingDeletionDao().getPendingDeletions()
            )
        }

    private fun saveLegacySnapshot(snapshot: AccountLocalSnapshot) {
        if (File(accountSnapshotDirectory, LEGACY_SNAPSHOT_NAME).exists()) return
        writeSnapshot(File(accountSnapshotDirectory, LEGACY_SNAPSHOT_NAME), snapshot)
    }

    private fun saveSnapshot(uid: String, snapshot: AccountLocalSnapshot) {
        writeSnapshot(snapshotFile(uid), snapshot)
    }

    private fun readSnapshot(uid: String): AccountLocalSnapshot? {
        val file = snapshotFile(uid)
        if (!file.exists()) return null
        return snapshotAdapter.fromJson(file.readText())
            ?: throw IllegalStateException("The saved local data for this account could not be read.")
    }

    private fun writeSnapshot(file: File, snapshot: AccountLocalSnapshot) {
        accountSnapshotDirectory.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(snapshotAdapter.toJson(snapshot))
        if (!temporaryFile.renameTo(file)) {
            temporaryFile.copyTo(file, overwrite = true)
            check(temporaryFile.delete()) { "Could not finish saving the local account snapshot." }
        }
    }

    private fun snapshotFile(uid: String): File {
        val accountHash = MessageDigest.getInstance("SHA-256")
            .digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(accountSnapshotDirectory, "$accountHash.json")
    }

    private suspend fun restoreSnapshot(snapshot: AccountLocalSnapshot, database: EdenseDatabase) {
        snapshot.profile?.let { database.teacherProfileDao().insertOrUpdate(it) }
        snapshot.calendar?.let { database.academicCalendarDao().insertOrUpdate(it) }
        database.academicTermDao().replaceAll(snapshot.terms)
        snapshot.subjects.forEach { database.subjectDao().insertOrUpdate(it) }
        database.periodDao().insertPeriods(snapshot.periods)
        snapshot.sessions.forEach { database.teachingSessionDao().insertSession(it) }
        snapshot.tasks.forEach { database.taskDao().insertTask(it) }
        snapshot.pendingDeletions.forEach { database.pendingDeletionDao().insert(it) }
    }

    private companion object {
        const val PREFERENCES_NAME = "edense_local_account"
        const val OWNER_UID_KEY = "owner_uid"
        const val SNAPSHOT_DIRECTORY = "account_data"
        const val LEGACY_SNAPSHOT_NAME = "legacy_unassigned.json"
    }
}
