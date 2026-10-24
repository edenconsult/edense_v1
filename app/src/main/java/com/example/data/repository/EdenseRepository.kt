package com.example.data.repository

import android.content.Context
import com.example.data.alarm.AlarmScheduler
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.example.domain.schedule.SchedulingEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EdenseRepository(
    private val database: EdenseDatabase,
    private val appContext: Context
) {
    val teacherProfile: Flow<TeacherProfileEntity?> =
        database.teacherProfileDao().getProfile()

    val academicCalendar: Flow<AcademicCalendarEntity?> =
        database.academicCalendarDao().getCalendar()

    val allSubjects: Flow<List<SubjectEntity>> =
        database.subjectDao().getAllSubjects()

    val selectedSubjects: Flow<List<SubjectEntity>> =
        database.subjectDao().getSelectedSubjects()

    val selectedSubjectsCount: Flow<Int> =
        database.subjectDao().getSelectedCount()

    val allPeriods: Flow<List<PeriodEntity>> =
        database.periodDao().getAllPeriods()

    fun getPeriodsForDay(dayOfWeek: Int): Flow<List<PeriodEntity>> =
        database.periodDao().getPeriodsForDay(dayOfWeek)

    fun getPeriodsForSubject(subjectId: String): Flow<List<PeriodEntity>> =
        database.periodDao().getPeriodsForSubject(subjectId)

    val allSessions: Flow<List<TeachingSessionEntity>> =
        database.teachingSessionDao().getAllSessions()

    fun getSessionsForSubject(subjectId: String): Flow<List<TeachingSessionEntity>> =
        database.teachingSessionDao().getSessionsForSubject(subjectId)

    fun getLatestSessionForSubject(subjectId: String): Flow<TeachingSessionEntity?> =
        database.teachingSessionDao().getLatestSessionForSubject(subjectId)

    val allTasks: Flow<List<TaskEntity>> =
        database.taskDao().getAllTasks()

    val pendingTasks: Flow<List<TaskEntity>> =
        database.taskDao().getPendingTasks()

    // ---- Updates ----

    suspend fun saveTeacherProfile(profile: TeacherProfileEntity) {
        database.teacherProfileDao().insertOrUpdate(profile)
    }

    suspend fun saveAcademicCalendar(calendar: AcademicCalendarEntity) {
        database.academicCalendarDao().insertOrUpdate(calendar)
    }

    /**
     * Selects or updates a teacher's subject. Enforces maximum 2 subjects rule.
     * Returns true if successfully selected, or false if 2 subjects are already chosen.
     */
    suspend fun selectSubject(
        subjectId: String,
        assignedClasses: String,
        defaultClassroom: String,
        assignedClassrooms: String,
        targetPeriodsPerWeek: Int
    ): Boolean {
        val currentCount = database.subjectDao().getSelectedCountDirect()
        val subject = database.subjectDao().getSubjectByIdDirect(subjectId) ?: return false

        // If not already selected and limit reached, deny
        if (!subject.isSelected && currentCount >= 2) {
            return false
        }

        database.subjectDao().updateSubjectSelection(
            id = subjectId,
            isSelected = true,
            assignedClasses = assignedClasses,
            defaultClassroom = defaultClassroom,
            assignedClassrooms = assignedClassrooms,
            targetPeriods = targetPeriodsPerWeek
        )
        return true
    }

    suspend fun deselectSubject(subjectId: String) {
        database.subjectDao().updateSubjectSelection(
            id = subjectId,
            isSelected = false,
            assignedClasses = "",
            defaultClassroom = "",
            assignedClassrooms = "",
            targetPeriods = 0
        )
        // Also clean up timetable periods for this subject
        database.periodDao().deletePeriodsForSubject(subjectId)
    }

    suspend fun addPeriod(
        subjectId: String,
        dayOfWeek: Int,
        startTime: String,
        className: String,
        classroom: String,
        isDoublePeriod: Boolean
    ) {
        val selectedSlot = SchedulingEngine.periodSlots
            .firstOrNull { it.startTime == startTime }
            ?: throw IllegalArgumentException("No school period starts at $startTime")
        val slotIndex = SchedulingEngine.periodSlots.indexOf(selectedSlot)
        val secondSlot = SchedulingEngine.periodSlots.getOrNull(slotIndex + 1)
        if (isDoublePeriod && (secondSlot == null || secondSlot.startMinute != selectedSlot.endMinute)) {
            throw IllegalArgumentException("A double period must use two consecutive periods without a break")
        }

        val blockId = UUID.randomUUID().toString()
        val periods = buildList {
            add(
                PeriodEntity(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    dayOfWeek = dayOfWeek,
                    startTime = selectedSlot.startTime,
                    endTime = selectedSlot.endTime,
                    blockId = blockId,
                    classroom = classroom,
                    className = className
                )
            )
            if (isDoublePeriod && secondSlot != null) {
                add(
                    PeriodEntity(
                        id = UUID.randomUUID().toString(),
                        subjectId = subjectId,
                        dayOfWeek = dayOfWeek,
                        startTime = secondSlot.startTime,
                        endTime = secondSlot.endTime,
                        blockId = blockId,
                        classroom = classroom,
                        className = className
                    )
                )
            }
        }
        database.periodDao().insertPeriods(periods)

        val subject = database.subjectDao().getSubjectByIdDirect(subjectId)
        val profile = database.teacherProfileDao().getProfileDirect()
        val leadMinutes = profile?.reminderLeadMinutes ?: 10

        if (subject != null) {
            AlarmScheduler.scheduleClassReminder(
                context = appContext,
                blockId = blockId,
                subjectName = subject.name,
                className = className.ifBlank { subject.assignedClasses.ifBlank { "SHS" } },
                classroom = classroom.ifBlank { subject.defaultClassroom },
                dayOfWeek = dayOfWeek,
                startTime = selectedSlot.startTime,
                leadMinutes = leadMinutes
            )
        }
    }

    suspend fun deletePeriod(id: String, blockId: String) {
        database.periodDao().deletePeriod(id)
        AlarmScheduler.cancelAlarm(appContext, blockId)
    }

    suspend fun logSession(
        subjectId: String,
        sessionDate: String,
        lastTopicCovered: String,
        nextTopicPlanned: String,
        status: String,
        notes: String = ""
    ) {
        val session = TeachingSessionEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            sessionDate = sessionDate,
            lastTopicCovered = lastTopicCovered,
            nextTopicPlanned = nextTopicPlanned,
            status = status,
            notes = notes
        )
        database.teachingSessionDao().insertSession(session)
    }

    suspend fun addTask(
        title: String,
        subjectId: String,
        type: String,
        dueDate: String,
        reminderTime: String = "06:00",
        notes: String = ""
    ) {
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            subjectId = subjectId,
            type = type,
            dueDate = dueDate,
            reminderTime = reminderTime,
            notes = notes,
            isCompleted = false
        )
        database.taskDao().insertTask(task)
    }

    suspend fun toggleTaskCompleted(id: String, isCompleted: Boolean) {
        database.taskDao().setTaskCompleted(id, isCompleted)
    }

    suspend fun deleteTask(id: String) {
        database.taskDao().deleteTask(id)
    }
}
