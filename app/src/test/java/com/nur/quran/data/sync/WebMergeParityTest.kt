package com.nur.quran.data.sync

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Web truth syncMerge.js `mergeStateInto`. These are the rules where the Kotlin merge
 * already has web-parity behaviour on a web-shaped payload: scalars are last-write-wins,
 * keyed maps deep-merge (so both devices' keys survive) and primitive arrays union.
 */
class WebMergeParityTest {

    private val service = syncRig().service
    private val web = webState()

    /** What an Android device pushes: the flattened section spellings, plus its own rows. */
    private fun androidState(): Map<String, Any?> = mapOf(
        "version" to 1,
        "updatedAt" to 1778200000000L,
        "theme" to "light",
        "translationId" to 131,
        "reciterId" to 3,
        "arabicFont" to "Amiri Quran",
        "bookmarksJson" to Gson().toJson(
            listOf(mapOf("verseKey" to "1:1", "chapterId" to 1, "surahName" to "Al-Fatihah", "timestamp" to 11L)),
        ),
        "hifdhJson" to Gson().toJson(
            mapOf(
                "history" to mapOf("1:1" to mapOf("strength" to "medium", "lastReviewed" to 100L)),
                "goals" to listOf(mapOf("id" to 99L, "targetType" to "surah", "targetId" to 2)),
                "memorizedAyahs" to listOf("1:1"),
            ),
        ),
        "readingStatsJson" to Gson().toJson(
            mapOf(
                "sessions" to listOf(mapOf("date" to "2026-05-04", "duration" to 600L, "type" to "reading", "timestamp" to 1778140000L)),
                "recentlyRead" to listOf(mapOf("chapterId" to 1, "chapterName" to "Al-Fatihah", "verseKey" to "1:1", "timestamp" to 1778150000L)),
            ),
        ),
    )

    @Test
    fun `shared scalars take the web value and keep the rest`() {
        val merged = service.mergeStateInto(androidState(), web)
        assertEquals("dark", merged["theme"])
        assertEquals(131.0, numberOf(merged["translationId"]))
        assertEquals(3.0, numberOf(merged["reciterId"]))
        assertEquals("Amiri Quran", merged["arabicFont"])
        assertTrue(merged.containsKey("updatedAt"))
        assertTrue(merged.containsKey("bookmarksJson"))
    }

    @Test
    fun `android sections survive next to the web spellings`() {
        val merged = service.mergeStateInto(androidState(), web)
        listOf("bookmarksJson", "hifdhJson", "readingStatsJson")
            .forEach { field -> assertEquals(androidState()[field], merged[field]) }
        listOf("bookmarks", "hifdhHistory", "readingSessions")
            .forEach { field -> assertTrue(merged.containsKey(field)) }
    }
}
