package com.nur.quran.analytics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsFlowStatsTest {

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    // Wednesday 2026-10-07 (mid-week, mid-month) pins calendar expectations.
    private val wed = fmt.parse("2026-10-07")!!

    @Test
    fun `rangeKeys today is single day`() {
        assertEquals(listOf("2026-10-07"), AnalyticsStats.rangeKeys(AnalyticsStats.FlowRange.TODAY, wed))
    }

    @Test
    fun `rangeKeys week starts Sunday with 7 days`() {
        val keys = AnalyticsStats.rangeKeys(AnalyticsStats.FlowRange.WEEK, wed)
        assertEquals(listOf("2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07",
            "2026-10-08", "2026-10-09", "2026-10-10"), keys)
    }

    @Test
    fun `rangeKeys month is full calendar month`() {
        val keys = AnalyticsStats.rangeKeys(AnalyticsStats.FlowRange.MONTH, wed)
        assertEquals(31, keys.size)
        assertEquals("2026-10-01", keys.first())
        assertEquals("2026-10-31", keys.last())
    }

    @Test
    fun `previousRangeKeys matches length ending day before`() {
        val prev = AnalyticsStats.previousRangeKeys(AnalyticsStats.FlowRange.WEEK, wed)
        assertEquals(7, prev.size)
        assertEquals("2026-09-27", prev.first())
        assertEquals("2026-10-03", prev.last())
    }

    @Test
    fun `filterByRange keeps only in-range sessions`() {
        val sessions = listOf(
            AnalyticsStats.Session("2026-10-07", 60),
            AnalyticsStats.Session("2026-09-01", 60)
        )
        val out = AnalyticsStats.filterByRange(sessions, AnalyticsStats.FlowRange.TODAY, wed)
        assertEquals(1, out.size)
    }

    @Test
    fun `bucketByDay rounds seconds to minutes per key`() {
        val sessions = listOf(
            AnalyticsStats.Session("2026-10-06", 90),
            AnalyticsStats.Session("2026-10-06", 90)
        )
        val buckets = AnalyticsStats.bucketByDay(sessions, listOf("2026-10-06", "2026-10-07"))
        assertEquals(3, buckets[0].minutes)
        assertEquals(0, buckets[1].minutes)
        assertEquals("Tue", buckets[0].label)
    }

    @Test
    fun `bucketByHour fills 24 local-hour buckets`() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 7, 21, 30, 0) }
        val sessions = listOf(
            AnalyticsStats.Session("2026-10-07", 600, timestamp = cal.timeInMillis)
        )
        val buckets = AnalyticsStats.bucketByHour(sessions, "2026-10-07")
        assertEquals(24, buckets.size)
        assertEquals(10, buckets[21].minutes)
        assertEquals(0, buckets[20].minutes)
    }

}
