package com.nur.quran.i18n

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleFormattingTest {
    private val now = 1_800_000_000_000L

    @Test fun frenchRelativeTimeUsesCldrWordOrderAndPluralForms() {
        assertEquals("il y a 39 minutes", relativeTimeLabel(now - 39 * 60_000, now, Locale.FRENCH))
        assertEquals("il y a 1 heure", relativeTimeLabel(now - 60 * 60_000, now, Locale.FRENCH))
        assertEquals("maintenant", relativeTimeLabel(now, now, Locale.FRENCH))
    }

    @Test fun arabicRelativeTimeDoesNotDependOnEnglishAgoTemplates() {
        val label = relativeTimeLabel(now - 39 * 60_000, now, Locale.forLanguageTag("ar"))
        assertTrue(label.startsWith("قبل"))
        assertTrue(label.contains("دقيقة"))
        assertFalse(label.contains("ago"))
        assertFalse(label.contains("min"))
    }
    @Test fun hausaAndYorubaUseLocalizedRelativeTimePatterns() {
        for (code in listOf("ha", "yo")) {
            val label = relativeTimeLabel(now - 39 * 60_000, now, Locale.forLanguageTag(code))
            println("$code relative time: $label")
            assertFalse("$code still uses an English relative time: $label", label.contains("ago", ignoreCase = true))
            assertTrue(label.isNotBlank())
        }
    }

}
