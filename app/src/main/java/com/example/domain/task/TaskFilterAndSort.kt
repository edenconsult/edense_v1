package com.example.domain.task

enum class TaskSortOrder(val label: String) {
    DUE_DATE_ASC("Due Date (Earliest first)"),
    DUE_DATE_DESC("Due Date (Latest first)"),
    PRIORITY("Priority / Urgency"),
    SUBJECT("Subject (A to Z)"),
    TITLE("Task Title (A to Z)")
}

enum class TaskPriorityFilter(val label: String) {
    ALL("All Priorities"),
    OVERDUE("Overdue"),
    DUE_TODAY("Due Today"),
    DUE_SOON("Due Soon"),
    UPCOMING("Upcoming")
}
