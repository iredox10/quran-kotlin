package com.nur.quran.data.mushaf

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class IndopakNavigationTest {
    @Test fun originalPrintedEditionIndexContainsEverySurahInPageOrder() {
        val root = sequenceOf(File("src/main/assets/data/mushafs"), File("app/src/main/assets/data/mushafs")).first { it.isDirectory }
        val rows: List<IndopakSurah> = File(root, "indopak_navigation.json").reader().use {
            Gson().fromJson(it, object : TypeToken<List<IndopakSurah>>() {}.type)
        }
        assertEquals((1..114).toList(), rows.map { it.surah })
        assertTrue(rows.all { it.page in 2..848 && it.name.isNotBlank() })
        assertEquals(rows.map { it.page }.sorted(), rows.map { it.page })
        assertEquals(2, rows.first().page)
        assertEquals(3, rows[1].page)
        assertEquals(848, rows.last().page)
    }
}
