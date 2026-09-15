package com.nur.quran.data

import com.nur.quran.data.api.ApiTranslation
import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.db.entities.VerseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationFallbackTest {

    @Test
    fun `api verse with null translation falls back to offline translation text for 1-1`() {
        val apiVerse = ApiVerse(
            id = 1,
            verse_number = 1,
            verse_key = "1:1",
            page_number = 1,
            juz_number = 1,
            text_uthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            words = null,
            translations = null,
            audio = null
        )

        val translation = TranslationFallback.resolveTranslation(apiVerse)

        assertNotNull(translation)
        assertTrue(
            "Expected offline fallback starting with 'In the name of God', got: $translation",
            translation!!.startsWith("In the name of God")
        )
        assertEquals(
            "In the name of God, the Lord of Mercy, the Giver of Mercy!",
            translation
        )
    }

    @Test
    fun `api verse with empty translations list falls back to offline text`() {
        val apiVerse = ApiVerse(
            id = 1,
            verse_number = 1,
            verse_key = "1:1",
            page_number = 1,
            juz_number = 1,
            text_uthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            words = null,
            translations = emptyList(),
            audio = null
        )

        val translation = TranslationFallback.resolveTranslation(apiVerse)

        assertNotNull(translation)
        assertTrue(translation!!.startsWith("In the name of God"))
    }

    @Test
    fun `api verse with blank translation text falls back to offline text`() {
        val blankTranslation = ApiTranslation(
            resource_id = 20,
            text = "   "
        )
        val apiVerse = ApiVerse(
            id = 1,
            verse_number = 1,
            verse_key = "1:1",
            page_number = 1,
            juz_number = 1,
            text_uthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            words = null,
            translations = listOf(blankTranslation),
            audio = null
        )

        val translation = TranslationFallback.resolveTranslation(apiVerse)

        assertNotNull(translation)
        assertTrue(translation!!.startsWith("In the name of God"))
    }

    @Test
    fun `api verse with valid translation retains api text without fallback`() {
        val validTranslation = ApiTranslation(
            resource_id = 20,
            text = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
        )
        val apiVerse = ApiVerse(
            id = 1,
            verse_number = 1,
            verse_key = "1:1",
            page_number = 1,
            juz_number = 1,
            text_uthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            words = null,
            translations = listOf(validTranslation),
            audio = null
        )

        val translation = TranslationFallback.resolveTranslation(apiVerse)

        assertEquals("In the name of Allah, the Entirely Merciful, the Especially Merciful.", translation)
    }

    @Test
    fun `translation 131 maps to fallback translation id 20`() {
        val resolvedId = TranslationFallback.resolveTranslationId(131)
        assertEquals(20, resolvedId)
        assertEquals(TranslationFallback.FALLBACK_TRANSLATION_ID, resolvedId)

        // Also verify the convenience function in com.nur.quran.data.translation
        val packageResolvedId = com.nur.quran.data.translation.resolveTranslationId(131)
        assertEquals(20, packageResolvedId)
    }

    @Test
    fun `valid translation ids other than 131 are preserved`() {
        assertEquals(20, TranslationFallback.resolveTranslationId(20))
        assertEquals(85, TranslationFallback.resolveTranslationId(85))
        assertEquals(22, TranslationFallback.resolveTranslationId(22))
        assertEquals(163, TranslationFallback.resolveTranslationId(163))
    }

    @Test
    fun `backfillBlankTranslations fills null and empty translations from offline fallback`() {
        val verses = listOf(
            VerseEntity(
                id = 1,
                chapterId = 1,
                verseNumber = 1,
                verseKey = "1:1",
                textUthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                textIndopak = null,
                textQpcHafs = null,
                pageNumber = 1,
                juzNumber = 1,
                translation = null
            ),
            VerseEntity(
                id = 2,
                chapterId = 1,
                verseNumber = 2,
                verseKey = "1:2",
                textUthmani = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ",
                textIndopak = null,
                textQpcHafs = null,
                pageNumber = 1,
                juzNumber = 1,
                translation = ""
            ),
            VerseEntity(
                id = 3,
                chapterId = 1,
                verseNumber = 3,
                verseKey = "1:3",
                textUthmani = "ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                textIndopak = null,
                textQpcHafs = null,
                pageNumber = 1,
                juzNumber = 1,
                translation = "Existing translation text"
            )
        )

        val backfilled = TranslationFallback.backfillBlankTranslations(verses)

        assertEquals(3, backfilled.size)
        assertTrue(backfilled[0].translation!!.startsWith("In the name of God"))
        assertEquals("Praise belongs to God, Lord of the Worlds,", backfilled[1].translation)
        assertEquals("Existing translation text", backfilled[2].translation)
    }

    @Test
    fun `getOfflineTranslation provides offline text for common verses`() {
        val verse1 = TranslationFallback.getOfflineTranslation("1:1")
        assertNotNull(verse1)
        assertTrue(verse1!!.startsWith("In the name of God"))

        val verse2 = TranslationFallback.getOfflineTranslation("1:2")
        assertEquals("Praise belongs to God, Lord of the Worlds,", verse2)
    }
}
