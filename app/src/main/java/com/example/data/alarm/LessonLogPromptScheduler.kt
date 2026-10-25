package com.example.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.domain.schedule.SchedulingEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LessonLogPromptScheduler {
    fun schedule(
        context: Context,
        blockPeriods: List<PeriodEntity>,
        subjectName: String,
        academicCalendar: AcademicCalendarEntity?,
        academicTerms: List<AcademicTermEntity>
    ) {
        val lastPeriod = blockPeriods.maxByOrNull { SchedulingEngine.parseTimeToMinutes(it.endTime) }
            ?: return
        val triggerAtMillis = nextLessonEndTimeMillis(
            nowMillis = System.currentTimeMillis(),
            dayOfWeek = lastPeriod.dayOfWeek,
            endTime = lastPeriod.endTime,
            academicCalendar = academicCalendar,
            academicTerms = academicTerms
        )
        if (triggerAtMillis == null) {
            Log.i("LessonLogPrompt", "No active lesson date found for block ${lastPeriod.blockId}.")
            cancel(context, lastPeriod.blockId)
            return
        }

        val requestCode = "lesson_log_${lastPeriod.blockId}".hashCode()
        val subjectId = lastPeriod.subjectId
        val className = lastPeriod.className.ifBlank { "Class" }
        val scheduledDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            .format(Date(triggerAtMillis))
        val intent = Intent(context, EdenseAlarmReceiver::class.java).apply {
            putExtra(EdenseAlarmReceiver.EXTRA_TITLE, "Log your lesson")
            putExtra(
                EdenseAlarmReceiver.EXTRA_MESSAGE,
                "Your $subjectName lesson with $className has ended. Add your lesson notes."
            )
            putExtra(EdenseAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EdenseAlarmReceiver.EXTRA_IS_CLASS, true)
            putExtra(EdenseAlarmReceiver.EXTRA_PROMPT_LOG, true)
            putExtra(EdenseAlarmReceiver.EXTRA_BLOCK_ID, lastPeriod.blockId)
            putExtra(EdenseAlarmReceiver.EXTRA_SUBJECT_ID, subjectId)
            putExtra(EdenseAlarmReceiver.EXTRA_CLASS_NAME, className)
            putExtra(EdenseAlarmReceiver.EXTRA_DAY_OF_WEEK, lastPeriod.dayOfWeek)
            putExtra(EdenseAlarmReceiver.EXTRA_END_TIME, lastPeriod.endTime)
            putExtra(EdenseAlarmReceiver.EXTRA_SUBJECT_NAME, subjectName)
            putExtra(EdenseAlarmReceiver.EXTRA_SESSION_DATE, scheduledDate)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        AlarmDelivery.schedule(
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
            triggerAtMillis,
            pendingIntent,
            "LessonLogPrompt"
        )
    }

    fun cancel(context: Context, blockId: String) {
        val requestCode = "lesson_log_$blockId".hashCode()
        val intent = Intent(context, EdenseAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }

    internal fun nextLessonEndTimeMillis(
        nowMillis: Long,
        dayOfWeek: Int,
        endTime: String,
        academicCalendar: AcademicCalendarEntity?,
        academicTerms: List<AcademicTermEntity>
    ): Long? {
        val targetDay = when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            else -> return null
        }
        val parts = endTime.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

        repeat(3660) { dayOffset ->
            val occurrence = today.clone() as Calendar
            occurrence.add(Calendar.DAY_OF_YEAR, dayOffset)
            if (occurrence.get(Calendar.DAY_OF_WEEK) == targetDay) {
                occurrence.set(Calendar.HOUR_OF_DAY, hour)
                occurrence.set(Calendar.MINUTE, minute)
                occurrence.set(Calendar.SECOND, 0)
                occurrence.set(Calendar.MILLISECOND, 0)
                val date = dateFormat.format(Date(occurrence.timeInMillis))
                val isPaused = academicCalendar != null &&
                    (academicCalendar.isManualVacationMode ||
                        (academicCalendar.pauseAlarmsDuringVacation &&
                            academicCalendar.isVacation(date, academicTerms)))
                if (occurrence.timeInMillis > nowMillis && !isPaused) {
                    return occurrence.timeInMillis
                }
            }
        }
        return null
    }
}
