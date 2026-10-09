package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM unit tests for the pure Android → web state mappings in SyncSnapshot.
 * Prefs/DAO reads stay in SnapshotBuilder (Android Context) and are not
 * covered here.
 */
class SyncSnapshotMappingsTest {

    @Test
    fun `default scale maps to web default font index`() {
        assertEquals(2, fontScaleToWebFontSizeIndex(1f))
    }

    @Test
    fun `font scale maps across the whole web index range`() {
        assertEquals(1, fontScaleToWebFontSizeIndex(0.5f))
        assertEquals(3, fontScaleToWebFontSizeIndex(1.25f))
        assertEquals(4, fontScaleToWebFontSizeIndex(1.5f))
        assertEquals(6, fontScaleToWebFontSizeIndex(2f))
        assertEquals(8, fontScaleToWebFontSizeIndex(2.5f))
    }

    @Test
    fun `font scale clamps past the web index range`() {
        assertEquals(1, fontScaleToWebFontSizeIndex(0f))
        assertEquals(8, fontScaleToWebFontSizeIndex(3f))
    }

    @Test
    fun `word tooltip behavior passes through known values`() {
        assertEquals("tajweed", normalizeWordTooltipBehavior("tajweed"))
        assertEquals("translation", normalizeWordTooltipBehavior(" translation "))
        assertEquals("none", normalizeWordTooltipBehavior("None"))
    }

    @Test
    fun `word tooltip behavior drops unknown or blank values`() {
        assertNull(normalizeWordTooltipBehavior(null))
        assertNull(normalizeWordTooltipBehavior(""))
        assertNull(normalizeWordTooltipBehavior("highlight"))
    }

    @Test
    fun `arabic font id is derived from the android font display name`() {
        assertEquals("kfgqpc-hafs", arabicFontIdFromName("KFGQPC Hafs"))
        assertEquals("scheherazade-new", arabicFontIdFromName("Scheherazade New"))
        assertEquals("amiri-quran", arabicFontIdFromName("Amiri Quran"))
        assertEquals("noto-naskh-arabic", arabicFontIdFromName("Noto Naskh Arabic"))
    }

    @Test
    fun `arabic font id is dropped for names web does not know`() {
        assertNull(arabicFontIdFromName(null))
        assertNull(arabicFontIdFromName(""))
        assertNull(arabicFontIdFromName("Comic Sans Arabic"))
    }

    @Test
    fun `read preference passes through web values`() {
        assertEquals("before", normalizeReadPreference("before"))
        assertEquals("split", normalizeReadPreference(" SPLIT "))
    }

    @Test
    fun `read preference drops values web does not accept`() {
        assertNull(normalizeReadPreference(null))
        assertNull(normalizeReadPreference(""))
        assertNull(normalizeReadPreference("whenever"))
    }

    @Test
    fun `active prayers keep canonical order and drop unknown names`() {
        val parsed = normalizeActivePrayers(
            linkedSetOf("Isha", "Fajr", "Duha", "Asr", "Maghrib")
        )
        assertEquals(listOf("Fajr", "Asr", "Maghrib", "Isha"), parsed)
    }

    @Test
    fun `active prayers are omitted when nothing valid is stored`() {
        assertNull(normalizeActivePrayers(null))
        assertNull(normalizeActivePrayers(emptySet()))
        assertNull(normalizeActivePrayers(linkedSetOf("Duha")))
    }

    @Test
    fun `memorized surah ids are deduped and sorted`() {
        assertEquals(listOf(1, 2, 18), memorizedSurahIds(linkedSetOf("18", "1", "2", "1", "")))
    }

    @Test
    fun `memorized surah ids drop non-numeric entries`() {
        assertEquals(emptyList<Int>(), memorizedSurahIds(linkedSetOf("nope", "")))
        assertEquals(emptyList<Int>(), memorizedSurahIds(null))
    }

    @Test
    fun `memorized ayah keys are sorted and deduped`() {
        assertEquals(
            listOf("1:1", "2:5"),
            memorizedAyahKeys(linkedSetOf("2:5", "1:1", "2:5", " 1:1 "))
        )
        assertEquals(emptyList<String>(), memorizedAyahKeys(null))
    }

    @Test
    fun `tour flags become sorted ids`() {
        val prefs = mapOf<String, Any?>(
            "tour_home" to true,
            "tour_advanced-tour" to true,
            "tour_memorization-tour" to false,
            "coachmark_home" to true,
            "visit_home" to 3
        )
        assertEquals(listOf("advanced-tour", "home"), flagIds(prefs, "tour_"))
        assertEquals(listOf("home"), flagIds(prefs, "coachmark_"))
    }

    @Test
    fun `page visit counts keep positive entries only`() {
        val prefs = mapOf<String, Any?>(
            "visit_home" to 3,
            "visit_planner" to 0,
            "visit_" to 4,
            "onboarding_dismissed" to true
        )
        assertEquals(mapOf("home" to 3), pageVisitCounts(prefs))
    }
}
