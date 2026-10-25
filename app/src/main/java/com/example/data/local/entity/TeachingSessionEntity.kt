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
    val className: String = "",
    val periodBlockId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

fun TeachingSessionEntity.displayStatus(): String = formatSessionStatus(status)

fun formatSessionStatus(status: String): String = when (status.uppercase()) {
    "PARTIAL" -> "Unfinished"
    "SKIPPED" -> "Skipped"
    "COMPLETED" -> "Completed"
    else -> status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
