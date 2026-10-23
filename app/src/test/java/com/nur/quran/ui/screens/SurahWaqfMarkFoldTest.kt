package com.nur.quran.ui.screens

import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.ui.screens.mushafPlainVerseText
import com.nur.quran.ui.screens.mushafPlainWordTexts
import com.nur.quran.utils.TajweedProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression for the "dotted circles with signs" report.
 *
 * api.quran.com word texts carry pause/waqf marks AFTER a separating space
 * inside the word text ("رَّبِّهِمْ ۖ" = "...مْ" + U+0020 + U+06D6), and the
 * same pattern exists in the quran_full.json verse fallback (~4.4k occurrences
 * Quran-wide). Rendered per word, the space isolates the mark, the shaper has
 * no base in its cluster and draws the mark's dotted-circle placeholder
 * (U+25CC). foldWaqfMarkSeparator glues the mark to the word's last glyph;
 * the word-join itself renders the spacing space.
 */
class SurahWaqfMarkFoldTest {

    private fun word(position: Int, text: String?, charType: String = "word"): WordEntity =
        WordEntity(
            id = position,
            verseId = 1,
            position = position,
            textUthmani = text,
            textIndopak = text,
            textQpcHafs = text,
            textUthmaniTajweed = null,
            translation = null,
            transliteration = null,
            charTypeName = charType,
            lineNumber = 0
        )

    // 2:5 word 5 as shipped by api.quran.com v4: pause mark after a space.
    private val waqfWord = "رَّبِّهِمْ \u06D6"

    @Test
    fun `fold glues space-separated waqf mark to its base word`() {
        assertEquals("رَّبِّهِمْ\u06D6", TajweedProcessor.foldWaqfMarkSeparator(waqfWord))
    }

    @Test
    fun `fold preserves normal word-join spaces`() {
        val joined = "فِي قُلُوبِهِم"
        assertEquals(joined, TajweedProcessor.foldWaqfMarkSeparator(joined))
    }

    @Test
    fun `fold is idempotent`() {
        val once = TajweedProcessor.foldWaqfMarkSeparator(waqfWord)
        assertEquals(once, TajweedProcessor.foldWaqfMarkSeparator(once))
    }

    @Test
    fun `fold covers indopak sign variants and NBSP, keeps standalone symbols spaced`() {
        // U+0615 small-high-tah (2:13 indopak); NBSP as the separator variant.
        assertEquals("م\u0615", TajweedProcessor.foldWaqfMarkSeparator("م \u0615"))
        assertEquals("م\u06D6", TajweedProcessor.foldWaqfMarkSeparator("م\u00a0\u06D6"))
        // U+06E9 place of sajdah (So, standalone glyph) keeps its spacing.
        assertEquals("م \u06E9", TajweedProcessor.foldWaqfMarkSeparator("م \u06E9"))
    }

    @Test
    fun `mushafPlainWordTexts folds waqf marks per word`() {
        val words = listOf(
            word(1, "هُدًى"),
            word(2, waqfWord)
        )
        val texts = mushafPlainWordTexts(words, "madani-standard", "KFGQPC Hafs")
        assertEquals("هُدًى", texts[0])
        assertFalse(
            "word text must not keep a space before its mark: ${texts[1]}",
            texts[1].contains(" \u06D6")
        )
        assertTrue(texts[1].endsWith("\u06D6"))
    }

    @Test
    fun `mushafPlainWordTexts keeps end-marker canonicalization`() {
        val words = listOf(
            word(1, "قُلْ"),
            word(2, "٥", charType = "end")
        )
        val texts = mushafPlainWordTexts(words, "madani-standard", "Uthman Taha Naskh")
        assertEquals("\u06DD٥", texts[1])
    }

    @Test
    fun `mushafPlainVerseText folds verse-level space-mark sequences`() {
        val verse = VerseEntity(
            id = 1,
            chapterId = 2,
            verseNumber = 2,
            verseKey = "2:2",
            textUthmani = "لَا رَيْبَ \u06DB فِيهِ \u06DB",
            textIndopak = "لَا رَيْبَ \u06DB فِيهِ \u06DB",
            textQpcHafs = "لَا رَيْبَ \u06DB فِيهِ \u06DB",
            pageNumber = 1,
            juzNumber = 1,
            translation = null
        )
        val out = mushafPlainVerseText(verse, "madani-standard", "Uthman Taha Naskh")
        assertEquals("لَا رَيْبَ\u06DB فِيهِ\u06DB", out)
    }

    @Test
    fun `getWordTajweedSegments tolerates unfolded markup with space-separated mark`() {
        // Markup side: mark separate after a space; plain side: folded (glued).
        // The processor folds both sides internally, segments stay index-covering.
        val plain = "قُلْۖ"
        val html = "قُلْ <tajweed class='qalaqah'>ۖ</tajweed>"
        val segs = TajweedProcessor.getWordTajweedSegments(plain, html, "#000000")
        assertTrue("must produce segments over the plain text", segs.isNotEmpty())
        assertEquals(0, segs.minOf { it.start })
        assertEquals(plain.length, segs.maxOf { it.end })
    }
}
