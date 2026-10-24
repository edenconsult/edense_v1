package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_profile")
data class TeacherProfileEntity(
    @PrimaryKey val id: String = "local_teacher",
    val name: String = "Teacher",
    val email: String = "",
    val schoolName: String = "Senior High School",
    val schoolStartTime: String = "07:00",
    val schoolEndTime: String = "15:00",
    val isOnboardingCompleted: Boolean = false,
    val reminderLeadMinutes: Int = 10,
    val updatedAt: Long = System.currentTimeMillis()
)
