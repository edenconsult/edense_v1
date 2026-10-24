package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.alarm.AlarmScheduler
import com.example.data.alarm.TaskReminderScheduler
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
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

    val academicTerms: Flow<List<AcademicTermEntity>> =
        database.academicTermDao().getTerms()

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

    fun getLatestSessionForClass(
        subjectId: String,
        className: String
    ): Flow<TeachingSessionEntity?> =
        database.teachingSessionDao().getLatestSessionForClass(subjectId, className)

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
        rescheduleClassReminders()
    }

    suspend fun saveAcademicTerms(terms: List<AcademicTermEntity>) {
        database.academicTermDao().replaceAll(terms)
        rescheduleClassReminders()
    }

    suspend fun rescheduleClassReminders() {
        val periods = database.periodDao().getAllPeriodsDirect()
        val subjects = database.subjectDao().getSelectedSubjectsDirect().associateBy { it.id }
        val profile = database.teacherProfileDao().getProfileDirect()
        val calendar = database.academicCalendarDao().getCalendarDirect()
        val terms = database.academicTermDao().getTermsDirect()
        val leadMinutes = profile?.reminderLeadMinutes ?: 10

        periods.groupBy { it.blockId }.values.forEach { blockPeriods ->
            val period = blockPeriods.minByOrNull { it.startTime } ?: return@forEach
            val subject = subjects[period.subjectId] ?: return@forEach
            AlarmScheduler.cancelAlarm(appContext, period.blockId)
            AlarmScheduler.scheduleClassReminder(
                context = appContext,
                blockId = period.blockId,
                subjectName = subject.name,
                className = period.className.ifBlank { subject.assignedClasses.ifBlank { "SHS" } },
                classroom = period.classroom.ifBlank { subject.defaultClassroom },
                dayOfWeek = period.dayOfWeek,
                startTime = period.startTime,
                leadMinutes = leadMinutes,
                academicCalendar = calendar,
                academicTerms = terms
            )
        }
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

    suspend fun addCustomSubject(name: String, category: String): Boolean {
        val normalizedName = name.trim().replace(Regex("\\s+"), " ")
        require(normalizedName.isNotBlank()) { "Enter a subject name." }
        require(category == "CORE" || category == "ELECTIVE") {
            "Choose a valid subject category."
        }
        if (database.subjectDao().getSubjectByNameDirect(normalizedName) != null) {
            return false
        }

        database.subjectDao().insertOrUpdate(
            SubjectEntity(
                id = "custom_${UUID.randomUUID()}",
                name = normalizedName,
                category = category,
                colorHex = if (category == "CORE") "#1E6F5C" else "#7C3AED"
            )
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
        val calendar = database.academicCalendarDao().getCalendarDirect()
        val terms = database.academicTermDao().getTermsDirect()

        if (subject != null) {
            AlarmScheduler.scheduleClassReminder(
                context = appContext,
                blockId = blockId,
                subjectName = subject.name,
                className = className.ifBlank { subject.assignedClasses.ifBlank { "SHS" } },
                classroom = classroom.ifBlank { subject.defaultClassroom },
                dayOfWeek = dayOfWeek,
                startTime = selectedSlot.startTime,
                leadMinutes = leadMinutes,
                academicCalendar = calendar,
                academicTerms = terms
            )
        }
    }

    suspend fun deletePeriod(id: String, blockId: String) {
        database.periodDao().deletePeriod(id)
        AlarmScheduler.cancelAlarm(appContext, blockId)
    }

    suspend fun logSession(
        subjectId: String,
        className: String,
        sessionDate: String,
        lastTopicCovered: String,
        nextTopicPlanned: String,
        status: String,
        notes: String = "",
        createFollowUpTask: Boolean = false,
        followUpDueDate: String = ""
    ) {
        val session = TeachingSessionEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            className = className,
            sessionDate = sessionDate,
            lastTopicCovered = lastTopicCovered,
            nextTopicPlanned = nextTopicPlanned,
            status = status,
            notes = notes
        )
        val followUpTask = if (createFollowUpTask) {
            require(nextTopicPlanned.isNotBlank()) {
                "A planned next topic is required to create a follow-up task"
            }
            require(followUpDueDate.isNotBlank()) {
                "A due date is required to create a follow-up task"
            }
            TaskEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                title = "Prepare next lesson: $nextTopicPlanned",
                type = "FOLLOW_UP",
                dueDate = followUpDueDate,
                notes = "Created from the teaching session logged on $sessionDate.",
                className = className
            )
        } else {
            null
        }
        database.withTransaction {
            database.teachingSessionDao().insertSession(session)
            followUpTask?.let { database.taskDao().insertTask(it) }
        }
        followUpTask?.let { TaskReminderScheduler.schedule(appContext, it) }
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
        TaskReminderScheduler.schedule(appContext, task)
    }

    suspend fun toggleTaskCompleted(id: String, isCompleted: Boolean) {
        val task = database.taskDao().getTaskByIdDirect(id) ?: return
        database.taskDao().setTaskCompleted(id, isCompleted)
        if (isCompleted) {
            TaskReminderScheduler.cancel(appContext, id)
        } else {
            TaskReminderScheduler.schedule(appContext, task.copy(isCompleted = false))
        }
    }

    suspend fun deleteTask(id: String) {
        database.taskDao().deleteTask(id)
        TaskReminderScheduler.cancel(appContext, id)
    }
}
