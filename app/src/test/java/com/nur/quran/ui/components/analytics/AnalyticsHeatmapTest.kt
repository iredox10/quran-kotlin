package com.nur.quran.ui.components.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsHeatmapTest {

    @Test
    fun `level 0 for zero or inactive duration`() {
        assertEquals(0, getHeatmapLevel(0))
        assertEquals(0, getHeatmapLevel(-5))
    }

    @Test
    fun `level 1 for 1 to 9 minutes read`() {
        assertEquals(1, getHeatmapLevel(1))
        assertEquals(1, getHeatmapLevel(5))
        assertEquals(1, getHeatmapLevel(9))
    }

    @Test
    fun `level 2 for 10 to 29 minutes read`() {
        assertEquals(2, getHeatmapLevel(10))
        assertEquals(2, getHeatmapLevel(15))
        assertEquals(2, getHeatmapLevel(29))
    }

    @Test
    fun `level 3 for 30 minutes or more read`() {
        assertEquals(3, getHeatmapLevel(30))
        assertEquals(3, getHeatmapLevel(45))
        assertEquals(3, getHeatmapLevel(120))
    }

    @Test
    fun `formatHeatmapBannerText formats active minutes with em-dash`() {
        val formatted = formatHeatmapBannerText("2026-09-12", 15)
        assertEquals("2026-09-12 — 15m", formatted)
    }

    @Test
    fun `formatHeatmapBannerText formats zero minutes as No activity`() {
        val formatted = formatHeatmapBannerText("2026-09-12", 0)
        assertEquals("2026-09-12 — No activity", formatted)
    }

    @Test
    fun `formatHeatmapBannerText formats negative minutes as No activity`() {
        val formatted = formatHeatmapBannerText("2026-09-12", -1)
        assertEquals("2026-09-12 — No activity", formatted)
    }

    @Test
    fun `formatHeatmapBannerText works with formatted date strings`() {
        val formatted = formatHeatmapBannerText("Sat Sep 12", 45)
        assertEquals("Sat Sep 12 — 45m", formatted)
    }

    @Test
    fun `35 days data chunks into 5 weeks of 7 days`() {
        val testData = (1..35).map { i -> Pair("Day $i", i) }
        val weeks = testData.chunked(7)
        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals("Day 1", weeks[0][0].first)
        assertEquals("Day 7", weeks[0][6].first)
        assertEquals("Day 8", weeks[1][0].first)
        assertEquals("Day 35", weeks[4][6].first)
    }
}
