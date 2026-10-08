package com.nur.quran.data

import com.nur.quran.data.audio.ListeningSessionTracker
import com.nur.quran.data.hifdh.toggleMemorizedSurah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemzStoreTest {

    @Test
    fun `toggleMemorizedSurah adds surah id and full ayah range`() {
        val result = toggleMemorizedSurah(emptySet(), emptySet(), 112, 4)
        assertEquals(setOf(112), result.surahs)
        assertEquals(setOf("112:1", "112:2", "112:3", "112:4"), result.ayahs)
    }

    @Test
    fun `toggleMemorizedSurah removes surah id and full ayah range`() {
        val ayahs = setOf("112:1", "112:2", "112:3", "112:4", "113:1")
        val result = toggleMemorizedSurah(setOf(112), ayahs, 112, 4)
        assertTrue(result.surahs.isEmpty())
        assertEquals(setOf("113:1"), result.ayahs)
    }

    @Test
    fun `toggleMemorizedSurah without totalVerses only flips surah id`() {
        val result = toggleMemorizedSurah(emptySet(), setOf("112:1"), 112, null)
        assertEquals(setOf(112), result.surahs)
        assertEquals(setOf("112:1"), result.ayahs)
    }

    @Test
    fun `listening tracker drops segments under 10s`() {
        val tracker = ListeningSessionTracker()
        assertNull(tracker.onPlayingChanged(true, 2, 0L))
        assertNull(tracker.onPlayingChanged(false, 2, 9_000L))
    }

    @Test
    fun `listening tracker emits segment at 10s with chapter`() {
        val tracker = ListeningSessionTracker()
        tracker.onPlayingChanged(true, 2, 0L)
        val segment = tracker.onPlayingChanged(false, 2, 61_000L)
        assertEquals(61L, segment?.durationSeconds)
        assertEquals(2, segment?.chapterId)
    }
}
