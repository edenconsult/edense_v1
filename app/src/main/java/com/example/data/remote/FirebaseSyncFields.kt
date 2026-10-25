package com.example.data.remote

import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TeacherProfileEntity

internal fun TeacherProfileEntity.toFirestoreFields(userId: String, accountEmail: String?): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "name" to name,
        "email" to (accountEmail ?: email),
        "schoolName" to schoolName,
        "schoolStartTime" to schoolStartTime,
        "schoolEndTime" to schoolEndTime,
        "breakfastBreakStart" to breakfastBreakStart,
        "breakfastBreakEnd" to breakfastBreakEnd,
        "lunchBreakStart" to lunchBreakStart,
        "lunchBreakEnd" to lunchBreakEnd,
        "reminderLeadMinutes" to reminderLeadMinutes,
        "isOnboardingCompleted" to isOnboardingCompleted
    )

internal fun AcademicCalendarEntity.toFirestoreFields(userId: String): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "academicYear" to academicYear,
        "systemType" to systemType,
        "activeTermName" to activeTermName,
        "termStartDate" to termStartDate,
        "termEndDate" to termEndDate,
        "pauseAlarmsDuringVacation" to pauseAlarmsDuringVacation,
        "isManualVacationMode" to isManualVacationMode
    )

internal fun AcademicTermEntity.toFirestoreFields(userId: String): Map<String, Any> =
    mapOf(
        "id" to id,
        "userId" to userId,
        "name" to name,
        "startDate" to startDate,
        "endDate" to endDate,
        "orderIndex" to orderIndex
    )

internal fun SubjectEntity.toFirestoreFields(userId: String): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "name" to name,
        "category" to category,
        "isSelected" to isSelected,
        "assignedClasses" to assignedClasses,
        "defaultClassroom" to defaultClassroom,
        "assignedClassrooms" to assignedClassrooms,
        "targetPeriodsPerWeek" to targetPeriodsPerWeek,
        "colorHex" to colorHex,
        "isArchived" to isArchived
    )

internal fun PeriodEntity.toFirestoreFields(userId: String): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "subjectId" to subjectId,
        "dayOfWeek" to dayOfWeek,
        "startTime" to startTime,
        "endTime" to endTime,
        "blockId" to blockId,
        "classroom" to classroom,
        "className" to className
    )

internal fun TeachingSessionEntity.toFirestoreFields(userId: String): Map<String, Any> =
    mapOf(
        "id" to id,
        "userId" to userId,
        "subjectId" to subjectId,
        "className" to className,
        "sessionDate" to sessionDate,
        "lastTopicCovered" to lastTopicCovered,
        "nextTopicPlanned" to nextTopicPlanned,
        "status" to status,
        "notes" to notes,
        "periodBlockId" to periodBlockId,
        "createdAt" to createdAt
    )

internal fun TaskEntity.toFirestoreFields(userId: String): Map<String, Any> =
    mapOf(
        "id" to id,
        "userId" to userId,
        "subjectId" to subjectId,
        "className" to className,
        "title" to title,
        "type" to type,
        "dueDate" to dueDate,
        "reminderTime" to reminderTime,
        "isCompleted" to isCompleted,
        "notes" to notes,
        "periodBlockId" to periodBlockId,
        "periodReminderTiming" to periodReminderTiming,
        "createdAt" to createdAt
    )
