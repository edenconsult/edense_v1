package com.example.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.schedule.SchedulingEngine
import java.util.Calendar

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
        leadMinutes: Int = 10
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startTotalMin = SchedulingEngine.parseTimeToMinutes(startTime)
        val triggerTotalMin = (startTotalMin - leadMinutes).coerceAtLeast(0)

        val triggerHour = triggerTotalMin / 60
        val triggerMinute = triggerTotalMin % 60

        // Map dayOfWeek (1=Mon..5=Fri) to Calendar.DAY_OF_WEEK (Calendar.MONDAY..Calendar.FRIDAY)
        val targetCalDay = when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            else -> Calendar.MONDAY
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, targetCalDay)
            set(Calendar.HOUR_OF_DAY, triggerHour)
            set(Calendar.MINUTE, triggerMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, EdenseAlarmReceiver::class.java).apply {
            putExtra(EdenseAlarmReceiver.EXTRA_TITLE, "Upcoming Class in $leadMinutes mins")
            putExtra(
                EdenseAlarmReceiver.EXTRA_MESSAGE,
                "$subjectName ($className) starts at $startTime in $classroom."
            )
            putExtra(EdenseAlarmReceiver.EXTRA_NOTIFICATION_ID, blockId.hashCode())
            putExtra(EdenseAlarmReceiver.EXTRA_IS_CLASS, true)
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
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Handled gracefully if exact alarm permission is restricted
        }
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
