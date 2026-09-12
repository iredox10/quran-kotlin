package com.nur.quran.ui.components.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerUtilsTest {

    @Test
    fun `getPaceStats calculates 30 days Ramadan pace correctly`() {
        val stats = PlannerUtils.getPaceStats(durationDays = 30, totalUnits = 604, startDateStr = "2026-03-01")
        assertEquals(21, stats.dailyPages)
        assertEquals(5, stats.perPrayer)
        assertTrue(stats.endLabel.contains("2026"))
    }

    @Test
    fun `getPaceStats calculates 60 days Steady pace correctly`() {
        val stats = PlannerUtils.getPaceStats(durationDays = 60, totalUnits = 604, startDateStr = "2026-01-01")
        assertEquals(11, stats.dailyPages)
        assertEquals(3, stats.perPrayer)
    }

    @Test
    fun `getPaceStats accounts for excluded days`() {
        // Exclude Friday (5 in JS getDay)
        val stats = PlannerUtils.getPaceStats(durationDays = 7, excludeDays = listOf(5), startDateStr = "2026-01-01")
        assertEquals(87, stats.dailyPages)
    }

    @Test
    fun `formatSessionTime formats various durations`() {
        assertEquals("0m", PlannerUtils.formatSessionTime(0))
        assertEquals("45s", PlannerUtils.formatSessionTime(45))
        assertEquals("12m", PlannerUtils.formatSessionTime(720))
        assertEquals("1h 15m", PlannerUtils.formatSessionTime(4500))
    }

    @Test
    fun `formatDaysRemaining formats correctly`() {
        assertEquals("Completed", PlannerUtils.formatDaysRemaining(30, 30))
        assertEquals("1 day left", PlannerUtils.formatDaysRemaining(30, 29))
        assertEquals("15 days left", PlannerUtils.formatDaysRemaining(30, 15))
    }

    @Test
    fun `calculateRingProgress clamps between 0 and 1`() {
        assertEquals(0f, PlannerUtils.calculateRingProgress(0, 30), 0.001f)
        assertEquals(0.5f, PlannerUtils.calculateRingProgress(15, 30), 0.001f)
        assertEquals(1.0f, PlannerUtils.calculateRingProgress(30, 30), 0.001f)
        assertEquals(1.0f, PlannerUtils.calculateRingProgress(35, 30), 0.001f)
    }
}
