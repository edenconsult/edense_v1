package com.example.domain.task

enum class TaskType(val code: String, val displayName: String) {
    ASSIGNMENT("ASSIGNMENT", "Assignment / Homework"),
    TEST("TEST", "Class Test / Quiz"),
    EXAM("EXAM", "Examination"),
    PROJECT("PROJECT", "Project / Practical"),
    LESSON_PLAN("LESSON_PLAN", "Lesson Plan"),
    MEETING("MEETING", "Department / Staff Meeting"),
    ADMINISTRATIVE("ADMINISTRATIVE", "SBA / Assessment Records"),
    OTHER("OTHER", "Other Academic Task");

    companion object {
        fun fromCode(code: String): TaskType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: OTHER
        }

        fun displayLabel(code: String): String {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }?.displayName ?: code
        }
    }
}
