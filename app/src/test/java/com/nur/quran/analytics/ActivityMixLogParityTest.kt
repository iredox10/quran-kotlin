package com.nur.quran.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ActivityMixLogParityTest {

    private fun date(year: Int, month: Int, day: Int) =
        Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }.time

    @Test
    fun `mixRangeTitle all returns All time`() {
        assertEquals("All time", AnalyticsStats.mixRangeTitle("all", date(2026, 9, 7)))
    }

    @Test
    fun `mixRangeTitle today returns single day label`() {
        assertEquals("Oct 7", AnalyticsStats.mixRangeTitle("today", date(2026, 9, 7)))
    }

    @Test
    fun `mixRangeTitle week spans Sunday start`() {
        // Wed Oct 7 2026 -> week Sun Oct 4 .. Sat Oct 10.
        assertEquals("Oct 4 – 10", AnalyticsStats.mixRangeTitle("week", date(2026, 9, 7)))
    }

    @Test
    fun `normalizeChapterId accepts integer-valued decimals like web`() {
        assertEquals(3, ActivityRecord.normalizeChapterId("3.0"))
        assertEquals(114, ActivityRecord.normalizeChapterId("114"))
        assertNull(ActivityRecord.normalizeChapterId("3.5"))
        assertNull(ActivityRecord.normalizeChapterId("0"))
        assertNull(ActivityRecord.normalizeChapterId("115"))
        assertNull(ActivityRecord.normalizeChapterId(""))
        assertNull(ActivityRecord.normalizeChapterId(null))
    }

    @Test
    fun `validateMinutes rounds decimals and caps at 600`() {
        val rounded = ActivityRecord.validateMinutes("14.6")
        assertEquals(true, rounded.valid)
        assertEquals(15, rounded.minutes)
        val capped = ActivityRecord.validateMinutes("601")
        assertEquals(true, capped.valid)
        assertEquals(600, capped.minutes)
        assertEquals(true, capped.capped)
    }
}
