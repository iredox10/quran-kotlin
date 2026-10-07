package com.nur.quran.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsFlowSummaryTest {

    @Test
    fun `summarize totals with minutes and byType`() {
        val sessions = listOf(
            AnalyticsStats.Session("2026-10-07", 120, type = "reading"),
            AnalyticsStats.Session("2026-10-06", 60, type = "listening"),
            AnalyticsStats.Session("2026-10-06", 0)
        )
        val s = AnalyticsStats.summarize(sessions)
        assertEquals(180L, s.seconds)
        assertEquals(3, s.count)
        assertEquals(2, s.activeDays)
        assertEquals(2, s.avgPerDayMin)
        assertEquals(3, s.minutes)
        assertEquals(120L, s.byType["reading"])
    }

    @Test
    fun `summarize empty list is all zeros`() {
        val s = AnalyticsStats.summarize(emptyList())
        assertEquals(0L, s.seconds)
        assertEquals(0, s.count)
        assertEquals(0, s.activeDays)
        assertEquals(0, s.avgPerDayMin)
    }

    @Test
    fun `formatDuration matches web labels`() {
        assertEquals("42m", AnalyticsStats.formatDuration(42 * 60L))
        assertEquals("1h 5m", AnalyticsStats.formatDuration(65 * 60L))
    }

    @Test
    fun `deltaPercent null without baseline zero without data`() {
        assertNull(AnalyticsStats.deltaPercent(60, 0))
        assertEquals(0, AnalyticsStats.deltaPercent(0, 0))
        assertEquals(50, AnalyticsStats.deltaPercent(90, 60))
        assertEquals(-50, AnalyticsStats.deltaPercent(30, 60))
    }
}
