package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

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
    fun isVacation(dateStr: String, terms: List<AcademicTermEntity> = emptyList()): Boolean {
        if (isManualVacationMode) return true
        val validTerms = terms.filter { term ->
            term.startDate.isNotBlank() && term.endDate.isNotBlank() && term.startDate <= term.endDate
        }
        if (validTerms.isEmpty()) {
            return (termStartDate.isNotBlank() && dateStr < termStartDate) ||
                (termEndDate.isNotBlank() && dateStr > termEndDate)
        }
        return validTerms.none { dateStr >= it.startDate && dateStr <= it.endDate }
    }

    fun nextTermStartDate(dateStr: String, terms: List<AcademicTermEntity> = emptyList()): String {
        val validTerms = terms.filter { it.startDate.isNotBlank() && it.startDate > dateStr }
        return validTerms.minByOrNull { it.startDate }?.startDate.orEmpty()
    }

    fun hasValidTerms(terms: List<AcademicTermEntity>): Boolean {
        if (terms.isEmpty()) return termStartDate.isNotBlank() && termEndDate.isNotBlank()
        val sorted = terms.sortedBy { it.startDate }
        return sorted.all { term ->
            val start = parseIsoDate(term.startDate)
            val end = parseIsoDate(term.endDate)
            start != null && end != null && !end.before(start)
        } && sorted.zipWithNext().all { (current, next) -> current.endDate < next.startDate }
    }

    private fun parseIsoDate(value: String): java.util.Date? {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }
        val position = ParsePosition(0)
        val parsed = format.parse(value, position) ?: return null
        return parsed.takeIf { position.index == value.length && format.format(it) == value }
    }
}
