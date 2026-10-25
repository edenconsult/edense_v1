package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_profile")
data class TeacherProfileEntity(
    @PrimaryKey val id: String = "local_teacher",
    val name: String = "Teacher",
    val email: String = "",
    val schoolName: String = "Senior High School",
    val schoolStartTime: String = "07:00",
    val schoolEndTime: String = "15:00",
    @ColumnInfo(defaultValue = "'09:00'") val breakfastBreakStart: String = "09:00",
    @ColumnInfo(defaultValue = "'09:30'") val breakfastBreakEnd: String = "09:30",
    @ColumnInfo(defaultValue = "'12:30'") val lunchBreakStart: String = "12:30",
    @ColumnInfo(defaultValue = "'13:00'") val lunchBreakEnd: String = "13:00",
    val isOnboardingCompleted: Boolean = false,
    val reminderLeadMinutes: Int = 10,
    val updatedAt: Long = System.currentTimeMillis()
)
