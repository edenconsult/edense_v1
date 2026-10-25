package com.example.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.task.PeriodReminderTiming
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TaskReminderScheduler {
    fun schedule(context: Context, task: TaskEntity, linkedPeriod: PeriodEntity? = null) {
        cancel(context, task.id)
        if (task.isCompleted) return

        if (task.periodBlockId.isNotBlank() && linkedPeriod == null) {
            Log.e("TaskReminderScheduler", "Period ${task.periodBlockId} is missing for task ${task.id}.")
            return
        }
        val triggerAtMillis = if (linkedPeriod != null) {
            periodReminderTimeMillis(linkedPeriod, task.periodReminderTiming, System.currentTimeMillis())
        } else {
            reminderTimeMillis(task, System.currentTimeMillis())
        }
        if (triggerAtMillis == null) {
            Log.i("TaskReminderScheduler", "No future reminder time for task ${task.id}")
            return
        }

        val requestCode = "task_${task.id}".hashCode()
        val typeLabel = com.example.domain.task.TaskType.displayLabel(task.type)
        val reminderDescription = linkedPeriod?.let {
            val timing = PeriodReminderTiming.entries.firstOrNull { timing ->
                timing.name == task.periodReminderTiming
            }?.label ?: task.periodReminderTiming
            "$timing for ${task.className.ifBlank { "class" }} on its next scheduled lesson."
        } ?: "$typeLabel is due today (${task.dueDate}) at ${task.reminderTime}."
        val intent = Intent(context, EdenseAlarmReceiver::class.java).apply {
            putExtra(EdenseAlarmReceiver.EXTRA_TITLE, "Academic Deadline: ${task.title}")
            putExtra(EdenseAlarmReceiver.EXTRA_MESSAGE, reminderDescription)
            putExtra(EdenseAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EdenseAlarmReceiver.EXTRA_IS_CLASS, false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            AlarmDelivery.schedule(
                alarmManager,
                triggerAtMillis,
                pendingIntent,
                "TaskReminderScheduler"
            )
        } catch (e: SecurityException) {
            Log.e("TaskReminderScheduler", "Unable to schedule reminder for task ${task.id}", e)
        }
    }

    fun cancel(context: Context, taskId: String) {
        val intent = Intent(context, EdenseAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "task_$taskId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    internal fun reminderTimeMillis(task: TaskEntity, nowMillis: Long): Long? {
        val dueDate = parseDate(task.dueDate) ?: return null
        val timeParts = task.reminderTime.split(":")
        if (timeParts.size != 2) return null
        val hour = timeParts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = timeParts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null

        val trigger = Calendar.getInstance().apply {
            time = dueDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return trigger.takeIf { it > nowMillis }
    }

    internal fun periodReminderTimeMillis(
        period: PeriodEntity,
        timingCode: String,
        nowMillis: Long
    ): Long? {
        val timing = PeriodReminderTiming.entries.firstOrNull { it.name == timingCode } ?: run {
            Log.e("TaskReminderScheduler", "Unknown period reminder timing: $timingCode")
            return null
        }
        val dayOfWeek = when (period.dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            else -> return null
        }
        val boundaryTime = when (timing) {
            PeriodReminderTiming.BEFORE_START -> period.startTime
            PeriodReminderTiming.BEFORE_END,
            PeriodReminderTiming.AFTER_END -> period.endTime
        }
        val timeParts = boundaryTime.split(":")
        if (timeParts.size != 2) return null
        val hour = timeParts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = timeParts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val offsetMinutes = if (timing == PeriodReminderTiming.AFTER_END) 0 else -5
        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(8) { dayOffset ->
            val occurrence = today.clone() as Calendar
            occurrence.add(Calendar.DAY_OF_YEAR, dayOffset)
            if (occurrence.get(Calendar.DAY_OF_WEEK) == dayOfWeek) {
                occurrence.set(Calendar.HOUR_OF_DAY, hour)
                occurrence.set(Calendar.MINUTE, minute)
                occurrence.set(Calendar.SECOND, 0)
                occurrence.set(Calendar.MILLISECOND, 0)
                occurrence.add(Calendar.MINUTE, offsetMinutes)
                if (occurrence.timeInMillis > nowMillis) return occurrence.timeInMillis
            }
        }
        return null
    }

    private fun parseDate(value: String): java.util.Date? {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }
        val position = ParsePosition(0)
        val parsed = format.parse(value, position) ?: return null
        return parsed.takeIf { position.index == value.length && format.format(it) == value }
    }
}
