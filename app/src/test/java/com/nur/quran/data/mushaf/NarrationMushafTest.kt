package com.nur.quran.data.mushaf

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NarrationMushafTest {
    @Test fun bothNarrationsAreCompleteWithTheirOwnNumberingAndPageRanges() {
        val root = sequenceOf(File("src/main/assets/data/mushafs"), File("app/src/main/assets/data/mushafs")).first { it.isDirectory }
        for (edition in NarrationMushaf.entries) {
            val type = object : TypeToken<List<NarrationAyah>>() {}.type
            val rows: List<NarrationAyah> = File(root, "${edition.id}.json").reader().use { Gson().fromJson(it, type) }
            assertEquals(6214, rows.size)
            assertEquals(6214, rows.map { it.id }.toSet().size)
            assertEquals((1..114).toSet(), rows.map { it.surah }.toSet())
            assertEquals((1..604).toSet(), rows.flatMap { it.pages }.toSet())
            rows.groupBy { it.surah }.forEach { (_, chapter) ->
                assertEquals((1..chapter.size).toList(), chapter.map { it.number })
                assertTrue(chapter.all { it.text.isNotBlank() && it.pages.isNotEmpty() })
            }
            // These are native Madani counts: Fatiha starts with Al-Hamd, not a Hafs-key substitution.
            assertTrue(rows.first().text.contains("حَمْدُ"))
            assertTrue(rows.first { it.surah == 1 && it.number == 3 }.text.contains("مَلِكِ"))
            assertEquals(listOf(2, 3), NarrationAyah(0, 1, "", 1, "", "2-3").pages)
        }
    }
}
