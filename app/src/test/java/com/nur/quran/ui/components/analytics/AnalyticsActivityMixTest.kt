package com.nur.quran.ui.components.analytics

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsActivityMixTest {

    @Test
    fun `calculateActivityMixSegments handles empty list`() {
        val segments = calculateActivityMixSegments(emptyList())
        assertTrue(segments.isEmpty())
    }

    @Test
    fun `calculateActivityMixSegments filters out zero-minute entries`() {
        val input = listOf(
            Triple("Reading", 0, Color.Green),
            Triple("Focus", 0, Color.Red)
        )
        val segments = calculateActivityMixSegments(input)
        assertTrue(segments.isEmpty())
    }

    @Test
    fun `calculateActivityMixSegments single item gets full 360 degrees`() {
        val input = listOf(
            Triple("Reading", 45, Color(0xFF10B981))
        )
        val segments = calculateActivityMixSegments(input)
        assertEquals(1, segments.size)
        assertEquals("Reading", segments[0].name)
        assertEquals(45, segments[0].mins)
        assertEquals(-90f, segments[0].startAngle, 0.01f)
        assertEquals(360f, segments[0].sweepAngle, 0.01f)
    }

    @Test
    fun `calculateActivityMixSegments multiple items have proportional angles and gaps`() {
        val input = listOf(
            Triple("Reading", 50, Color(0xFF10B981)),
            Triple("Memorizing", 50, Color(0xFF3B82F6))
        )
        val segments = calculateActivityMixSegments(input, paddingAngle = 6f)
        assertEquals(2, segments.size)

        // Raw sweep for each is 180f. With paddingAngle = 6f:
        // Gap is 6f, sweep is 174f.
        val seg0 = segments[0]
        val seg1 = segments[1]

        assertEquals("Reading", seg0.name)
        assertEquals(50, seg0.mins)
        assertEquals(-90f + 3f, seg0.startAngle, 0.01f) // -87f
        assertEquals(174f, seg0.sweepAngle, 0.01f)

        assertEquals("Memorizing", seg1.name)
        assertEquals(50, seg1.mins)
        assertEquals(-90f + 180f + 3f, seg1.startAngle, 0.01f) // 93f
        assertEquals(174f, seg1.sweepAngle, 0.01f)

        // Gap between seg0 end (-87 + 174 = 87f) and seg1 start (93f) is exactly 6f
        val seg0End = seg0.startAngle + seg0.sweepAngle
        val gap = seg1.startAngle - seg0End
        assertEquals(6f, gap, 0.01f)
    }

    @Test
    fun `calculateActivityMixSegments handles four standard categories matching web Progress`() {
        val input = listOf(
            Triple("Reading", 45, Color(0xFF10B981)),
            Triple("Memorizing", 30, Color(0xFF3B82F6)),
            Triple("Focus", 15, Color(0xFF8B5CF6)),
            Triple("Listening", 10, Color(0xFFF59E0B))
        )
        val totalMins = 45 + 30 + 15 + 10 // 100
        val segments = calculateActivityMixSegments(input, paddingAngle = 6f)

        assertEquals(4, segments.size)
        assertEquals("Reading", segments[0].name)
        assertEquals("Memorizing", segments[1].name)
        assertEquals("Focus", segments[2].name)
        assertEquals("Listening", segments[3].name)

        // Reading is 45% -> raw sweep = 162f
        assertEquals(162f - 6f, segments[0].sweepAngle, 0.01f)

        // Memorizing is 30% -> raw sweep = 108f
        assertEquals(108f - 6f, segments[1].sweepAngle, 0.01f)

        // Focus is 15% -> raw sweep = 54f
        assertEquals(54f - 6f, segments[2].sweepAngle, 0.01f)

        // Listening is 10% -> raw sweep = 36f
        assertEquals(36f - 6f, segments[3].sweepAngle, 0.01f)

        // Ensure angles strictly advance clockwise
        for (i in 0 until segments.size - 1) {
            assertTrue(segments[i + 1].startAngle > segments[i].startAngle)
        }
    }

    @Test
    fun `calculateActivityMixSegments ignores zero-minute categories in mixed list`() {
        val input = listOf(
            Triple("Reading", 60, Color.Green),
            Triple("Memorizing", 0, Color.Blue),
            Triple("Focus", 40, Color.Magenta)
        )
        val segments = calculateActivityMixSegments(input)
        assertEquals(2, segments.size)
        assertEquals("Reading", segments[0].name)
        assertEquals("Focus", segments[1].name)
    }
}
