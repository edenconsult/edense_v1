package com.example

import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.example.ui.screens.sessions.buildClassCarryForwards
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionsCarryForwardTest {
    @Test
    fun carryForwardIsScopedToEachAssignedClass() {
        val subject = SubjectEntity(
            id = "math",
            name = "Mathematics",
            assignedClasses = "3A9, 3A11"
        )
        val sessions = listOf(
            TeachingSessionEntity(
                id = "session-a",
                subjectId = "math",
                sessionDate = "2026-10-01",
                lastTopicCovered = "Algebra",
                nextTopicPlanned = "Equations",
                className = "3A9"
            ),
            TeachingSessionEntity(
                id = "session-b",
                subjectId = "math",
                sessionDate = "2026-10-02",
                lastTopicCovered = "Geometry",
                nextTopicPlanned = "Triangles",
                className = "3A11"
            )
        )

        val summaries = buildClassCarryForwards(listOf(subject), sessions)

        assertEquals(listOf("3A9", "3A11"), summaries.map { it.className })
        assertEquals("Algebra", summaries[0].latestSession?.lastTopicCovered)
        assertEquals("Geometry", summaries[1].latestSession?.lastTopicCovered)
    }

    @Test
    fun legacySessionCarriesForwardOnlyWhenSubjectHasOneClass() {
        val subject = SubjectEntity(
            id = "math",
            name = "Mathematics",
            assignedClasses = "3A9"
        )
        val legacySession = TeachingSessionEntity(
            id = "legacy",
            subjectId = "math",
            sessionDate = "2026-10-01",
            lastTopicCovered = "Algebra",
            nextTopicPlanned = "Equations"
        )

        val summaries = buildClassCarryForwards(listOf(subject), listOf(legacySession))

        assertEquals("Algebra", summaries.single().latestSession?.lastTopicCovered)
    }
}
