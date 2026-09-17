package com.nur.quran.ui.screens

import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.ui.screens.buildCleanVerseTajweedHtml
import com.nur.quran.ui.screens.formatArabicDigits
import com.nur.quran.ui.screens.formatCleanEndMarker
import com.nur.quran.ui.screens.mushafPlainVerseText
import com.nur.quran.ui.screens.mushafPlainWordTexts
import com.nur.quran.ui.screens.usesEmbeddedEndMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the two reported end-of-ayah marker bugs:
 *
 * Bug 1 — double medallion: the KFGQPC Hafs font shapes bare Arabic-Indic digits
 * into the full ayah medallion via GSUB, so emitting U+06DD before the digits
 * renders TWO medallions. Other fonts need the U+06DD frame.
 *
 * Bug 2 — detached/duplicated marker: the clean tajweed HTML and the plain
 * mushaf word list must end with exactly one end marker attached after the
 * verse words, even when duplicate `end` word rows exist (stale offline rows
 * surviving alongside network rows).
 *
 * Entities are constructed directly, following the TranslationFallbackTest pattern.
 */
class SurahEndMarkerTest {

    private fun word(
        id: Int,
        position: Int,
        text: String?,
        charType: String = "word",
        tajweed: String? = null
    ): WordEntity = WordEntity(
        id = id,
        verseId = 1,
        position = position,
        textUthmani = text,
        textIndopak = text,
        textQpcHafs = text,
        textUthmaniTajweed = tajweed,
        translation = null,
        transliteration = null,
        charTypeName = charType,
        lineNumber = 0
    )

    private fun endWord(id: Int, position: Int, text: String?): WordEntity = word(
        id = id,
        position = position,
        text = text,
        charType = "end"
    )

    private fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var from = 0
        while (true) {
            val idx = haystack.indexOf(needle, from)
            if (idx < 0) return count
            count++
            from = idx + needle.length
        }
    }

    // ---- (1) KFGQPC-Hafs marker is bare digits, other fonts get U+06DD prefix ----

    @Test
    fun `formatArabicDigits converts to Arabic-Indic digits`() {
        assertEquals("١", formatArabicDigits(1))
        assertEquals("٢", formatArabicDigits(2))
        assertEquals("٧", formatArabicDigits(7))
        assertEquals("١٠", formatArabicDigits(10))
        assertEquals("١١٤", formatArabicDigits(114))
        assertEquals("٢٥٥", formatArabicDigits(255))
    }

    @Test
    fun `usesEmbeddedEndMarker is true only for KFGQPC Hafs`() {
        assertTrue(usesEmbeddedEndMarker("KFGQPC Hafs"))
        assertTrue(usesEmbeddedEndMarker("kfgqpc-hafs"))
        assertTrue(usesEmbeddedEndMarker("  KFGQPC HAFS  "))
        assertFalse(usesEmbeddedEndMarker("Uthman Taha Naskh"))
        assertFalse(usesEmbeddedEndMarker("Amiri Quran"))
        assertFalse(usesEmbeddedEndMarker("Noto Naskh Arabic"))
        assertFalse(usesEmbeddedEndMarker("Scheherazade New"))
        assertFalse(usesEmbeddedEndMarker("System Default"))
    }

    @Test
    fun `formatCleanEndMarker KFGQPC is bare digits, other fonts get U+06DD prefix`() {
        val kfgqpc = formatCleanEndMarker(null, 7, "KFGQPC Hafs")
        assertEquals("<tajweed class='end'>٧</tajweed>", kfgqpc)
        assertFalse("KFGQPC marker must not contain U+06DD: $kfgqpc", kfgqpc.contains("\u06DD"))

        val uthman = formatCleanEndMarker(null, 7, "Uthman Taha Naskh")
        assertEquals("<tajweed class='end'>\u06DD٧</tajweed>", uthman)

        val amiri = formatCleanEndMarker(null, 7, "Amiri Quran")
        assertEquals("<tajweed class='end'>\u06DD٧</tajweed>", amiri)
    }

    @Test
    fun `formatCleanEndMarker strips ornament frame from end word for KFGQPC`() {
        // End word as stored offline/API: ornament frame around the number.
        val framed = endWord(99, 5, "\u06DD٧")
        val kfgqpc = formatCleanEndMarker(framed, 7, "KFGQPC Hafs")
        assertEquals("<tajweed class='end'>٧</tajweed>", kfgqpc)

        val braced = endWord(99, 5, "﴿٧﴾")
        assertEquals("<tajweed class='end'>٧</tajweed>", formatCleanEndMarker(braced, 7, "KFGQPC Hafs"))

        // Other fonts keep/ensure the U+06DD frame.
        assertEquals(
            "<tajweed class='end'>\u06DD٧</tajweed>",
            formatCleanEndMarker(framed, 7, "Uthman Taha Naskh")
        )
    }

    // ---- (4) Arabic-Indic digit end words resolve to the right verse number ----

    @Test
    fun `formatCleanEndMarker arabic-indic end word resolves to verse number`() {
        val arabicIndicEnd = endWord(99, 5, "٧")
        assertEquals(
            "<tajweed class='end'>٧</tajweed>",
            formatCleanEndMarker(arabicIndicEnd, 7, "KFGQPC Hafs")
        )
        assertEquals(
            "<tajweed class='end'>\u06DD٧</tajweed>",
            formatCleanEndMarker(arabicIndicEnd, 7, "Uthman Taha Naskh")
        )
    }

    @Test
    fun `formatCleanEndMarker multi-digit arabic-indic end word resolves correctly`() {
        val end = endWord(99, 8, "٢٥٥")
        assertEquals(
            "<tajweed class='end'>٢٥٥</tajweed>",
            formatCleanEndMarker(end, 255, "KFGQPC Hafs")
        )
        assertEquals(
            "<tajweed class='end'>\u06DD٢٥٥</tajweed>",
            formatCleanEndMarker(end, 255, "Scheherazade New")
        )
    }

    @Test
    fun `formatCleanEndMarker ascii end word resolves to same digits`() {
        val asciiEnd = endWord(99, 5, "7")
        assertEquals(
            "<tajweed class='end'>٧</tajweed>",
            formatCleanEndMarker(asciiEnd, 7, "KFGQPC Hafs")
        )
    }

    // ---- (2) buildCleanVerseTajweedHtml ends with exactly one attached marker ----

    @Test
    fun `buildCleanVerseTajweedHtml ends with exactly one attached marker`() {
        val words = listOf(
            word(1, 1, "بِسْمِ"),
            word(2, 2, "ٱللَّهِ"),
            endWord(3, 3, "١")
        )
        val html = buildCleanVerseTajweedHtml(null, words, 1, "KFGQPC Hafs")

        assertTrue("must end with closing tajweed tag, got: $html", html.endsWith("</tajweed>"))
        assertTrue(
            "must end with bare-digit marker, got: $html",
            html.endsWith("<tajweed class='end'>١</tajweed>")
        )
        assertEquals("exactly one end marker, got: $html", 1, countOccurrences(html, "class='end'"))
        assertEquals("KFGQPC output must not contain U+06DD, got: $html", 0, countOccurrences(html, "\u06DD"))
        assertTrue("verse words must precede marker, got: $html", html.startsWith("بِسْمِ"))
        assertFalse("no double space before marker, got: $html", html.contains("  <tajweed"))
    }

    @Test
    fun `buildCleanVerseTajweedHtml collapses duplicate end words to single marker`() {
        val words = listOf(
            word(1, 1, "ٱلْحَمْدُ"),
            word(2, 2, "لِلَّهِ"),
            endWord(3, 3, "٢"),
            endWord(4, 4, "٢")
        )
        val html = buildCleanVerseTajweedHtml(null, words, 2, "KFGQPC Hafs")

        assertEquals("duplicate end rows must collapse to one marker, got: $html", 1, countOccurrences(html, "class='end'"))
        assertTrue("got: $html", html.endsWith("<tajweed class='end'>٢</tajweed>"))
        assertTrue("got: $html", html.contains("ٱلْحَمْدُ"))
        assertTrue("got: $html", html.contains("لِلَّهِ"))
    }

    @Test
    fun `buildCleanVerseTajweedHtml strips stale end marker from fullVerseHtml`() {
        val words = listOf(
            word(1, 1, "بِسْمِ"),
            endWord(2, 2, "١")
        )
        val stale = "بِسْمِ <tajweed class='end'>\u06DD١</tajweed>"
        val html = buildCleanVerseTajweedHtml(stale, words, 1, "KFGQPC Hafs")

        assertEquals("stale marker must be stripped to exactly one, got: $html", 1, countOccurrences(html, "class='end'"))
        assertTrue("got: $html", html.endsWith("<tajweed class='end'>١</tajweed>"))
        assertEquals("KFGQPC must not leak U+06DD, got: $html", 0, countOccurrences(html, "\u06DD"))
    }

    @Test
    fun `buildCleanVerseTajweedHtml other fonts emit single U+06DD marker`() {
        val words = listOf(
            word(1, 1, "بِسْمِ"),
            endWord(2, 2, "١"),
            endWord(3, 3, "١")
        )
        val html = buildCleanVerseTajweedHtml(null, words, 1, "Uthman Taha Naskh")

        assertEquals(1, countOccurrences(html, "class='end'"))
        assertEquals(1, countOccurrences(html, "\u06DD"))
        assertTrue("got: $html", html.endsWith("<tajweed class='end'>\u06DD١</tajweed>"))
    }

    // ---- (3) mushafPlainWordTexts collapses duplicates, never drops words ----

    @Test
    fun `mushafPlainWordTexts collapses duplicate end words and keeps all words`() {
        val words = listOf(
            word(1, 1, "ٱلْحَمْدُ"),
            word(2, 2, "لِلَّهِ"),
            endWord(3, 3, "٢"),
            endWord(4, 4, "٢")
        )
        val texts = mushafPlainWordTexts(words, "madani-standard", "KFGQPC Hafs")

        // Index-aligned: no reordering/removal so tap targets stay valid.
        assertEquals(4, texts.size)
        assertEquals("ٱلْحَمْدُ", texts[0])
        assertEquals("لِلَّهِ", texts[1])
        // Exactly one non-blank end marker.
        val endTexts = texts.subList(2, 4)
        assertEquals(1, endTexts.count { it.isNotBlank() })
        assertEquals("", endTexts[1])
        assertFalse(endTexts[0].contains("\u06DD"))
        assertTrue(endTexts[0].contains("٢"))
    }

    @Test
    fun `mushafPlainWordTexts other fonts ensure U+06DD on single marker`() {
        val words = listOf(
            word(1, 1, "بِسْمِ"),
            endWord(2, 2, "١"),
            endWord(3, 3, "١")
        )
        val texts = mushafPlainWordTexts(words, "madani-standard", "Uthman Taha Naskh")

        assertEquals(3, texts.size)
        assertEquals("بِسْمِ", texts[0])
        assertTrue("marker must carry U+06DD frame, got: ${texts[1]}", texts[1].contains("\u06DD"))
        assertTrue(texts[1].contains("١"))
        assertEquals("", texts[2])
    }

    @Test
    fun `mushafPlainWordTexts KFGQPC strips U+06DD frame from stored end word`() {
        val words = listOf(
            word(1, 1, "بِسْمِ"),
            endWord(2, 2, "\u06DD١")
        )
        val texts = mushafPlainWordTexts(words, "madani-standard", "KFGQPC Hafs")

        assertEquals(2, texts.size)
        assertEquals("بِسْمِ", texts[0])
        assertEquals("١", texts[1])
    }

    @Test
    fun `mushafPlainVerseText applies per-font ornament rules`() {
        val verse = VerseEntity(
            id = 1,
            chapterId = 1,
            verseNumber = 1,
            verseKey = "1:1",
            textUthmani = "بِسْمِ ٱللَّهِ",
            textIndopak = "بِسْمِ ٱللَّهِ",
            textQpcHafs = "بِسْمِ ٱللَّهِ \u06DD١",
            pageNumber = 1,
            juzNumber = 1,
            translation = null
        )
        val kfgqpc = mushafPlainVerseText(verse, "madani-standard", "KFGQPC Hafs")
        assertFalse("KFGQPC verse text must strip U+06DD, got: $kfgqpc", kfgqpc.contains("\u06DD"))

        val uthman = mushafPlainVerseText(verse, "madani-standard", "Uthman Taha Naskh")
        assertTrue("other fonts keep U+06DD frame, got: $uthman", uthman.contains("\u06DD"))
    }
}
