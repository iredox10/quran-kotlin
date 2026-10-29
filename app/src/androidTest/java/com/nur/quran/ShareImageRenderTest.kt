package com.nur.quran

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nur.quran.ui.components.ShareCardFormat
import com.nur.quran.ui.components.ShareCardTheme
import com.nur.quran.ui.components.paginateVerseCard
import com.nur.quran.ui.components.renderVerseCardBitmap
import com.nur.quran.ui.components.renderVerseCardsToFiles
import com.nur.quran.ui.components.verseCardHeight
import com.nur.quran.ui.components.verseCardContentHeight
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareImageRenderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
    private val translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful."

    @Test
    fun exportMatchesPreviewForEveryThemeAndFormat(): Unit = runBlocking {
        for (format in ShareCardFormat.entries) {
            val pages = paginateVerseCard(context, arabic, translation, format)
            assertEquals(1, pages.size)
            for (theme in ShareCardTheme.entries) {
                val files = renderVerseCardsToFiles(context, "1:1", arabic, translation, "Al-Fatihah • Ayah 1:1", theme, format)
                assertEquals(1, files.size)
                val exported = requireNotNull(BitmapFactory.decodeFile(files.single().absolutePath))
                val preview = renderVerseCardBitmap(context, pages.single(), "Al-Fatihah • Ayah 1:1", theme, format)
                try {
                    assertEquals(1080, exported.width)
                    assertEquals(format.height, exported.height)
                    assertEquals(theme.background, exported.getPixel(0, 0))
                    assertTrue("Export and preview diverged for $theme / $format", exported.sameAs(preview))
                    assertTrue("Blank card", files.single().length() > 10_000)
                } finally {
                    exported.recycle()
                    preview.recycle()
                    files.single().delete()
                }
            }
        }
    }

    @Test
    fun longAyahPreservesEveryCharacterOnOneGrowingCard() {
        val longArabic = ("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ ").repeat(24)
        val longTranslation = ("Allah! There is no god but He, the Living, the Self-subsisting, Eternal. ").repeat(24)
        for (format in ShareCardFormat.entries) {
            val pages = paginateVerseCard(context, longArabic, longTranslation, format)
            assertEquals("Full ayah must stay on one image", 1, pages.size)
            val page = pages.single()
            assertEquals(longArabic, page.arabic)
            assertEquals(longTranslation, page.translation)
            val height = verseCardHeight(context, page, format)
            assertTrue("Card should grow beyond its minimum aspect ratio", height > format.height)
            assertTrue("Arabic and translation overlap the footer", height >= verseCardContentHeight(context, page) + 320)
        }
    }

    @Test
    fun arabicOnlyAndTranslationOnlyAreSupported() {
        assertEquals(arabic, paginateVerseCard(context, arabic, null).single().arabic)
        assertEquals(translation, paginateVerseCard(context, "", translation).single().translation)
    }
}
