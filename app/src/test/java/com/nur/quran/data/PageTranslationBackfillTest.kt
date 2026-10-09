package com.nur.quran.data

import com.nur.quran.data.db.entities.VerseEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PageTranslationBackfillTest {

    private fun verse(id: Int, key: String, translation: String?) = VerseEntity(
        id, 1, id, key, "نص", null, null, 1, 1, translation
    )

    @Test
    fun `one batched fetch for N blank verses`() = runBlocking {
        val verses = (1..12).map { verse(it, "2:$it", null) }
        var calls = 0
        val out = TranslationFallback.backfillPageTranslations(
            verses, false,
            fetchPacked = { keys -> calls++; assertEquals(12, keys.size); emptyMap() },
            offlineByKey = { "off-$it" }
        )
        assertEquals(1, calls)
        assertEquals("off-2:1", out.first().translation)
    }

    @Test
    fun `no fetch when all translations present`() = runBlocking {
        val verses = (1..8).map { verse(it, "3:$it", "kept") }
        var calls = 0
        val out = TranslationFallback.backfillPageTranslations(
            verses, false,
            fetchPacked = { calls++; emptyMap() },
            offlineByKey = { "off" }
        )
        assertEquals(0, calls)
        assertEquals(listOf("kept"), out.map { it.translation }.distinct())
    }

    @Test
    fun `packed wins then offline then blank kept`() = runBlocking {
        val verses = listOf(verse(1, "4:1", null), verse(2, "4:2", ""), verse(3, "4:3", "keep"))
        val out = TranslationFallback.backfillPageTranslations(
            verses, false,
            fetchPacked = { mapOf("4:1" to "packed") },
            offlineByKey = { if (it == "4:2") "offline" else null }
        )
        assertEquals(listOf("packed", "offline", "keep"), out.map { it.translation })
    }
}
