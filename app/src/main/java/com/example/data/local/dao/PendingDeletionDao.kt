package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PendingDeletionEntity

@Dao
interface PendingDeletionDao {
    @Query("SELECT * FROM pending_sync_deletions ORDER BY collectionPath, documentId")
    suspend fun getPendingDeletions(): List<PendingDeletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingDeletionEntity)

    @Query(
        "DELETE FROM pending_sync_deletions " +
            "WHERE collectionPath = :collectionPath AND documentId = :documentId"
    )
    suspend fun remove(collectionPath: String, documentId: String)
}
