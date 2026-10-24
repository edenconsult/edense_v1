package com.example.domain.task

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class TaskUrgency(val label: String) {
    OVERDUE("Overdue"),
    DUE_TODAY("Due today"),
    DUE_SOON("Due soon"),
    UPCOMING("Upcoming"),
    UNKNOWN("Date unavailable")
}

fun taskUrgency(dueDate: String, today: String): TaskUrgency {
    val dueDay = parseIsoDate(dueDate) ?: return TaskUrgency.UNKNOWN
    val currentDay = parseIsoDate(today) ?: return TaskUrgency.UNKNOWN
    val daysRemaining = ((dueDay.time - currentDay.time) / MILLIS_PER_DAY).toInt()
    return when {
        daysRemaining < 0 -> TaskUrgency.OVERDUE
        daysRemaining == 0 -> TaskUrgency.DUE_TODAY
        daysRemaining <= 2 -> TaskUrgency.DUE_SOON
        else -> TaskUrgency.UPCOMING
    }
}

private fun parseIsoDate(value: String): java.util.Date? {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val position = ParsePosition(0)
    val parsed = format.parse(value, position) ?: return null
    return parsed.takeIf { position.index == value.length && format.format(it) == value }
}

private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
