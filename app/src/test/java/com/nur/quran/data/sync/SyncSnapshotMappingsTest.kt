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
}
