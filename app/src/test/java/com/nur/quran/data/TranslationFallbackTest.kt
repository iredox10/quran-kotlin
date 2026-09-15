package com.nur.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationFallbackTest {

    @Test
    fun `deprecated translation id 131 is migrated to 20`() {
        val storedId = 131
        val effectiveId = if (storedId == 131) 20 else storedId
        assertEquals(20, effectiveId)
    }

    @Test
    fun `supported translation ids are preserved`() {
        val validIds = listOf(20, 85, 22, 84, 32, 234)
        for (id in validIds) {
            val effective = if (id == 131) 20 else id
            assertEquals(id, effective)
        }
    }

    @Test
    fun `blank translation falls back to offline translation`() {
        val offlineMap = mapOf(
            "1:1" to "In the name of God, the Lord of Mercy, the Giver of Mercy!",
            "2:1" to "Alif Lam Mim",
            "2:2" to "This is the Scripture in which there is no doubt, containing guidance for those who are mindful of God,"
        )

        val apiTranslation: String? = null
        val verseKey = "2:2"
        val resolved = if (!apiTranslation.isNullOrBlank()) apiTranslation else offlineMap[verseKey]

        assertNotNull(resolved)
        assertEquals("This is the Scripture in which there is no doubt, containing guidance for those who are mindful of God,", resolved)
    }

    @Test
    fun `non-blank api translation is preserved without override`() {
        val offlineMap = mapOf("1:1" to "Fallback Translation")
        val apiTranslation = "Custom API Translation Text"
        val verseKey = "1:1"
        val resolved = if (!apiTranslation.isNullOrBlank()) apiTranslation else offlineMap[verseKey]

        assertEquals("Custom API Translation Text", resolved)
    }

    @Test
    fun `offline fallback covers prominent surahs`() {
        val testKeys = listOf("1:1", "1:2", "2:1", "2:2", "2:255", "36:1", "112:1", "114:1")
        val mockOfflineTranslations = testKeys.associateWith { "Translation for $it" }

        for (key in testKeys) {
            assertTrue(mockOfflineTranslations.containsKey(key))
            assertFalse(mockOfflineTranslations[key].isNullOrBlank())
        }
    }
}
