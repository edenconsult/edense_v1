package com.example.data.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class FirebaseSyncWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val syncManager = FirebaseSyncManager(applicationContext)
        val expectedUid = inputData.getString(FirebaseSyncWorkScheduler.ACCOUNT_UID_INPUT_KEY)
            ?: return Result.success()
        if (syncManager.currentUser?.uid != expectedUid) return Result.success()

        val result = syncManager.syncLocalDataToCloud(expectedUid)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}

object FirebaseSyncWorkScheduler {
    const val UNIQUE_WORK_NAME = "firebase_local_data_sync"
    const val ACCOUNT_UID_INPUT_KEY = "account_uid"

    fun createRequest(uid: String): OneTimeWorkRequest {
        require(uid.isNotBlank()) { "Cannot schedule a sync without an account ID." }
        return OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
            .setInputData(Data.Builder().putString(ACCOUNT_UID_INPUT_KEY, uid).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
    }

    fun enqueue(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueWorkName(uid),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            createRequest(uid)
        )
    }

    private fun uniqueWorkName(uid: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "$UNIQUE_WORK_NAME:$digest"
    }
}
