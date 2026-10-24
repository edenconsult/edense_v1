package com.example.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.EdenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = EdenseDatabase.getDatabase(context)
                    val periods = db.periodDao().getAllPeriodsDirect()
                    val subjects = db.subjectDao().getSelectedSubjectsDirect().associateBy { it.id }
                    val profile = db.teacherProfileDao().getProfileDirect()
                    val leadMinutes = profile?.reminderLeadMinutes ?: 10

                    for (blockPeriods in periods.groupBy { it.blockId }.values) {
                        val period = blockPeriods.minByOrNull { it.startTime } ?: continue
                        val subject = subjects[period.subjectId] ?: continue
                        AlarmScheduler.scheduleClassReminder(
                            context = context,
                            blockId = period.blockId,
                            subjectName = subject.name,
                            className = period.className.ifBlank { subject.assignedClasses.ifBlank { "SHS" } },
                            classroom = period.classroom.ifBlank { subject.defaultClassroom },
                            dayOfWeek = period.dayOfWeek,
                            startTime = period.startTime,
                            leadMinutes = leadMinutes
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
