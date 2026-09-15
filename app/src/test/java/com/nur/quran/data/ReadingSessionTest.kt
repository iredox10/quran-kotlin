package com.nur.quran.data

import com.nur.quran.analytics.AnalyticsStats
import com.nur.quran.data.db.entities.ReadingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReadingSessionTest {

    companion object {
        private const val MIN_SESSION_DURATION_SECONDS = 10L
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private val DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")
    }

    private fun isSessionEligible(durationSeconds: Long): Boolean =
        durationSeconds >= MIN_SESSION_DURATION_SECONDS

    // ========================================================================
    // 1. Test filtering sessions < 10 seconds
    // ========================================================================

    @Test
    fun `sessions under 10 seconds are filtered out`() {
        assertFalse("0 second session must be filtered out", isSessionEligible(0L))
        assertFalse("1 second session must be filtered out", isSessionEligible(1L))
        assertFalse("5 second session must be filtered out", isSessionEligible(5L))
        assertFalse("8 second session must be filtered out", isSessionEligible(8L))
        assertFalse("9 second session (just below threshold) must be filtered out", isSessionEligible(9L))
    }

    @Test
    fun `sessions of 10 seconds or greater are retained`() {
        assertTrue("10 second session (exact threshold) must be retained", isSessionEligible(10L))
        assertTrue("11 second session must be retained", isSessionEligible(11L))
        assertTrue("30 second checkpoint must be retained", isSessionEligible(30L))
        assertTrue("60 second session must be retained", isSessionEligible(60L))
        assertTrue("300 second session must be retained", isSessionEligible(300L))
    }

    @Test
    fun `negative or invalid durations are filtered out`() {
        assertFalse("Negative duration must be filtered out", isSessionEligible(-1L))
        assertFalse("Large negative duration must be filtered out", isSessionEligible(-100L))
    }

    @Test
    fun `filter list of ReadingSessionEntity retains only sessions with duration at least 10 seconds`() {
        val sessions = listOf(
            ReadingSessionEntity(id = 1, date = "2026-09-15", duration = 0L),
            ReadingSessionEntity(id = 2, date = "2026-09-15", duration = 5L),
            ReadingSessionEntity(id = 3, date = "2026-09-15", duration = 9L),
            ReadingSessionEntity(id = 4, date = "2026-09-15", duration = 10L),
            ReadingSessionEntity(id = 5, date = "2026-09-15", duration = 25L),
            ReadingSessionEntity(id = 6, date = "2026-09-15", duration = 120L)
        )

        val filtered = sessions.filter { it.duration >= MIN_SESSION_DURATION_SECONDS }

        assertEquals(3, filtered.size)
        assertEquals(listOf(4L, 5L, 6L), filtered.map { it.id })
        assertTrue(filtered.all { it.duration >= 10L })
    }

    @Test
    fun `filter list where all sessions are under 10 seconds produces empty list`() {
        val shortSessions = listOf(
            ReadingSessionEntity(id = 1, date = "2026-09-15", duration = 2L),
            ReadingSessionEntity(id = 2, date = "2026-09-15", duration = 4L),
            ReadingSessionEntity(id = 3, date = "2026-09-15", duration = 9L)
        )

        val filtered = shortSessions.filter { it.duration >= MIN_SESSION_DURATION_SECONDS }
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `filter list where all sessions are 10 seconds or more retains entire list`() {
        val validSessions = listOf(
            ReadingSessionEntity(id = 1, date = "2026-09-15", duration = 10L),
            ReadingSessionEntity(id = 2, date = "2026-09-15", duration = 60L),
            ReadingSessionEntity(id = 3, date = "2026-09-15", duration = 300L)
        )

        val filtered = validSessions.filter { it.duration >= MIN_SESSION_DURATION_SECONDS }
        assertEquals(validSessions.size, filtered.size)
    }

    // ========================================================================
    // 2. Test session date formatting ("yyyy-MM-dd")
    // ========================================================================

    @Test
    fun `session date matches yyyy-MM-dd format`() {
        val testDate = "2026-09-15"
        assertTrue(testDate.matches(DATE_REGEX))
    }

    @Test
    fun `SimpleDateFormat formats date as yyyy-MM-dd`() {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15)
        }
        val formatted = sdf.format(cal.time)
        assertEquals("2026-09-15", formatted)
        assertTrue(formatted.matches(DATE_REGEX))
    }

    @Test
    fun `session date formats single digit months and days with leading zeros`() {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 5)
        }
        val formatted = sdf.format(cal.time)
        assertEquals("2026-01-05", formatted)
        assertTrue(formatted.matches(DATE_REGEX))
    }

    @Test
    fun `LocalDate toString matches yyyy-MM-dd format`() {
        val localDate = LocalDate.of(2026, 9, 15)
        assertEquals("2026-09-15", localDate.toString())
        assertTrue(localDate.toString().matches(DATE_REGEX))

        val currentLocalDate = LocalDate.now().toString()
        assertTrue(currentLocalDate.matches(DATE_REGEX))
    }

    @Test
    fun `session date parsing and roundtrip formatting preserves yyyy-MM-dd`() {
        val originalDateStr = "2026-09-15"
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        val parsedDate: Date? = sdf.parse(originalDateStr)

        assertNotNull(parsedDate)
        val reformatted = sdf.format(parsedDate!!)
        assertEquals(originalDateStr, reformatted)
    }

    @Test
    fun `ReadingSessionEntity preserves yyyy-MM-dd date property`() {
        val session = ReadingSessionEntity(
            id = 1L,
            date = "2026-09-15",
            duration = 120L,
            type = "reading",
            chapterId = 1
        )
        assertEquals("2026-09-15", session.date)
        assertTrue(session.date.matches(DATE_REGEX))
    }

    @Test
    fun `grouping sessions by yyyy-MM-dd date aggregates correctly`() {
        val sessions = listOf(
            ReadingSessionEntity(date = "2026-09-14", duration = 60L),
            ReadingSessionEntity(date = "2026-09-14", duration = 120L),
            ReadingSessionEntity(date = "2026-09-15", duration = 180L)
        )

        val grouped = sessions.groupBy { it.date }
        assertEquals(2, grouped.size)
        assertEquals(180L, grouped["2026-09-14"]?.sumOf { it.duration })
        assertEquals(180L, grouped["2026-09-15"]?.sumOf { it.duration })
    }

    // ========================================================================
    // 3. Test session type ("reading", "memorizing")
    // ========================================================================

    @Test
    fun `ReadingSessionEntity default type is reading`() {
        val session = ReadingSessionEntity(
            date = "2026-09-15",
            duration = 60L
        )
        assertEquals("reading", session.type)
    }

    @Test
    fun `AnalyticsStats Session default type is reading`() {
        val session = AnalyticsStats.Session(
            date = "2026-09-15",
            durationSec = 60L
        )
        assertEquals("reading", session.type)
    }

    @Test
    fun `ReadingSessionEntity accepts reading type explicitly`() {
        val session = ReadingSessionEntity(
            date = "2026-09-15",
            duration = 60L,
            type = "reading",
            chapterId = 2
        )
        assertEquals("reading", session.type)
    }

    @Test
    fun `ReadingSessionEntity accepts memorizing type explicitly`() {
        val session = ReadingSessionEntity(
            date = "2026-09-15",
            duration = 180L,
            type = "memorizing",
            chapterId = 36
        )
        assertEquals("memorizing", session.type)
    }

    @Test
    fun `filter sessions separates reading and memorizing types`() {
        val sessions = listOf(
            ReadingSessionEntity(id = 1, date = "2026-09-15", duration = 60L, type = "reading"),
            ReadingSessionEntity(id = 2, date = "2026-09-15", duration = 120L, type = "memorizing"),
            ReadingSessionEntity(id = 3, date = "2026-09-15", duration = 180L, type = "reading"),
            ReadingSessionEntity(id = 4, date = "2026-09-15", duration = 240L, type = "memorizing")
        )

        val readingSessions = sessions.filter { it.type == "reading" }
        val memorizingSessions = sessions.filter { it.type == "memorizing" }

        assertEquals(2, readingSessions.size)
        assertEquals(2, memorizingSessions.size)
        assertEquals(240L, readingSessions.sumOf { it.duration })
        assertEquals(360L, memorizingSessions.sumOf { it.duration })
    }

    @Test
    fun `AnalyticsStats minutesByType aggregates reading and memorizing correctly`() {
        val sessions = listOf(
            // 90s reading = 1.5 min -> Math.round = 2 min
            AnalyticsStats.Session(date = "2026-09-15", durationSec = 90L, type = "reading"),
            // 150s reading = 2.5 min -> Math.round = 3 min (total reading: 240s = 4.0 min -> 4)
            AnalyticsStats.Session(date = "2026-09-15", durationSec = 150L, type = "reading"),
            // 300s memorizing = 5.0 min -> Math.round = 5 min
            AnalyticsStats.Session(date = "2026-09-15", durationSec = 300L, type = "memorizing")
        )

        val minutes = AnalyticsStats.minutesByType(sessions)

        assertEquals(4, minutes["reading"])
        assertEquals(5, minutes["memorizing"])
        assertEquals(0, minutes["focus"])
        assertEquals(0, minutes["listening"])
    }

    @Test
    fun `AnalyticsStats minutesByType treats empty type as reading for backwards compatibility`() {
        val sessions = listOf(
            AnalyticsStats.Session(date = "2026-09-15", durationSec = 120L, type = "")
        )

        val minutes = AnalyticsStats.minutesByType(sessions)
        assertEquals(2, minutes["reading"])
    }

    @Test
    fun `conversion from ReadingSessionEntity to AnalyticsStats Session preserves type and fields`() {
        val entity = ReadingSessionEntity(
            id = 42L,
            date = "2026-09-15",
            duration = 180L,
            type = "memorizing",
            chapterId = 67,
            timestamp = 1000L
        )

        val statSession = AnalyticsStats.Session(
            date = entity.date,
            durationSec = entity.duration,
            type = entity.type,
            chapterId = entity.chapterId,
            timestamp = entity.timestamp
        )

        assertEquals(entity.date, statSession.date)
        assertEquals(entity.duration, statSession.durationSec)
        assertEquals(entity.type, statSession.type)
        assertEquals(entity.chapterId, statSession.chapterId)
        assertEquals(entity.timestamp, statSession.timestamp)
    }
}
