package com.example.data.alarm

import android.content.Context
import android.util.Log
import com.example.data.local.EdenseDatabase
import com.example.domain.schedule.periodBlock

object ReminderRescheduler {
    suspend fun rescheduleAll(context: Context) {
        val database = EdenseDatabase.getDatabase(context)
        val periods = database.periodDao().getAllPeriodsDirect()
        val subjects = database.subjectDao().getSelectedSubjectsDirect().associateBy { it.id }
        val profile = database.teacherProfileDao().getProfileDirect()
        val calendar = database.academicCalendarDao().getCalendarDirect()
        val terms = database.academicTermDao().getTermsDirect()
        val tasks = database.taskDao().getPendingTasksDirect()

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
                leadMinutes = profile?.reminderLeadMinutes ?: 10,
                academicCalendar = calendar,
                academicTerms = terms
            )
            LessonLogPromptScheduler.schedule(
                context,
                blockPeriods,
                subject.name,
                calendar,
                terms
            )
        }

        tasks.forEach { task ->
            val linkedPeriod = periods.periodBlock(task.periodBlockId)
                ?.takeIf { task.periodBlockId.isNotBlank() && it.subjectId == task.subjectId }
            TaskReminderScheduler.schedule(context, task, linkedPeriod, calendar, terms)
        }

        Log.i(
            "ReminderRescheduler",
            "Rescheduled ${periods.groupBy { it.blockId }.size} class blocks and ${tasks.size} task reminders."
        )
    }
}
