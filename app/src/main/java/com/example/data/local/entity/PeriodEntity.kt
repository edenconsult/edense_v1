package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday
    val startTime: String, // HH:mm format e.g. "07:00"
    val endTime: String,   // HH:mm format e.g. "08:00"
    val blockId: String,   // Shared across consecutive periods in the same class block
    val classroom: String = "",
    val className: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
