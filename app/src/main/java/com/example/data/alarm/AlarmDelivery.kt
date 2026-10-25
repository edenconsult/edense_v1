package com.example.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log

internal object AlarmDelivery {
    fun schedule(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        tag: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            return
        }

        val canUseExactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (!canUseExactAlarm) {
            Log.w(tag, "Exact alarm access is disabled; scheduling an inexact reminder.")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (error: SecurityException) {
            Log.e(tag, "Exact alarm access was revoked; falling back to inexact scheduling.", error)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
