package com.example.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.domain.schedule.SchedulingEngine
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlarmScheduler {

    /**
     * Schedules a local notification 10 minutes (or leadMinutes) before a class period.
     */
    fun scheduleClassReminder(
        context: Context,
        blockId: String,
        subjectName: String,
        className: String,
        classroom: String,
        dayOfWeek: Int, // 1 = Mon..5 = Fri
        startTime: String, // "07:00"
        leadMinutes: Int = 10,
        academicCalendar: AcademicCalendarEntity? = null,
        academicTerms: List<AcademicTermEntity> = emptyList()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = nextReminderTimeMillis(
            nowMillis = System.currentTimeMillis(),
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            leadMinutes = leadMinutes,
            academicCalendar = academicCalendar,
            academicTerms = academicTerms
        )
        if (triggerAtMillis == null) {
            Log.i("AlarmScheduler", "No active-term reminder date found for block $blockId")
            return
        }

        val intent = Intent(context, EdenseAlarmReceiver::class.java).apply {
            putExtra(EdenseAlarmReceiver.EXTRA_TITLE, "Upcoming Class in $leadMinutes mins")
            putExtra(EdenseAlarmReceiver.EXTRA_SUBJECT_NAME, subjectName)
            putExtra(EdenseAlarmReceiver.EXTRA_CLASS_NAME, className)
            putExtra(EdenseAlarmReceiver.EXTRA_CLASSROOM, classroom)
            putExtra(
                EdenseAlarmReceiver.EXTRA_MESSAGE,
                "$subjectName ($className) starts at $startTime in $classroom."
            )
            putExtra(EdenseAlarmReceiver.EXTRA_NOTIFICATION_ID, blockId.hashCode())
            putExtra(EdenseAlarmReceiver.EXTRA_IS_CLASS, true)
            putExtra(EdenseAlarmReceiver.EXTRA_BLOCK_ID, blockId)
            putExtra(EdenseAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(EdenseAlarmReceiver.EXTRA_START_TIME, startTime)
            putExtra(EdenseAlarmReceiver.EXTRA_LEAD_MINUTES, leadMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            blockId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Unable to schedule class reminder for block $blockId", e)
        }
    }

    internal fun nextReminderTimeMillis(
        nowMillis: Long,
        dayOfWeek: Int,
        startTime: String,
        leadMinutes: Int,
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
        val triggerTotalMin = (SchedulingEngine.parseTimeToMinutes(startTime) - leadMinutes)
            .coerceAtLeast(0)
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val date = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

        repeat(3660) {
            val candidate = date.clone() as Calendar
            candidate.add(Calendar.DAY_OF_YEAR, it)
            if (candidate.get(Calendar.DAY_OF_WEEK) == targetDay) {
                candidate.set(Calendar.HOUR_OF_DAY, triggerTotalMin / 60)
                candidate.set(Calendar.MINUTE, triggerTotalMin % 60)
                candidate.set(Calendar.SECOND, 0)
                candidate.set(Calendar.MILLISECOND, 0)
                val dateString = dateFormat.format(Date(candidate.timeInMillis))
                val isPaused = academicCalendar != null &&
                    (academicCalendar.isManualVacationMode ||
                        (academicCalendar.pauseAlarmsDuringVacation &&
                            academicCalendar.isVacation(dateString, academicTerms)))
                if (candidate.timeInMillis > now.timeInMillis && !isPaused) {
                    return candidate.timeInMillis
                }
            }
        }
        return null
    }

    /**
     * Cancels an existing alarm by stable ID.
     */
    fun cancelAlarm(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EdenseAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
