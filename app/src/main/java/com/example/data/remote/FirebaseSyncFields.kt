package com.example.data.remote

import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeachingSessionEntity

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
        "createdAt" to createdAt
    )
