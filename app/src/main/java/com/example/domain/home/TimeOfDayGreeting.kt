package com.example.domain.home

fun timeOfDayGreeting(time: String): String {
    val hour = time.substringBefore(':').toIntOrNull()
    require(hour != null && hour in 0..23) { "Time must start with an hour from 00 to 23" }
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
