package com.example.domain.schedule

object ClassroomAssignments {
    fun parse(values: String): List<String> =
        values.split(",").map(String::trim).filter(String::isNotEmpty)

    fun defaultRooms(classes: List<String>): List<String> = classes

    fun roomForClass(className: String, classes: List<String>, rooms: List<String>): String {
        val index = classes.indexOf(className)
        return rooms.getOrNull(index)?.takeIf(String::isNotBlank) ?: className
    }
}
