package com.nur.quran.ui.components.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileUtilsTest {

    @Test
    fun `formatMinutes formats less than one hour correctly`() {
        assertEquals("0m", ProfileUtils.formatMinutes(0))
        assertEquals("1m", ProfileUtils.formatMinutes(60))
        assertEquals("2m", ProfileUtils.formatMinutes(90))
        assertEquals("45m", ProfileUtils.formatMinutes(2700))
    }

    @Test
    fun `formatMinutes formats hours and minutes correctly`() {
        assertEquals("1h", ProfileUtils.formatMinutes(3600))
        assertEquals("1h 15m", ProfileUtils.formatMinutes(4500))
        assertEquals("2h", ProfileUtils.formatMinutes(7200))
        assertEquals("2h 30m", ProfileUtils.formatMinutes(9000))
    }

    @Test
    fun `timeAgo returns relative timestamps accurately`() {
        val now = 1_000_000_000L
        assertEquals("Never", ProfileUtils.timeAgo(null, now))
        assertEquals("Never", ProfileUtils.timeAgo(0L, now))
        assertEquals("Just now", ProfileUtils.timeAgo(now - 30_000L, now))
        assertEquals("5m ago", ProfileUtils.timeAgo(now - 5 * 60_000L, now))
        assertEquals("3h ago", ProfileUtils.timeAgo(now - 3 * 3600_000L, now))
        assertEquals("2d ago", ProfileUtils.timeAgo(now - 2 * 86400_000L, now))
    }

    @Test
    fun `getInitials extracts two letters properly`() {
        assertEquals("?", ProfileUtils.getInitials(null))
        assertEquals("?", ProfileUtils.getInitials(""))
        assertEquals("AI", ProfileUtils.getInitials("Ali Imran"))
        assertEquals("JD", ProfileUtils.getInitials("john.doe@example.com"))
        assertEquals("QU", ProfileUtils.getInitials("quran_user@test.org"))
        assertEquals("AL", ProfileUtils.getInitials("Ali"))
    }

    @Test
    fun `getGreeting returns correct greeting based on hour`() {
        assertEquals("Good Morning", ProfileUtils.getGreeting(8))
        assertEquals("Good Morning", ProfileUtils.getGreeting(11))
        assertEquals("Good Afternoon", ProfileUtils.getGreeting(12))
        assertEquals("Good Afternoon", ProfileUtils.getGreeting(16))
        assertEquals("Good Evening", ProfileUtils.getGreeting(17))
        assertEquals("Good Evening", ProfileUtils.getGreeting(23))
    }
}
