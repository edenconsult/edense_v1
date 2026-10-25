package com.example.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.task.PeriodReminderTiming
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskReminderScheduler {
    fun schedule(
        context: Context,
        task: TaskEntity,
        linkedPeriod: PeriodEntity? = null,
        academicCalendar: AcademicCalendarEntity? = null,
        academicTerms: List<AcademicTermEntity> = emptyList()
    ) {
        cancel(context, task.id)
        if (task.isCompleted) return

        if (task.periodBlockId.isNotBlank() && linkedPeriod == null) {
            Log.e("TaskReminderScheduler", "Period ${task.periodBlockId} is missing for task ${task.id}.")
            return
        }
        val triggerAtMillis = if (linkedPeriod != null) {
            periodReminderTimeMillis(
                linkedPeriod,
                task.periodReminderTiming,
                System.currentTimeMillis(),
                academicCalendar,
                academicTerms
            )
        } else {
            reminderTimeMillis(task, System.currentTimeMillis(), academicCalendar, academicTerms)
        }
        if (triggerAtMillis == null) {
            Log.i("TaskReminderScheduler", "No future reminder time for task ${task.id}")
            return
        }

        val requestCode = "task_${task.id}".hashCode()
        val typeLabel = com.example.domain.task.TaskType.displayLabel(task.type)
        val triggerDate = formatDate(triggerAtMillis)
        val reminderDescription = linkedPeriod?.let {
            val timing = PeriodReminderTiming.entries.firstOrNull { timing ->
                timing.name == task.periodReminderTiming
            }?.label ?: task.periodReminderTiming
            "$timing for ${task.className.ifBlank { "class" }} on its next scheduled lesson."
        } ?: if (triggerDate > task.dueDate) {
            "$typeLabel was due on ${task.dueDate}. Reminder paused for vacation and resumes today."
        } else {
            "$typeLabel is due today (${task.dueDate}) at ${task.reminderTime}."
        }
        val intent = Intent(context, EdenseAlarmReceiver::class.java).apply {
            putExtra(EdenseAlarmReceiver.EXTRA_TITLE, "Academic Deadline: ${task.title}")
            putExtra(EdenseAlarmReceiver.EXTRA_MESSAGE, reminderDescription)
            putExtra(EdenseAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EdenseAlarmReceiver.EXTRA_IS_CLASS, false)
            putExtra(EdenseAlarmReceiver.EXTRA_TASK_ID, task.id)
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

    internal fun reminderTimeMillis(
        task: TaskEntity,
        nowMillis: Long,
        academicCalendar: AcademicCalendarEntity? = null,
        academicTerms: List<AcademicTermEntity> = emptyList()
    ): Long? {
        val dueDate = parseDate(task.dueDate) ?: return null
        val timeParts = task.reminderTime.split(":")
        if (timeParts.size != 2) return null
        val hour = timeParts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = timeParts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null

        val dueDateCalendar = Calendar.getInstance().apply {
            time = dueDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (academicCalendar?.isManualVacationMode == true) return null
        if (academicCalendar == null || !academicCalendar.pauseAlarmsDuringVacation) {
            return dueDateCalendar.timeInMillis.takeIf { it > nowMillis }
        }

        val dueDateTime = dueDateCalendar.timeInMillis
        val dueDateIsVacation = academicCalendar.isVacation(task.dueDate, academicTerms)
        if (!dueDateIsVacation && dueDateTime > nowMillis) {
            return dueDateTime
        }
        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!dueDateIsVacation &&
            !academicCalendar.isVacation(formatDate(today.timeInMillis), academicTerms)
        ) {
            return null
        }
        val firstSearchDate = if (dueDateIsVacation) dueDateCalendar else today

        repeat(MAX_VACATION_SEARCH_DAYS) { dayOffset ->
            val candidate = firstSearchDate.clone() as Calendar
            candidate.add(Calendar.DAY_OF_YEAR, dayOffset)
            candidate.set(Calendar.HOUR_OF_DAY, hour)
            candidate.set(Calendar.MINUTE, minute)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)
            val candidateDate = formatDate(candidate.timeInMillis)
            if (!academicCalendar.isVacation(candidateDate, academicTerms) &&
                candidate.timeInMillis > nowMillis
            ) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    internal fun periodReminderTimeMillis(
        period: PeriodEntity,
        timingCode: String,
        nowMillis: Long,
        academicCalendar: AcademicCalendarEntity? = null,
        academicTerms: List<AcademicTermEntity> = emptyList()
    ): Long? {
        if (academicCalendar?.isManualVacationMode == true) return null
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

        repeat(MAX_VACATION_SEARCH_DAYS) { dayOffset ->
            val occurrence = today.clone() as Calendar
            occurrence.add(Calendar.DAY_OF_YEAR, dayOffset)
            if (occurrence.get(Calendar.DAY_OF_WEEK) == dayOfWeek) {
                occurrence.set(Calendar.HOUR_OF_DAY, hour)
                occurrence.set(Calendar.MINUTE, minute)
                occurrence.set(Calendar.SECOND, 0)
                occurrence.set(Calendar.MILLISECOND, 0)
                occurrence.add(Calendar.MINUTE, offsetMinutes)
                val occurrenceDate = formatDate(occurrence.timeInMillis)
                val isVacation = academicCalendar?.pauseAlarmsDuringVacation == true &&
                    academicCalendar.isVacation(occurrenceDate, academicTerms)
                if (occurrence.timeInMillis > nowMillis && !isVacation) return occurrence.timeInMillis
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

    private fun formatDate(timeMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(timeMillis))

    private const val MAX_VACATION_SEARCH_DAYS = 3660
}
