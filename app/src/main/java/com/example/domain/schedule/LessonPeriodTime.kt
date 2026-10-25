package com.example.domain.schedule

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.data.local.entity.PeriodEntity

fun periodsForSessionDate(
    periods: List<PeriodEntity>,
    subjectId: String,
    sessionDate: String
): List<PeriodEntity> {
    val dayOfWeek = try {
        SchedulingEngine.dayOfWeekFromDate(sessionDate)
    } catch (_: IllegalArgumentException) {
        return emptyList()
    }
    return periods.filter { it.subjectId == subjectId && it.dayOfWeek == dayOfWeek }
}

fun lessonEndTimeMillis(sessionDate: String, endTime: String): Long? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
    }
    val datePosition = ParsePosition(0)
    val date = dateFormat.parse(sessionDate, datePosition)
        ?.takeIf { datePosition.index == sessionDate.length && dateFormat.format(it) == sessionDate }
        ?: return null
    val timeParts = endTime.split(":")
    if (timeParts.size != 2) return null
    val hour = timeParts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val minute = timeParts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun isLessonPeriodPast(sessionDate: String, endTime: String, nowMillis: Long): Boolean =
    lessonEndTimeMillis(sessionDate, endTime)?.let { it < nowMillis } ?: false

fun sessionPeriodMatchesDate(sessionDate: String, periodDayOfWeek: Int): Boolean {
    return sessionDateDayOfWeek(sessionDate) == periodDayOfWeek
}

fun sessionDateDayOfWeek(sessionDate: String): Int {
    return try {
        SchedulingEngine.dayOfWeekFromDate(sessionDate)
    } catch (_: IllegalArgumentException) {
        0
    }
}
