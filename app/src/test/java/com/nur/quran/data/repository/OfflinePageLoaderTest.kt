package com.nur.quran.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.HIZB_STARTS
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.TranslationFallback
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class OfflinePageLoaderTest {
    private val empty = LocalPageContent(emptyList(), emptyList())
    private fun verse(id: Int, key: String) = VerseEntity(
        id, key.substringBefore(":").toInt(), key.substringAfter(":").toInt(),
        key, "نص", "نص", "نص", 604, 30, "Bundled translation"
    )
    private fun word(verse: VerseEntity, position: Int = 1) = WordEntity(
        verse.id * 100 + position, verse.id, position, "نص", "نص", "نص", null,
        "word meaning", "word", "word"
    )
    private val lastPage = listOf(verse(6222, "112:1"), verse(6226, "113:1"), verse(6231, "114:1"))
        .let { LocalPageContent(it, it.map(::word)) }

    @Test fun `cold page returns bundled content even if network would never respond`() = runBlocking {
        var writes = 0
        val result = withTimeout(1000) {
            loadPageOfflineFirst(empty, { lastPage }, { writes++ }, { awaitCancellation() })
        }
        assertEquals(lastPage, result)
        assertEquals(1, writes)
    }

    @Test fun `complete local page needs neither a write nor a network request`() = runBlocking {
        val result = loadPageOfflineFirst(lastPage, { lastPage }, { error("Unexpected write") }, { error("Unexpected HTTP") })
        assertEquals(lastPage, result)
    }

    @Test fun `partial cross chapter cache is completed and saved in Quran order`() = runBlocking {
        val saved = lastPage.verses.last().copy(translation = "Downloaded edition")
        val partial = LocalPageContent(listOf(saved), listOf(word(saved).copy(translation = null, transliteration = null)))
        var persisted: LocalPageContent? = null
        val result = loadPageOfflineFirst(partial, { lastPage }, { persisted = it }, { error("Unexpected HTTP") })
        assertEquals(listOf("112:1", "113:1", "114:1"), result.verses.map { it.verseKey })
        assertEquals("Downloaded edition", result.verses.last().translation)
        assertEquals(lastPage.words, result.words)
        assertEquals(result, persisted)
        val selected = TranslationFallback.mergePageTranslations(result.verses,
            mapOf("112:1" to "Selected edition"), { null }, true)
        assertEquals("Selected edition", selected.first().translation)
    }

    @Test fun `blank scripts and incomplete word rows are repaired locally`() = runBlocking {
        val source = lastPage.copy(words = (lastPage.words + word(lastPage.verses.first(), 2))
            .sortedWith(compareBy<WordEntity> { it.verseId }.thenBy { it.position }))
        val broken = lastPage.copy(verses = lastPage.verses.map { it.copy(textUthmani = "", textQpcHafs = null) })
        val result = loadPageOfflineFirst(broken, { source }, {}, { error("Unexpected HTTP") })
        assertEquals(source, result)
    }

    @Test fun `network fallback is used only when no usable local content exists`() = runBlocking {
        var writes = 0
        val result = loadPageOfflineFirst(empty, { empty }, { writes++ }, { lastPage })
        assertEquals(lastPage, result)
        assertEquals(1, writes)
    }

    @Test fun `cancelling fallback propagates without persisting stale data`() = runBlocking {
        try {
            loadPageOfflineFirst(empty, { empty }, { error("Unexpected write") }, { throw CancellationException("Superseded") })
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("Superseded", expected.message)
        }
    }

    @Test fun `all 604 pages and every juz and hizb entry can load with an unavailable network`() = runBlocking {
        val rows: List<QuranRepository.OfflineVerseItem> = Gson().fromJson(
            File("src/main/assets/data/quran_full.json").readText(),
            object : TypeToken<List<QuranRepository.OfflineVerseItem>>() {}.type,
        )
        assertEquals(6236, rows.size)
        val pages = rows.groupBy { it.page_number }
        assertEquals((1..604).toSet(), pages.keys)
        withTimeout(5000) {
            for (page in 1..604) {
                val bundled = LocalPageContent(pages.getValue(page).map { row ->
                    verse(row.id, row.verse_key).copy(textUthmani = row.text_uthmani, pageNumber = page)
                }, emptyList())
                val result = loadPageOfflineFirst(empty, { bundled }, {}, { awaitCancellation() })
                assertEquals(bundled.verses.size, result.verses.size)
                assertTrue(result.verses.all { !it.textUthmani.isNullOrBlank() })
            }
        }
        for (division in JUZ_STARTS + HIZB_STARTS) {
            assertTrue("Missing division ${division.verseKey}", pages.getValue(division.pageNumber).any { it.verse_key == division.verseKey })
        }
        assertEquals((25..29).map { "2:$it" }, pages.getValue(5).map { it.verse_key })
    }
}
