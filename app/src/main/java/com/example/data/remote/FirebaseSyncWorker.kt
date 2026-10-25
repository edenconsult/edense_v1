package com.example.data.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
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
        if (!syncManager.isUserSignedIn) return Result.success()

        val result = syncManager.syncLocalDataToCloud()
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}

object FirebaseSyncWorkScheduler {
    const val UNIQUE_WORK_NAME = "firebase_local_data_sync"

    fun createRequest(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

    fun enqueue(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            createRequest()
        )
    }
}
