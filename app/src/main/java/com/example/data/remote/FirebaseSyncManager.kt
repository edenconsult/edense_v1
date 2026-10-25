package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.example.data.network.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class SyncState {
    data object Idle : SyncState()
    data object Checking : SyncState()
    data object Syncing : SyncState()
    data class Synced(val message: String = "Cloud synced") : SyncState()
    data class OfflineFallback(val reason: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class FirebaseSyncManager(private val context: Context) {

    private val auth get() = Firebase.auth
    val networkMonitor by lazy { NetworkMonitor(context) }
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadMutex = Mutex()
    private var pendingSyncJob: Job? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val firestore: FirebaseFirestore by lazy {
        val dbId = try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            ""
        }
        if (dbId.isBlank() || dbId == "(default)" || dbId.startsWith("ai-studio-android-")) {
            FirebaseFirestore.getInstance()
        } else {
            try {
                FirebaseFirestore.getInstance(dbId)
            } catch (e: Exception) {
                FirebaseFirestore.getInstance()
            }
        }
    }

    val currentUser get() = auth.currentUser

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    val isOnline: Boolean
        get() = networkMonitor.isOnline

    @Synchronized
    fun requestSync() {
        if (!isOnline || !isUserSignedIn) return
        pendingSyncJob?.cancel()
        pendingSyncJob = syncScope.launch {
            delay(AUTO_SYNC_DEBOUNCE_MS)
            val result = syncLocalDataToCloud()
            result.exceptionOrNull()?.let { error ->
                Log.e("FirebaseSync", "Automatic sync failed", error)
            }
        }
    }

    /**
     * Primary startup sync: Checks internet connectivity first.
     * If online and authenticated, performs bidirectional sync.
     * If offline or not authenticated, smoothly falls back to local offline mode.
     */
    suspend fun syncOnAppLaunch(): Result<Unit> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Checking

        // 1. Check internet connectivity priority
        if (!networkMonitor.isOnline) {
            Log.d("FirebaseSync", "No internet connection detected on launch. Operating in offline mode.")
            _syncState.value = SyncState.OfflineFallback("Working offline (no internet)")
            return@withContext Result.success(Unit)
        }

        val user = auth.currentUser
        if (user == null) {
            Log.d("FirebaseSync", "Internet available, but teacher is not signed in. Operating in offline mode.")
            _syncState.value = SyncState.OfflineFallback("Working offline (not signed in)")
            return@withContext Result.success(Unit)
        }

        // 2. Internet available and user signed in: execute cloud synchronization
        Log.d("FirebaseSync", "Internet connected and user ${user.email} authenticated. Syncing cloud data...")
        _syncState.value = SyncState.Syncing

        return@withContext performBidirectionalSync(user.uid)
    }

    /**
     * Pulls remote Firestore records down to Room if they exist,
     * then pushes local Room records to Firestore to guarantee cloud backup.
     */
    suspend fun performBidirectionalSync(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        pendingSyncJob?.cancel()
        uploadMutex.withLock {
            val db = EdenseDatabase.getDatabase(context)

            try {
                val user = auth.currentUser
                    ?: return@withLock Result.failure(
                        IllegalStateException("Teacher is not signed in to Google.")
                    )
                if (user.uid != uid) {
                    return@withLock Result.failure(
                        IllegalStateException("The signed-in account changed before sync started.")
                    )
                }
                _syncState.value = SyncState.Syncing
                val userDoc = firestore.collection("users").document(uid)

                restoreCloudDataToLocal(uid, userDoc, db)

                val pushResult = uploadLocalDataToCloud()
                if (pushResult.isFailure) {
                    val error = pushResult.exceptionOrNull()
                        ?: IllegalStateException("Cloud upload failed")
                    val isPermissionDenied =
                        error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                    _syncState.value = if (isPermissionDenied) {
                        SyncState.OfflineFallback("Firestore rules pending in console.")
                    } else {
                        SyncState.OfflineFallback("Sync deferred: ${error.localizedMessage}")
                    }
                    return@withLock Result.failure(error)
                }

                _syncState.value = SyncState.Synced("Cloud synchronized")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Bidirectional sync failed", e)
                _syncState.value = SyncState.OfflineFallback(
                    "Sync failed: ${e.localizedMessage ?: "unknown error"}"
                )
                Result.failure(e)
            }
        }
    }

    /**
     * Restores remote documents from Cloud Firestore into local Room database.
     */
    private suspend fun restoreCloudDataToLocal(
        uid: String,
        userDoc: com.google.firebase.firestore.DocumentReference,
        db: EdenseDatabase
    ) {
        // 1. Profile
        val profileSnap = userDoc.get().await()
        if (profileSnap.exists()) {
            val existing = db.teacherProfileDao().getProfileDirect()
            val remoteProfile = TeacherProfileEntity(
                id = "local_teacher",
                name = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: profileSnap.getString("name") ?: existing?.name ?: "",
                email = auth.currentUser?.email ?: profileSnap.getString("email") ?: existing?.email ?: "",
                schoolName = profileSnap.getString("schoolName") ?: existing?.schoolName ?: "",
                schoolStartTime = profileSnap.getString("schoolStartTime") ?: existing?.schoolStartTime ?: "07:00",
                schoolEndTime = profileSnap.getString("schoolEndTime") ?: existing?.schoolEndTime ?: "15:00",
                breakfastBreakStart = profileSnap.getString("breakfastBreakStart")
                    ?: existing?.breakfastBreakStart ?: "09:00",
                breakfastBreakEnd = profileSnap.getString("breakfastBreakEnd")
                    ?: existing?.breakfastBreakEnd ?: "09:30",
                lunchBreakStart = profileSnap.getString("lunchBreakStart")
                    ?: existing?.lunchBreakStart ?: "12:30",
                lunchBreakEnd = profileSnap.getString("lunchBreakEnd")
                    ?: existing?.lunchBreakEnd ?: "13:00",
                reminderLeadMinutes = (profileSnap.getLong("reminderLeadMinutes") ?: 5L).toInt(),
                isOnboardingCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            db.teacherProfileDao().insertOrUpdate(remoteProfile)
        }

        // 2. Calendar
        val calSnap = userDoc.collection("calendars").get().await()
        if (!calSnap.isEmpty) {
            val doc = calSnap.documents.first()
            val calendar = AcademicCalendarEntity(
                id = "active_calendar",
                academicYear = doc.getString("academicYear") ?: "2026/27",
                systemType = doc.getString("systemType") ?: "SEMESTER",
                activeTermName = doc.getString("activeTermName") ?: "Semester 1",
                termStartDate = doc.getString("termStartDate") ?: "",
                termEndDate = doc.getString("termEndDate") ?: "",
                pauseAlarmsDuringVacation = doc.getBoolean("pauseAlarmsDuringVacation") ?: true,
                isManualVacationMode = doc.getBoolean("isManualVacationMode") ?: false
            )
            db.academicCalendarDao().insertOrUpdate(calendar)
        }

        // 3. Terms
        val termsSnap = userDoc.collection("terms").get().await()
        if (!termsSnap.isEmpty) {
            val terms = termsSnap.documents.map { doc ->
                AcademicTermEntity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    startDate = doc.getString("startDate") ?: "",
                    endDate = doc.getString("endDate") ?: "",
                    orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt()
                )
            }
            db.academicTermDao().replaceAll(terms)
        }

        // 4. Subjects
        val subjectsSnap = userDoc.collection("subjects").get().await()
        for (doc in subjectsSnap.documents) {
            val existing = db.subjectDao().getSubjectByIdDirect(doc.id)
            val subject = SubjectEntity(
                id = doc.id,
                name = doc.getString("name") ?: existing?.name ?: doc.id,
                category = doc.getString("category") ?: existing?.category ?: "CORE",
                assignedClasses = doc.getString("assignedClasses") ?: "SHS 2B",
                defaultClassroom = doc.getString("defaultClassroom") ?: "Room 1",
                assignedClassrooms = doc.getString("assignedClassrooms") ?: "Room 1",
                targetPeriodsPerWeek = (doc.getLong("targetPeriodsPerWeek") ?: 4L).toInt(),
                isSelected = doc.getBoolean("isSelected") ?: existing?.isSelected ?: true,
                colorHex = doc.getString("colorHex") ?: existing?.colorHex ?: "#1E3A8A",
                isArchived = doc.getBoolean("isArchived") ?: existing?.isArchived ?: false,
                updatedAt = System.currentTimeMillis()
            )
            db.subjectDao().insertOrUpdate(subject)
        }

        // 5. Periods
        val periodsSnap = userDoc.collection("periods").get().await()
        for (doc in periodsSnap.documents) {
            val period = PeriodEntity(
                id = doc.id,
                subjectId = doc.getString("subjectId") ?: "",
                dayOfWeek = (doc.getLong("dayOfWeek") ?: 1L).toInt(),
                startTime = doc.getString("startTime") ?: "07:00",
                endTime = doc.getString("endTime") ?: "08:00",
                blockId = doc.getString("blockId") ?: doc.id,
                classroom = doc.getString("classroom") ?: "",
                className = doc.getString("className") ?: ""
            )
            db.periodDao().insertPeriod(period)
        }

        // 6. Sessions
        val sessionsSnap = userDoc.collection("sessions").get().await()
        for (doc in sessionsSnap.documents) {
            val session = TeachingSessionEntity(
                id = doc.id,
                subjectId = doc.getString("subjectId") ?: "",
                sessionDate = doc.getString("sessionDate") ?: "",
                lastTopicCovered = doc.getString("lastTopicCovered") ?: "",
                nextTopicPlanned = doc.getString("nextTopicPlanned") ?: "",
                status = doc.getString("status") ?: "COMPLETED",
                notes = doc.getString("notes") ?: "",
                className = doc.getString("className") ?: "",
                periodBlockId = doc.getString("periodBlockId") ?: "",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
            db.teachingSessionDao().insertSession(session)
        }

        // 7. Tasks
        val tasksSnap = userDoc.collection("tasks").get().await()
        for (doc in tasksSnap.documents) {
            val task = TaskEntity(
                id = doc.id,
                subjectId = doc.getString("subjectId") ?: "",
                className = doc.getString("className") ?: "",
                title = doc.getString("title") ?: "",
                type = doc.getString("type") ?: "ASSIGNMENT",
                dueDate = doc.getString("dueDate") ?: "",
                reminderTime = doc.getString("reminderTime") ?: "06:00",
                isCompleted = doc.getBoolean("isCompleted") ?: false,
                notes = doc.getString("notes") ?: "",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
            db.taskDao().insertTask(task)
        }
    }

    /**
     * Uploads all local Room records to user's private Firestore document and subcollections.
     */
    suspend fun syncLocalDataToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        uploadMutex.withLock { uploadLocalDataToCloud() }
    }

    private suspend fun uploadLocalDataToCloud(): Result<Unit> {
        val user = auth.currentUser
        if (user == null) {
            _syncState.value = SyncState.OfflineFallback("Not signed in")
            return Result.failure(IllegalStateException("Teacher is not signed in to Google."))
        }

        val uid = user.uid
        val db = EdenseDatabase.getDatabase(context)

        return try {
            _syncState.value = SyncState.Syncing
            val userDoc = firestore.collection("users").document(uid)

            // 1. Sync Profile
            val profile = db.teacherProfileDao().getProfileDirect()
            if (profile != null) {
                userDoc.set(
                    profile.toFirestoreFields(uid, user.email) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            // 2. Sync Academic Calendar
            val calendar = db.academicCalendarDao().getCalendarDirect()
            if (calendar != null) {
                userDoc.collection("calendars").document(calendar.id).set(
                    calendar.toFirestoreFields(uid) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            // 2a. Sync all academic terms
            val terms = db.academicTermDao().getTermsDirect()
            for (term in terms) {
                userDoc.collection("terms").document(term.id).set(
                    term.toFirestoreFields(uid) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            // 3. Sync Selected Subjects
            val subjects = db.subjectDao().getAllSubjectsDirect()
            for (sub in subjects) {
                userDoc.collection("subjects").document(sub.id).set(
                    sub.toFirestoreFields(uid) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            // 4. Sync Timetable Periods
            val periods = db.periodDao().getAllPeriodsDirect()
            for (period in periods) {
                userDoc.collection("periods").document(period.id).set(
                    period.toFirestoreFields(uid) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            // 5. Sync all teaching sessions, including class-specific carry-forward data
            val sessions = db.teachingSessionDao().getAllSessionsDirect()
            for (session in sessions) {
                userDoc.collection("sessions").document(session.id).set(
                    session.toFirestoreFields(uid) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            // 6. Sync all tasks, including completed and class-specific follow-up tasks
            val tasks = db.taskDao().getAllTasksDirect()
            for (task in tasks) {
                userDoc.collection("tasks").document(task.id).set(
                    task.toFirestoreFields(uid) +
                        ("updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            }

            Log.d("FirebaseSync", "Successfully synced Edense data to cloud for uid: $uid")
            _syncState.value = SyncState.Synced("Cloud synchronized")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Cloud sync failed", e)
            _syncState.value = SyncState.OfflineFallback("Saved offline: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    companion object {
        private const val AUTO_SYNC_DEBOUNCE_MS = 500L
    }
}
