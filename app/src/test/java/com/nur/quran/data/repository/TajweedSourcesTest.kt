package com.nur.quran.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.api.ApiTajweedVerse
import com.nur.quran.utils.TajweedProcessor
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class TajweedSourcesTest {
    private val bundled: List<QuranRepository.OfflineVerseItem> = Gson().fromJson(
        File("src/main/assets/data/quran_full.json").readText(),
        object : TypeToken<List<QuranRepository.OfflineVerseItem>>() {}.type,
    )
    private fun rows(chapter: Int) = bundled.filter { it.verse_key.startsWith("$chapter:") }
        .map { ApiTajweedVerse(it.id,it.verse_key,it.text_uthmani_tajweed!!) }

    @Test fun `reported ayahs have the same full verse rules on a cold open as after cache fill`() {
        for (key in listOf("4:2","4:3","9:3")) {
            val chapter=key.substringBefore(":").toInt()
            val source=rows(chapter)
            val first=tajweedMapForKeys(setOf(key),completeTajweedRows(chapter,source,emptyList()))
            val cached=tajweedMapForKeys(setOf(key),completeTajweedRows(chapter,source,source))
            assertEquals(cached,first)
            assertEquals(TajweedProcessor.sanitizeTajweedHtml(source.first { it.verse_key==key }.text_uthmani_tajweed),first[key])
            assertTrue(first[key]!!.contains("<tajweed"))
        }
    }

    @Test fun `a previous surah or partial cache cannot remove the new surah's local rules`() {
        val chapter=rows(9)
        val result=completeTajweedRows(9,chapter,rows(4)+chapter.take(1))
        assertEquals(chapter.size,result.size)
        assertTrue(result.all { it.verse_key.startsWith("9:") })
        assertEquals(setOf("9:3"),tajweedMapForKeys(setOf("9:3"),result).keys)
    }

    @Test fun `every shipped ayah has complete first paint markup without a network request`() {
        assertEquals(6236,bundled.size)
        assertTrue(bundled.all { !it.text_uthmani_tajweed.isNullOrBlank() })
        for (chapter in 1..114) assertEquals(rows(chapter).size,completeTajweedRows(chapter,rows(chapter),emptyList()).size)
    }
}
