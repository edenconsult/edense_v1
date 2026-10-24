package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val subjectId: String = "",
    val title: String,
    val type: String = "ASSIGNMENT", // "ASSIGNMENT", "TEST", "EXAM", "PROJECT"
    val dueDate: String, // YYYY-MM-DD
    val reminderTime: String = "06:00", // HH:mm
    val isCompleted: Boolean = false,
    val notes: String = "",
    val className: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
