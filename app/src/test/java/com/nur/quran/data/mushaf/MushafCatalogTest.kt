package com.nur.quran.data.mushaf

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MushafCatalogTest {
    @Test fun `every selectable edition is complete offline Hafs with canonical page navigation`() {
        assertEquals(setOf("madani-standard", "madani-tajweed", "uthmani-hafs"), Mushaf.SELECTABLE.map { it.id }.toSet())
        assertTrue(Mushaf.SELECTABLE.all { it.pageCount == 604 && it.renderMode == MushafRenderMode.UNICODE })
        assertTrue(Mushaf.SELECTABLE.all { it.defaultFontId in it.supportedFontIds })
        assertTrue(Mushaf.SELECTABLE.none { "uthman-taha-naskh" in it.supportedFontIds })
    }

    @Test fun `unsupported saved IndoPak choices migrate without deleting or guessing Quran signs`() {
        assertFalse(Mushaf.INDOPAK.isSelectable)
        assertNotNull(Mushaf.INDOPAK.unavailableReason)
        assertEquals(Mushaf.DEFAULT, Mushaf.forReading("indopak"))
        assertEquals("KFGQPC Hafs", Mushaf.compatibleFontName("indopak", "Scheherazade New"))
        val legacy = "الحمد\uE021"
        val qpc = "ٱلۡحَمۡدُ"
        assertEquals(qpc, wordTextForMushaf(Mushaf.INDOPAK, "ٱلْحَمْدُ", legacy, qpc))
    }

    @Test fun `Uthmani edition uses its real script and default rather than relabeling QPC`() {
        assertEquals(Mushaf.UTHMANI_HAFS, Mushaf.fromId("uthmani-hafs"))
        assertEquals("ٱلْعَـٰلَمِينَ", wordTextForMushaf(Mushaf.UTHMANI_HAFS, "ٱلْعَـٰلَمِينَ", "الۡعٰلَمِيۡنَ", "ٱلۡعَٰلَمِينَ"))
        assertEquals("Amiri Quran", Mushaf.compatibleFontName("uthmani-hafs", null))
        assertEquals("KFGQPC Hafs", Mushaf.compatibleFontName(null, null))
        assertEquals("KFGQPC Hafs", Mushaf.compatibleFontName("madani-standard", "Uthman Taha Naskh"))
    }

    @Test fun `Tajweed is the same complete Arabic text with colored rules not a fake glyph edition`() {
        assertEquals(5, Mushaf.MADANI_TAJWEED.apiMushafId)
        assertEquals(Mushaf.MADANI_STANDARD.verseField, Mushaf.MADANI_TAJWEED.verseField)
        assertTrue(Mushaf.isTajweedEffective(Mushaf.MADANI_TAJWEED, false))
        assertFalse(Mushaf.isTajweedEffective(Mushaf.MADANI_STANDARD, false))
        assertTrue(Mushaf.isTajweedEffective(Mushaf.UTHMANI_HAFS, true))
    }

    @Test fun `bundled samples for every available edition avoid private-use glyphs and retain Arabic`() {
        for (chapter in listOf(1, 2, 6, 9, 18, 112, 114)) {
            val verses = JsonParser.parseString(File("src/main/assets/data/words/$chapter.json").readText()).asJsonObject["verses"].asJsonArray
            for (verse in verses) for (word in verse.asJsonObject["words"].asJsonArray) {
                val obj = word.asJsonObject
                fun field(key: String) = obj[key]?.takeUnless { it.isJsonNull }?.asString
                for (mushaf in Mushaf.SELECTABLE) {
                    val text = wordTextForMushaf(mushaf, field("text_uthmani"), field("text_indopak"), field("text_qpc_hafs"))
                    assertTrue("Blank word for ${mushaf.id}", text.isNotBlank())
                    assertFalse("Unsupported private-use glyph for ${mushaf.id}", text.any { it.code in 0xE000..0xF8FF })
                }
            }
        }
    }
}
