package com.example.data.remote

import com.example.data.local.dao.PendingDeletionDao
import com.example.data.local.entity.PendingDeletionEntity

class PendingDeletionSync(private val pendingDeletionDao: PendingDeletionDao) {
    suspend fun sync(deleteRemoteDocument: suspend (PendingDeletionEntity) -> Unit) {
        pendingDeletionDao.getPendingDeletions().forEach { deletion ->
            deleteRemoteDocument(deletion)
            pendingDeletionDao.remove(deletion.collectionPath, deletion.documentId)
        }
    }
}
