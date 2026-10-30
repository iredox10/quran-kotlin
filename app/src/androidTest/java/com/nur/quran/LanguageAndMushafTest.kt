package com.nur.quran

import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.mushaf.Mushaf
import com.nur.quran.data.mushaf.fontIdToName
import com.nur.quran.i18n.AppLanguages
import com.nur.quran.ui.components.loadMushafPreview
import com.nur.quran.ui.screens.buildVerseDisplayText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LanguageAndMushafTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun everyConfiguredLanguageLoadsOfflineAndArabicContentStaysIdentical() {
        val original = loadMushafPreview(context)
        val baseline = original.map { buildVerseDisplayText(it.words, Mushaf.DEFAULT.id, fontIdToName(Mushaf.DEFAULT.defaultFontId)).first }
        val english = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }).getString(R.string.settings_title)
        for (language in AppLanguages.supported.filter { it.code.isNotBlank() }) {
            val locale = Locale.forLanguageTag(language.code)
            val configuration = Configuration(context.resources.configuration).apply {
                setLocales(LocaleList(locale)); setLocale(locale); setLayoutDirection(locale)
            }
            val localized = context.createConfigurationContext(configuration)
            assertTrue(language.code, localized.getString(R.string.settings_title).isNotBlank())
            assertTrue(language.code, localized.getString(R.string.mushaf_picker_intro).isNotBlank())
            if (language.code in listOf("ar", "fr", "ha", "yo")) assertNotEquals(language.code, english, localized.getString(R.string.settings_title))
            assertEquals(language.code, if (language.isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR, configuration.layoutDirection)
            assertEquals(language.code, baseline, loadMushafPreview(localized).map { buildVerseDisplayText(it.words, Mushaf.DEFAULT.id, fontIdToName(Mushaf.DEFAULT.defaultFontId)).first })
        }
    }

    @Test fun everySelectableEditionHasACompleteLocalPreviewWithNoUnsupportedPrivateGlyphs() {
        val sample = loadMushafPreview(context)
        assertEquals(listOf(2, 3), sample.map { it.number })
        assertEquals(3, Mushaf.SELECTABLE.size)
        for (edition in Mushaf.SELECTABLE) {
            sample.forEach { verse ->
                val text = buildVerseDisplayText(verse.words, edition.id, fontIdToName(edition.defaultFontId)).first
                assertTrue(edition.id, text.isNotBlank())
                assertFalse(edition.id, text.contains('<'))
                assertFalse(edition.id, text.contains('\u25cc'))
                assertFalse(edition.id, text.any { Character.getType(it) == Character.PRIVATE_USE.toInt() })
            }
        }
        assertFalse(Mushaf.INDOPAK.isSelectable)
        assertEquals(Mushaf.DEFAULT.id, Mushaf.forReading(Mushaf.INDOPAK.id).id)
    }
}
