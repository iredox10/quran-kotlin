package com.nur.quran.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsStatsTest {

    private fun dateStr(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    @Test
    fun `dayMinutes rounds like web Math-round`() {
        val d = dateStr(0)
        val sessions = listOf(AnalyticsStats.Session(d, 90))
        // 90s = 1.5min -> Math.round = 2 (truncation would give 1)
        assertEquals(2, AnalyticsStats.dayMinutes(sessions, d))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val dates = setOf(dateStr(0), dateStr(-1), dateStr(-2))
        assertEquals(3, AnalyticsStats.streak(dates))
    }

    @Test
    fun `streak tolerates missing today but requires yesterday`() {
        val dates = setOf(dateStr(-1), dateStr(-2))
        assertEquals(2, AnalyticsStats.streak(dates))
    }

    @Test
    fun `streak is zero after a gap`() {
        val dates = setOf(dateStr(0), dateStr(-2))
        assertEquals(1, AnalyticsStats.streak(dates))
    }

    @Test
    fun `achievements newest tier first and union surah count`() {
        val sessionIds = listOf(1, 2, 3)
        val recentIds = listOf(3, 4, 5)
        // union = 5 surahs -> Explorer; 500 mins -> 500 Minutes badge
        val badges = AnalyticsStats.achievements(
            streakDays = 7,
            allTimeMins = 500,
            sessionChapterIds = sessionIds,
            recentlyReadIds = recentIds
        )
        assertEquals(3, badges.size)
        // newest/highest tier first (web reverse().slice(0,3)):
        // [Explorer, 500 Minutes, 100 Minutes] — older tiers fall off.
        assertEquals("Explorer", badges[0].title)
        assertTrue(badges.any { it.title == "500 Minutes" })
    }

    @Test
    fun `weekly goal percent caps at 100`() {
        assertEquals(100, AnalyticsStats.weeklyGoalPercent(300, 180))
        assertEquals(50, AnalyticsStats.weeklyGoalPercent(90, 180))
    }

    @Test
    fun `heatmap levels match web thresholds`() {
        assertEquals(0, AnalyticsStats.heatmapLevel(false, 9999))
        assertEquals(1, AnalyticsStats.heatmapLevel(true, 5 * 60L))
        assertEquals(2, AnalyticsStats.heatmapLevel(true, 15 * 60L))
        assertEquals(3, AnalyticsStats.heatmapLevel(true, 45 * 60L))
    }

    @Test
    fun `smart insight empty state`() {
        assertEquals(
            "Start reading to unlock insights!",
            AnalyticsStats.smartInsight(emptyList())
        )
    }
}
