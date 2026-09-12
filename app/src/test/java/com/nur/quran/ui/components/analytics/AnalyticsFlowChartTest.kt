package com.nur.quran.ui.components.analytics

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsFlowChartTest {

    @Test
    fun `computeYTicks handles zero minutes cleanly`() {
        val ticks = computeYTicks(0)
        assertEquals(listOf(0, 15, 30), ticks)
    }

    @Test
    fun `computeYTicks handles low minutes`() {
        val ticks = computeYTicks(20)
        assertEquals(listOf(0, 15, 30), ticks)
    }

    @Test
    fun `computeYTicks handles medium minutes`() {
        val ticks = computeYTicks(45)
        assertEquals(listOf(0, 15, 30, 45, 60), ticks)
    }

    @Test
    fun `computeYTicks handles large minutes`() {
        val ticks = computeYTicks(80)
        assertEquals(listOf(0, 30, 60, 90), ticks)
    }

    @Test
    fun `computeYTicks handles extra large minutes`() {
        val ticks = computeYTicks(150)
        assertEquals(listOf(0, 60, 120, 180), ticks)
    }

    @Test
    fun `findNearestIndex handles empty points`() {
        val idx = findNearestIndex(100f, emptyList())
        assertEquals(-1, idx)
    }

    @Test
    fun `findNearestIndex returns closest point index`() {
        val points = listOf(
            Offset(0f, 10f),
            Offset(50f, 20f),
            Offset(100f, 30f),
            Offset(150f, 40f)
        )
        assertEquals(0, findNearestIndex(-10f, points))
        assertEquals(0, findNearestIndex(20f, points))
        assertEquals(1, findNearestIndex(35f, points))
        assertEquals(1, findNearestIndex(70f, points))
        assertEquals(2, findNearestIndex(80f, points))
        assertEquals(3, findNearestIndex(140f, points))
        assertEquals(3, findNearestIndex(200f, points))
    }

    @Test
    fun `formatDateForIndex calculates date correctly`() {
        val formatted = formatDateForIndex(index = 6, totalDays = 7, dayLabel = "Mon")
        assertTrue(formatted.isNotEmpty())
    }

    @Test
    fun `formatDateForIndex skips if already contains digit`() {
        val formatted = formatDateForIndex(index = 6, totalDays = 7, dayLabel = "Sep 12")
        assertEquals("", formatted)
    }
}
