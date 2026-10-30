package com.nur.quran.i18n

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class AppLanguagesTest {
    @Test fun `catalog includes every offered language with unique stable selection codes`() {
        val languages = AppLanguages.supported
        assertEquals(22, languages.size) // 21 interface languages plus system default.
        assertEquals(languages.size, languages.map { it.code }.toSet().size)
        assertTrue(languages.all { it.nativeName.isNotBlank() && it.englishName.isNotBlank() })
        assertTrue(languages.any { it.code == "ha" && it.nativeName == "Hausa" })
        assertTrue(languages.any { it.code == "yo" && it.nativeName == "Yorùbá" })
    }

    @Test fun `Arabic Persian and Urdu request right to left layout`() {
        assertEquals(setOf("ar", "fa", "ur"), AppLanguages.supported.filter { it.isRtl }.map { it.code }.toSet())
    }

    @Test fun `regional platform locales map back to stable picker selections`() {
        assertEquals("zh-Hans", AppLanguages.supportedCodeFor(Locale.forLanguageTag("zh-Hans-CN")))
        assertEquals("zh-Hans", AppLanguages.supportedCodeFor(Locale.forLanguageTag("zh-CN")))
        assertEquals("fr", AppLanguages.supportedCodeFor(Locale.CANADA_FRENCH))
        assertEquals("ar", AppLanguages.supportedCodeFor(Locale.forLanguageTag("ar-NG")))
        assertEquals("", AppLanguages.supportedCodeFor(Locale.forLanguageTag("pl")))
    }
}
