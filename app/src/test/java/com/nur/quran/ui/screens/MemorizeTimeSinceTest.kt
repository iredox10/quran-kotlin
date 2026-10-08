package com.nur.quran.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MemorizeTimeSinceTest {

    private val now = 1_700_000_000_000L

    @Test
    fun minutesUnderHour() {
        assertEquals("0m ago", memorizeTimeSince(now, now))
        assertEquals("5m ago", memorizeTimeSince(now - 5 * 60_000, now))
        assertEquals("59m ago", memorizeTimeSince(now - 59 * 60_000, now))
    }

    @Test
    fun hoursUnderDay() {
        assertEquals("1h ago", memorizeTimeSince(now - 60 * 60_000, now))
        assertEquals("23h ago", memorizeTimeSince(now - 23 * 60 * 60_000, now))
    }

    @Test
    fun daysBeyond() {
        assertEquals("1d ago", memorizeTimeSince(now - 24 * 60 * 60_000, now))
        assertEquals("3d ago", memorizeTimeSince(now - 3 * 24 * 60 * 60_000, now))
    }

    @Test
    fun futureClampsToZero() {
        assertEquals("0m ago", memorizeTimeSince(now + 60_000, now))
    }
}
