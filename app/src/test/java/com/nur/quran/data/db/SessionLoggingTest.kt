package com.nur.quran.data.db

import com.nur.quran.data.db.entities.ReadingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Web parity (Memorization.jsx unmount guard + useAppStore.logReadingSession):
 * sessions < 10s are dropped, the date key is the UTC calendar day, and the
 * persisted entity carries duration seconds, type 'memorizing', chapterId.
 */
class SessionLoggingTest {

    @Test
    fun `sessions under 10 seconds are not loggable`() {
        assertFalse(isLoggableReadingSession(0L))
        assertFalse(isLoggableReadingSession(9L))
        assertFalse(isLoggableReadingSession(-5L))
    }

    @Test
    fun `sessions of 10 seconds or more are loggable`() {
        assertTrue(isLoggableReadingSession(10L))
        assertTrue(isLoggableReadingSession(11L))
        assertTrue(isLoggableReadingSession(300L))
    }

    @Test
    fun `utcDateKey matches UTC calendar day not device-local day`() {
        // 2025-12-31T23:59:59Z is already 2026-01-01 in +xx zones: UTC key stays 12-31.
        assertEquals("2025-12-31", utcDateKey(1767225599000L))
        assertEquals("2026-01-01", utcDateKey(1767225600000L))
        assertEquals("2026-09-15", utcDateKey(1789430400000L))
    }

    @Test
    fun `utcDateKey matches yyyy-MM-dd format`() {
        assertTrue(utcDateKey().matches(Regex("""^\d{4}-\d{2}-\d{2}$""")))
    }

    @Test
    fun `memorizing exit maps to ReadingSessionEntity with web field semantics`() {
        val durationSeconds = 95L
        val chapterId = 67
        val entity = ReadingSessionEntity(
            date = utcDateKey(1789430400000L),
            duration = durationSeconds,
            type = "memorizing",
            chapterId = chapterId,
            timestamp = 1789430495000L
        )
        assertTrue(isLoggableReadingSession(entity.duration))
        assertEquals("2026-09-15", entity.date)
        assertEquals(95L, entity.duration)
        assertEquals("memorizing", entity.type)
        assertEquals(67, entity.chapterId)
    }
}
