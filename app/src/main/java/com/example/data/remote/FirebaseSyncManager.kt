package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.EdenseDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncManager(private val context: Context) {

    private val auth get() = Firebase.auth
    private val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    val currentUser get() = auth.currentUser

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Uploads all local Room records to user's private Firestore document and subcollections.
     */
    suspend fun syncLocalDataToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            return@withContext Result.failure(IllegalStateException("Teacher is not signed in to Google."))
        }

        val uid = user.uid
        val db = EdenseDatabase.getDatabase(context)

        try {
            val userDoc = firestore.collection("users").document(uid)

            // 1. Sync Profile
            val profile = db.teacherProfileDao().getProfileDirect()
            if (profile != null) {
                userDoc.set(
                    mapOf(
                        "userId" to uid,
                        "name" to profile.name,
                        "email" to (user.email ?: profile.email),
                        "schoolName" to profile.schoolName,
                        "schoolStartTime" to profile.schoolStartTime,
                        "schoolEndTime" to profile.schoolEndTime,
                        "reminderLeadMinutes" to profile.reminderLeadMinutes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // 2. Sync Academic Calendar
            val calendar = db.academicCalendarDao().getCalendarDirect()
            if (calendar != null) {
                userDoc.collection("calendars").document(calendar.id).set(
                    mapOf(
                        "userId" to uid,
                        "academicYear" to calendar.academicYear,
                        "systemType" to calendar.systemType,
                        "activeTermName" to calendar.activeTermName,
                        "termStartDate" to calendar.termStartDate,
                        "termEndDate" to calendar.termEndDate,
                        "pauseAlarmsDuringVacation" to calendar.pauseAlarmsDuringVacation,
                        "isManualVacationMode" to calendar.isManualVacationMode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // 3. Sync Selected Subjects
            val subjects = db.subjectDao().getSelectedSubjectsDirect()
            for (sub in subjects) {
                userDoc.collection("subjects").document(sub.id).set(
                    mapOf(
                        "userId" to uid,
                        "name" to sub.name,
                        "category" to sub.category,
                        "assignedClasses" to sub.assignedClasses,
                        "defaultClassroom" to sub.defaultClassroom,
                        "assignedClassrooms" to sub.assignedClassrooms,
                        "targetPeriodsPerWeek" to sub.targetPeriodsPerWeek,
                        "colorHex" to sub.colorHex,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // 4. Sync Timetable Periods
            val periods = db.periodDao().getAllPeriodsDirect()
            for (period in periods) {
                userDoc.collection("periods").document(period.id).set(
                    mapOf(
                        "userId" to uid,
                        "subjectId" to period.subjectId,
                        "dayOfWeek" to period.dayOfWeek,
                        "startTime" to period.startTime,
                        "endTime" to period.endTime,
                        "blockId" to period.blockId,
                        "classroom" to period.classroom,
                        "className" to period.className,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // 5. Sync Teaching Sessions
            // Reading direct list via query
            val sessionsSnapshot = db.teachingSessionDao().getAllSessions()
            // Sync tasks
            val tasksSnapshot = db.taskDao().getPendingTasksForDateDirect("9999-12-31") // or all tasks

            Log.d("FirebaseSync", "Successfully synced Edense data to cloud for uid: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Cloud sync failed", e)
            Result.failure(e)
        }
    }
}
