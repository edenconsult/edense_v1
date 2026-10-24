package com.example

import com.example.domain.home.timeOfDayGreeting
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOfDayGreetingTest {
    @Test
    fun greetingChangesAtTimeOfDayBoundaries() {
        assertEquals("Good evening", timeOfDayGreeting("04:59"))
        assertEquals("Good morning", timeOfDayGreeting("05:00"))
        assertEquals("Good morning", timeOfDayGreeting("11:59"))
        assertEquals("Good afternoon", timeOfDayGreeting("12:00"))
        assertEquals("Good afternoon", timeOfDayGreeting("16:59"))
        assertEquals("Good evening", timeOfDayGreeting("17:00"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHourIsRejected() {
        timeOfDayGreeting("24:00")
    }
}
