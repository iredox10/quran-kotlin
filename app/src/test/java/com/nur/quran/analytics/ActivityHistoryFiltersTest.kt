package com.nur.quran.analytics

import com.nur.quran.data.db.entities.ReadingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Web-parity tests for the Activity History filters — mirrors
 * quran-app/src/utils/historyFilter.test.js (reference truth).
 */
class ActivityHistoryFiltersTest {

    // 2026-10-03 is a Saturday (matches the web test's pinned date).
    private val now: Date = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, 3, 15, 0, 0)
    }.time

    private fun session(
        date: String,
        duration: Long,
        type: String = "reading",
        extraChapterId: Int? = null,
        extraTimestamp: Long = 0L,
    ) = ReadingSessionEntity(
        date = date,
        duration = duration,
        type = type,
        chapterId = extraChapterId,
        timestamp = if (extraTimestamp != 0L) extraTimestamp else ActivityHistoryFilters.sessionTime(
            ReadingSessionEntity(id = 0, date = date, duration = 0)
        ),
    )

    private val chapters = mapOf(
        1 to Pair("Al-Fatihah", "الفاتحة"),
        2 to Pair("Al-Baqarah", "البقرة"),
    )
    private val resolver: (Int?) -> Pair<String?, String?>? = { id ->
        chapters[id]?.let { it.first to it.second }
    }

    @Test
    fun normalizeQueryTrimsAndLowercases() {
        assertEquals("fatihah", ActivityHistoryFilters.normalizeQuery("  FatihAH "))
        assertEquals("", ActivityHistoryFilters.normalizeQuery("   "))
        assertEquals("", ActivityHistoryFilters.normalizeQuery(null))
    }

    @Test
    fun emptyQueryMatchesEverything() {
        val s = session("2026-10-01", 60)
        assertTrue(ActivityHistoryFilters.matchesQuery(s, ""))
        assertTrue(ActivityHistoryFilters.matchesQuery(s, "   "))
    }

    @Test
    fun matchesByTypeIdAndLabelCaseInsensitively() {
        val focus = session("2026-10-01", 60, type = "pomodoro")
        assertTrue(ActivityHistoryFilters.matchesQuery(focus, "pom", resolver))
        assertTrue(ActivityHistoryFilters.matchesQuery(focus, "focus", resolver))
        assertFalse(ActivityHistoryFilters.matchesQuery(focus, "reading", resolver))
    }

    @Test
    fun matchesBySurahNameSimpleAndArabic() {
        val s = session("2026-10-01", 60, extraChapterId = 1)
        assertTrue(ActivityHistoryFilters.matchesQuery(s, "fatihah", resolver))
        assertTrue(ActivityHistoryFilters.matchesQuery(s, "الفات", resolver))
        assertFalse(ActivityHistoryFilters.matchesQuery(s, "bakarah", resolver))
    }

    @Test
    fun matchesSurahIdWhenResolverMisses() {
        val s = session("2026-10-01", 60, extraChapterId = 7)
        assertTrue(ActivityHistoryFilters.matchesQuery(s, "7", resolver))
        assertTrue(ActivityHistoryFilters.matchesQuery(s, "surah 7", resolver))
        // no chapterId -> no surah match
        val blank = session("2026-10-01", 60)
        assertFalse(ActivityHistoryFilters.matchesQuery(blank, "surah 7", resolver))
    }

    @Test
    fun filterSessionsCombinesRangeAndQuery() {
        val sessions = listOf(
            session("2026-10-02", 60, extraChapterId = 1), // Friday
            session("2026-10-01", 30, type = "listening", extraChapterId = 2), // Thursday
            session("2026-09-20", 15), // last month
        )
        val week = ActivityHistoryFilters.filterSessions(sessions, ActivityHistoryFilters.HistoryRange.WEEK, now = now)
        assertEquals(2, week.size)

        val quizListening =
            ActivityHistoryFilters.filterSessions(sessions, ActivityHistoryFilters.HistoryRange.WEEK, query = "listening", now = now)
        assertEquals(1, quizListening.size)
        assertEquals("listening", quizListening.first().type)

        assertTrue(
            ActivityHistoryFilters.filterSessions(
                sessions, ActivityHistoryFilters.HistoryRange.WEEK, query = "quran-not-there", now = now
            ).isEmpty()
        )
        assertEquals(sessions.size, ActivityHistoryFilters.filterSessions(sessions, ActivityHistoryFilters.HistoryRange.ALL, now = now).size)
    }

    @Test
    fun rangeKeysForTodayYesterdayWeekMonth() {
        assertEquals(
            listOf("2026-10-03"),
            ActivityHistoryFilters.rangeKeys(ActivityHistoryFilters.HistoryRange.TODAY, now)
        )
        assertEquals(
            listOf("2026-10-02"),
            ActivityHistoryFilters.rangeKeys(ActivityHistoryFilters.HistoryRange.YESTERDAY, now)
        )
        // Sunday-start week containing Sat 2026-09-27..Fri 2026-10-03? No — Sun 2026-09-27 is before Sat 10-03:
        val week = ActivityHistoryFilters.rangeKeys(ActivityHistoryFilters.HistoryRange.WEEK, now)!!
        assertEquals(7, week.size)
        assertEquals("2026-09-27", week.first()) // Sunday
        assertEquals("2026-10-03", week.last())
        // October: 31 keys, Oct 1 first
        val month = ActivityHistoryFilters.rangeKeys(ActivityHistoryFilters.HistoryRange.MONTH, now)!!
        assertEquals(31, month.size)
        assertEquals("2026-10-01", month.first())
        assertNull(ActivityHistoryFilters.rangeKeys(ActivityHistoryFilters.HistoryRange.ALL, now))
    }

    @Test
    fun sessionTimePrefersTimestampFallsBackToDate() {
        assertEquals(777L, session("2026-10-01", 10, extraTimestamp = 777L).let { ActivityHistoryFilters.sessionTime(it) })
        val midnight = ActivityHistoryFilters.sessionTime(session("2026-10-01", 10))
        assertTrue(midnight > 0L)
    }

    @Test
    fun groupSessionsByDaySortsNewestDayFirstSessionsNewestWithin() {
        val groups = ActivityHistoryFilters.groupSessionsByDay(
            listOf(
                session("2026-10-02", 60, extraTimestamp = 100L),
                session("2026-10-02", 60, extraTimestamp = 300L),
                session("2026-09-28", 45, extraTimestamp = 5L),
            ),
            now = now,
        )
        assertEquals(listOf("2026-10-02", "2026-09-28"), groups.map { it.key })
        assertEquals(listOf("Yesterday", "Mon, Sep 28"), groups.map { it.label })
        assertEquals(300L, groups[0].sessions.first().timestamp)
    }
}
