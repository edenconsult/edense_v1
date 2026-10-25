package com.example

import com.example.domain.task.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTypeDropdownTest {

    @Test
    fun taskTypeDropdownProvidesStandardAcademicOptions() {
        val types = TaskType.entries.map { it.code }
        assertTrue(types.contains("ASSIGNMENT"))
        assertTrue(types.contains("TEST"))
        assertTrue(types.contains("EXAM"))
        assertTrue(types.contains("PROJECT"))
        assertTrue(types.contains("LESSON_PLAN"))
        assertTrue(types.contains("MEETING"))
        assertTrue(types.contains("ADMINISTRATIVE"))
        assertTrue(types.contains("OTHER"))
    }

    @Test
    fun taskTypeDisplayLabelsAreUserFriendly() {
        assertEquals("Assignment / Homework", TaskType.ASSIGNMENT.displayName)
        assertEquals("Class Test / Quiz", TaskType.TEST.displayName)
        assertEquals("Examination", TaskType.EXAM.displayName)
        assertEquals("Project / Practical", TaskType.PROJECT.displayName)
        assertEquals("Lesson Plan", TaskType.LESSON_PLAN.displayName)
    }

    @Test
    fun taskTypeFromCodeSupportsCaseInsensitiveAndFallback() {
        assertEquals(TaskType.ASSIGNMENT, TaskType.fromCode("assignment"))
        assertEquals(TaskType.EXAM, TaskType.fromCode("EXAM"))
        assertEquals(TaskType.LESSON_PLAN, TaskType.fromCode("lesson_plan"))
        assertEquals(TaskType.OTHER, TaskType.fromCode("NON_EXISTENT_TYPE"))
    }

    @Test
    fun taskTypeDisplayLabelHelperReturnsFormattedNameOrOriginal() {
        assertEquals("Assignment / Homework", TaskType.displayLabel("ASSIGNMENT"))
        assertEquals("Examination", TaskType.displayLabel("EXAM"))
        assertEquals("CUSTOM_LEGACY_TYPE", TaskType.displayLabel("CUSTOM_LEGACY_TYPE"))
    }
}
