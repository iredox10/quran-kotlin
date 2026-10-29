package com.nur.quran.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.api.TafsirResponse
import java.io.File
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class OfflineTafsirLoaderTest {
    private val bundled = listOf(ApiTafsirVerse("6:76", "Bundled explanation", 169), ApiTafsirVerse("6:77", "Next ayah", 169))

    @Test fun `bundled commentary returns without waiting for an unavailable network`() = runBlocking {
        val result = withTimeout(1000) {
            loadTafsirOfflineFirst(6, emptyList(), bundled, { awaitCancellation() }, { error("Must not cache fallback as selected source") })
        }
        assertEquals(bundled, result.tafsirs)
    }

    @Test fun `downloaded edition wins and missing ayahs use accurately identified local fallback`() = runBlocking {
        val cached = listOf(ApiTafsirVerse("6:76", "Downloaded explanation", 168), ApiTafsirVerse("5:1", "Wrong chapter", 168))
        val result = loadTafsirOfflineFirst(6, cached, bundled, { error("Unexpected HTTP") }, { error("Unexpected write") })
        assertEquals(listOf("6:76", "6:77"), result.tafsirs.map { it.verse_key })
        assertEquals(listOf(168,169), result.tafsirs.map { it.resource_id })
        assertEquals("Downloaded explanation", result.tafsirs.first().text)
    }

    @Test fun `corrupt cached rows cannot hide the offline explanation`() = runBlocking {
        val result = loadTafsirOfflineFirst(6, listOf(ApiTafsirVerse("6:76", " ", 168)), bundled,
            { error("Unexpected HTTP") }, { error("Unexpected write") })
        assertEquals(bundled, result.tafsirs)
    }

    @Test fun `network is used and saved only when no local commentary exists`() = runBlocking {
        var saved: TafsirResponse? = null
        val response = TafsirResponse(bundled)
        assertEquals(response, loadTafsirOfflineFirst(6, emptyList(), emptyList(), { response }, { saved = it }))
        assertEquals(response, saved)
    }

    @Test fun `grouped commentary never carries across chapters or editions`() {
        val rows = listOf(ApiTafsirVerse("1:7", "Chapter one",169), ApiTafsirVerse("2:1", "",169),
            ApiTafsirVerse("1:7", "",168))
        val result = completeBundledTafsir(rows)
        assertEquals("", result.first { it.verse_key == "2:1" }.text)
        assertEquals("", result.first { it.resource_id == 168 }.text)
    }

    @Test fun `every Quran ayah has bundled offline tafsir`() = runBlocking {
        val rawTafsir: List<ApiTafsirVerse> = Gson().fromJson(File("src/main/assets/data/tafsir_ibn_kathir.json").readText(),
            object : TypeToken<List<ApiTafsirVerse>>() {}.type)
        val supplements: List<ApiTafsirVerse> = Gson().fromJson(File("src/main/assets/data/tafsir_ibn_kathir_supplement.json").readText(),
            object : TypeToken<List<ApiTafsirVerse>>() {}.type)
        val tafsir = completeBundledTafsir(rawTafsir + supplements)
        assertEquals(rawTafsir.first { it.verse_key == "2:8" }.text, tafsir.first { it.verse_key == "2:9" }.text)
        assertEquals(rawTafsir.first { it.verse_key == "112:1" }.text, tafsir.first { it.verse_key == "112:4" }.text)
        assertTrue(tafsir.first { it.verse_key == "105:5" }.text.contains("offline summary"))
        val verses: List<QuranRepository.OfflineVerseItem> = Gson().fromJson(File("src/main/assets/data/quran_full.json").readText(),
            object : TypeToken<List<QuranRepository.OfflineVerseItem>>() {}.type)
        assertEquals(6236, tafsir.size)
        assertEquals(verses.map { it.verse_key }.toSet(), tafsir.map { it.verse_key }.toSet())
        assertTrue(tafsir.all { it.text.isNotBlank() && it.resource_id == 169 })
        for (chapter in 1..114) {
            val rows = tafsir.filter { it.verse_key.substringBefore(':').toInt() == chapter }
            val result = withTimeout(1000) {
                loadTafsirOfflineFirst(chapter, emptyList(), rows, { awaitCancellation() }, { error("Unexpected write") })
            }
            assertEquals(rows.size, result.tafsirs.size)
        }
    }
}
