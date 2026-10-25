package com.example.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "pending_sync_deletions",
    primaryKeys = ["collectionPath", "documentId"]
)
data class PendingDeletionEntity(
    val collectionPath: String,
    val documentId: String
)
