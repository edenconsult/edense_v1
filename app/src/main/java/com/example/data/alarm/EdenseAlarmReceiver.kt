package com.example.data.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.EdenseDatabase
import com.example.domain.schedule.periodBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EdenseAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Class Starting Soon"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have an upcoming class."
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
        val isClassReminder = intent.getBooleanExtra(EXTRA_IS_CLASS, true)

        // Asynchronously check vacation settings before delivering
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = EdenseDatabase.getDatabase(context)
                val calendar = db.academicCalendarDao().getCalendarDirect()
                val terms = db.academicTermDao().getTermsDirect()
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val isLogPrompt = intent.getBooleanExtra(EXTRA_PROMPT_LOG, false)

                if (isLogPrompt) {
                    val blockId = intent.getStringExtra(EXTRA_BLOCK_ID)
                    val periods = db.periodDao().getAllPeriodsDirect()
                        .filter { it.blockId == blockId }
                    if (periods.isNotEmpty()) {
                        LessonLogPromptScheduler.schedule(
                            context,
                            periods,
                            intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Class",
                            calendar,
                            terms
                        )
                    }
                    if (calendar != null && (calendar.isManualVacationMode ||
                            (calendar.pauseAlarmsDuringVacation &&
                                calendar.isVacation(todayDate, terms)))
                    ) {
                        return@launch
                    }
                    val prompt = LessonLogPrompt(
                        blockId = blockId ?: return@launch,
                        subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: "",
                        className = intent.getStringExtra(EXTRA_CLASS_NAME) ?: "",
                        sessionDate = intent.getStringExtra(EXTRA_SESSION_DATE) ?: todayDate
                    )
                    if (MainActivity.isInForeground) {
                        LessonLogPromptController.request(prompt)
                    } else {
                        showNotification(
                            context,
                            title,
                            message,
                            notificationId,
                            isClassReminder = true,
                            lessonPrompt = prompt
                        )
                    }
                    return@launch
                }

                if (!isClassReminder) {
                    val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                    if (taskId != null) {
                        val task = db.taskDao().getTaskByIdDirect(taskId)
                        if (task == null || task.isCompleted) return@launch

                        val linkedPeriod = task.periodBlockId.takeIf { it.isNotBlank() }
                            ?.let { db.periodDao().getAllPeriodsDirect().periodBlock(it) }
                            ?.takeIf { it.subjectId == task.subjectId }
                        val isVacation = calendar?.isManualVacationMode == true ||
                            (calendar?.pauseAlarmsDuringVacation == true &&
                                calendar.isVacation(todayDate, terms))
                        if (isVacation) {
                            TaskReminderScheduler.schedule(
                                context,
                                task,
                                linkedPeriod,
                                calendar,
                                terms
                            )
                            return@launch
                        }
                    }
                }

                if (isClassReminder) {
                    val blockId = intent.getStringExtra(EXTRA_BLOCK_ID)
                    val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 0)
                    val startTime = intent.getStringExtra(EXTRA_START_TIME)
                    if (blockId != null && dayOfWeek in 1..5 && startTime != null) {
                        AlarmScheduler.scheduleClassReminder(
                            context = context,
                            blockId = blockId,
                            subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Class",
                            className = intent.getStringExtra(EXTRA_CLASS_NAME) ?: "SHS",
                            classroom = intent.getStringExtra(EXTRA_CLASSROOM) ?: "Classroom",
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 10),
                            academicCalendar = calendar,
                            academicTerms = terms
                        )
                    }
                }

                // Check vacation pause rule
                if (isClassReminder && calendar != null &&
                    (calendar.isManualVacationMode ||
                        (calendar.pauseAlarmsDuringVacation &&
                            calendar.isVacation(todayDate, terms)))
                ) {
                    // Suppress notification on vacation
                    return@launch
                }

                showNotification(context, title, message, notificationId, isClassReminder)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to deliver scheduled reminder.", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        isClassReminder: Boolean,
        lessonPrompt: LessonLogPrompt? = null
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = if (isClassReminder) {
            com.example.data.notification.EdenseNotificationManager.CHANNEL_CLASSES_ID
        } else {
            com.example.data.notification.EdenseNotificationManager.CHANNEL_TASKS_ID
        }
        com.example.data.notification.EdenseNotificationManager.ensureChannels(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            lessonPrompt?.let {
                putExtra(EXTRA_PROMPT_LOG, true)
                putExtra(EXTRA_BLOCK_ID, it.blockId)
                putExtra(EXTRA_SUBJECT_ID, it.subjectId)
                putExtra(EXTRA_CLASS_NAME, it.className)
                putExtra(EXTRA_SESSION_DATE, it.sessionDate)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission is unavailable; reminder was not shown", e)
        }
    }

    companion object {
        private const val TAG = "EdenseAlarmReceiver"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_IS_CLASS = "extra_is_class"
        const val EXTRA_BLOCK_ID = "extra_block_id"
        const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_CLASS_NAME = "extra_class_name"
        const val EXTRA_CLASSROOM = "extra_classroom"
        const val EXTRA_PROMPT_LOG = "extra_prompt_log"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_SESSION_DATE = "extra_session_date"
    }
}
