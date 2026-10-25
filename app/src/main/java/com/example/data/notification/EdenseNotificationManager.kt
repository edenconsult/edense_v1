package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.local.entity.TaskEntity
import com.example.domain.task.TaskType

enum class TaskChangeType(val actionTitle: String) {
    CREATED("New Task Scheduled"),
    UPDATED("Deadline Updated"),
    COMPLETED("Task Completed"),
    RESCHEDULED("Task Rescheduled")
}

object EdenseNotificationManager {

    const val CHANNEL_TASKS_ID = "edense_tasks_channel"
    const val CHANNEL_CLASSES_ID = "edense_reminders_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                "Academic Task Deadlines & Changes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming academic deadlines, tests, assignments, and task updates"
                enableVibration(true)
                setShowBadge(true)
            }

            val classesChannel = NotificationChannel(
                CHANNEL_CLASSES_ID,
                "Class Timetable Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming teaching periods and class sessions"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(tasksChannel)
            notificationManager.createNotificationChannel(classesChannel)
        }
    }

    fun notifyTaskDeadline(context: Context, task: TaskEntity, subjectName: String? = null) {
        ensureChannels(context)
        val typeLabel = TaskType.displayLabel(task.type)
        val subjectSuffix = if (!subjectName.isNullOrBlank()) " ($subjectName)" else ""
        val title = "Academic Deadline Due: ${task.title}"
        val message = "$typeLabel$subjectSuffix is due on ${task.dueDate} at ${task.reminderTime}."
        val notificationId = "task_deadline_${task.id}".hashCode()

        postNotification(
            context = context,
            channelId = CHANNEL_TASKS_ID,
            title = title,
            message = message,
            notificationId = notificationId
        )
    }

    fun notifyTaskChange(
        context: Context,
        task: TaskEntity,
        changeType: TaskChangeType,
        subjectName: String? = null
    ) {
        ensureChannels(context)
        val typeLabel = TaskType.displayLabel(task.type)
        val subjectSuffix = if (!subjectName.isNullOrBlank()) " • $subjectName" else ""
        val title = "${changeType.actionTitle}: ${task.title}"
        val message = when (changeType) {
            TaskChangeType.CREATED -> "$typeLabel scheduled for ${task.dueDate}$subjectSuffix."
            TaskChangeType.UPDATED, TaskChangeType.RESCHEDULED -> "$typeLabel deadline changed to ${task.dueDate}$subjectSuffix."
            TaskChangeType.COMPLETED -> "$typeLabel marked as finished$subjectSuffix."
        }
        val notificationId = "task_change_${task.id}".hashCode()

        postNotification(
            context = context,
            channelId = CHANNEL_TASKS_ID,
            title = title,
            message = message,
            notificationId = notificationId
        )
    }

    fun cancelTaskNotification(context: Context, taskId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel("task_deadline_$taskId".hashCode())
        notificationManager.cancel("task_change_$taskId".hashCode())
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int
    ) {
        try {
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

            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (error: SecurityException) {
            Log.e("EdenseNotification", "Notification permission is unavailable.", error)
        }
    }
}
