package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teaching_sessions")
data class TeachingSessionEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val sessionDate: String, // YYYY-MM-DD
    val lastTopicCovered: String,
    val nextTopicPlanned: String,
    val status: String = "COMPLETED", // "COMPLETED", "PARTIAL", "SKIPPED"
    val notes: String = "",
    val periodBlockId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
