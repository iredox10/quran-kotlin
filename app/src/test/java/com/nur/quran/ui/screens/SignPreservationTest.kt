package com.nur.quran.ui.screens

import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sign-completeness (web parity): every Quranic annotation sign — saktah
 * (U+06DF), imalah (U+06EA), ishmam (U+06EB), filled stop (U+06EC), sajda
 * (U+06DE), small high noon/meem (U+06E0/U+06E2) — must survive the display
 * pipeline on every font. Only the U+25CC dotted-circle placeholder is
 * stripped; U+06DD is additionally stripped in embedded (KFGQPC Hafs) mode
 * because that font shapes bare Arabic-Indic digits into one numbered
 * medallion via GSUB.
 */
class SignPreservationTest {
    private val signs = "\u06df\u06ea\u06eb\u06ec\u06de\u06e0\u06e2"

    private fun word(id: Int, text: String, charType: String = "word") = WordEntity(
        id = id, verseId = 1, position = id,
        textUthmani = text, textIndopak = text, textQpcHafs = text,
        textUthmaniTajweed = null,
        translation = null, transliteration = null, charTypeName = charType
    )

    private fun verse(text: String) = VerseEntity(
        id = 1, chapterId = 1, verseNumber = 1, verseKey = "1:1",
        textUthmani = text, textIndopak = text, textQpcHafs = text,
        pageNumber = 1, juzNumber = 1
    )

    @Test
    fun `every sign survives the per-word path on embedded Hafs`() {
        val raw = "فَٱتَّقُو$signs"
        val display = mushafPlainWordTexts(listOf(word(1, raw)), "madani-standard", "KFGQPC Hafs")
        assertEquals(raw, display[0])
    }

    @Test
    fun `every sign survives the per-word path on non-embedded fonts`() {
        val raw = "كِتَٰبٌ$signs"
        val display = mushafPlainWordTexts(listOf(word(1, raw)), "madani-standard", "Amiri Quran")
        assertEquals(raw, display[0])
    }

    @Test
    fun `dotted circle placeholder is stripped, real signs kept`() {
        val display = mushafPlainWordTexts(
            listOf(word(1, "فَٱتَّقُو\u25cc\u06dfا")),
            "madani-standard",
            "KFGQPC Hafs"
        )
        assertEquals("فَٱتَّقُو\u06dfا", display[0])
    }

    @Test
    fun `embedded end word renders bare digits without an extra medallion`() {
        val display = mushafPlainWordTexts(
            listOf(word(1, "بِسْمِ"), word(2, "\u06dd\u0663", charType = "end")),
            "madani-standard",
            "KFGQPC Hafs"
        )
        assertEquals("بِسْمِ", display[0])
        assertEquals("\u0663", display[1])
    }

    @Test
    fun `non-embedded end word keeps exactly one U+06DD frame`() {
        val display = mushafPlainWordTexts(
            listOf(word(1, "بِسْمِ"), word(2, "\u06dd\u0663", charType = "end")),
            "madani-standard",
            "Amiri Quran"
        )
        assertEquals("بِسْمِ", display[0])
        assertEquals("\u06dd\u0663", display[1])
    }

    @Test
    fun `first end word is kept, extra end words collapse`() {
        val display = mushafPlainWordTexts(
            listOf(
                word(1, "\u06dd\u0664", charType = "end"),
                word(2, "\u06dd\u0665", charType = "end")
            ),
            "madani-standard",
            "KFGQPC Hafs"
        )
        assertEquals("\u0664", display[0])
        assertEquals("", display[1])
    }

    @Test
    fun `verse-level fallback keeps signs and maps the digit-run to one end frame`() {
        val out = mushafPlainVerseText(
            verse("قُلْ يَا أَيُّهَا ٱلْكَفِرُونَ \u06ea\u06f6"),
            "madani-standard",
            "Amiri Quran"
        )
        assertTrue(out.contains("\u06ea"))
        assertTrue(out.endsWith("\u06dd\u0666"))
    }

    @Test
    fun `verse-level fallback on Hafs keeps signs and strips only frame and placeholder`() {
        val raw = "وَٱلْعَصْرِ \u06df \u06dd\u0664"
        val out = mushafPlainVerseText(verse(raw), "madani-standard", "KFGQPC Hafs")
        // The separator space before the sign is folded away too, so the sign
        // composes onto the base letter instead of its dotted-circle
        // placeholder; the end digits keep their spacing space.
        assertEquals("وَٱلْعَصْرِ\u06df \u0664", out)
    }

    @Test
    fun `tajweed verse html keeps signs and rebuilds one end marker`() {
        val html = "<tajweed class=ikhafa>فَٱتَّقُو\u06df</tajweed> <span class=end>\u06dd\u0662</span>"
        val out = buildCleanVerseTajweedHtml(
            html,
            listOf(word(1, "فَٱتَّقُو\u06df"), word(2, "\u06dd\u0662", charType = "end")),
            verseNumber = 2,
            fontName = "KFGQPC Hafs"
        )
        assertTrue(out.contains("\u06df"))
        assertEquals(
            "<tajweed class=ikhafa>فَٱتَّقُو\u06df</tajweed><tajweed class='end'>\u0662</tajweed>",
            out
        )
    }
}
