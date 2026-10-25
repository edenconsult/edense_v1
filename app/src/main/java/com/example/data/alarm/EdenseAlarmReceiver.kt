package com.example.data.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.EdenseDatabase
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

                showNotification(context, title, message, notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
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

        notificationManager.notify(notificationId, notification)
    }

    companion object {
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
    }
}
