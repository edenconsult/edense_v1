package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academic_calendar")
data class AcademicCalendarEntity(
    @PrimaryKey val id: String = "active_calendar",
    val academicYear: String = "2026/2027",
    val systemType: String = "SEMESTER", // "SEMESTER" or "TERM"
    val activeTermName: String = "Semester 1",
    val termStartDate: String = "2026-09-01",
    val termEndDate: String = "2026-12-18",
    val pauseAlarmsDuringVacation: Boolean = true,
    val isManualVacationMode: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Determines whether the given date (formatted YYYY-MM-DD) falls outside active school dates
     * or if manual vacation mode is active.
     */
    fun isVacation(dateStr: String): Boolean {
        if (isManualVacationMode) return true
        if (termStartDate.isNotBlank() && dateStr < termStartDate) return true
        if (termEndDate.isNotBlank() && dateStr > termEndDate) return true
        return false
    }
}
