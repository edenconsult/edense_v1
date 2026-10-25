package com.example.domain.task

enum class PeriodReminderTiming(val label: String) {
    BEFORE_START("Before class begins"),
    BEFORE_END("Before class ends"),
    AFTER_END("After class ends")
}
