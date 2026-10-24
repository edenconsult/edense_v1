package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String = "CORE", // "CORE" or "ELECTIVE"
    val isSelected: Boolean = false, // Max 2 subjects selected per teacher in a given academic year
    val assignedClasses: String = "", // e.g. "SHS 2B" or "SHS 2B, SHS 3A"
    val defaultClassroom: String = "", // e.g. "Room 14"
    val assignedClassrooms: String = "", // Comma-separated rooms paired by index with assignedClasses
    val targetPeriodsPerWeek: Int = 0, // Weekly target quota set during onboarding
    val colorHex: String = "#1E6F5C",
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
