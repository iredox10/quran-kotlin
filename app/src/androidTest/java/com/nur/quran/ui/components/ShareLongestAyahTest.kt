package com.nur.quran.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real bundled content catches Arabic combining marks and long translated text absent from synthetic fixtures. */
@RunWith(AndroidJUnit4::class)
class ShareLongestAyahTest {
    @Test fun longestBundledAyahsExportCompletelyInBothFormats(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rows = context.assets.open("data/quran_full.json").bufferedReader().use {
            JsonParser.parseReader(it).asJsonArray.map { row -> row.asJsonObject }
        }
        val debtVerse = rows.first { it["verse_key"].asString == "2:282" }
        val longestArabic = rows.maxBy { it["text_uthmani"].asString.length }
        val longestTranslation = rows.maxBy { it["translation"].asString.length }
        for (row in listOf(debtVerse,longestArabic,longestTranslation).distinctBy { it["verse_key"].asString }) {
            val key = row["verse_key"].asString
            val arabic = row["text_uthmani"].asString
            val translation = row["translation"].asString
            for (format in ShareCardFormat.entries) {
                val pages = paginateVerseCard(context,arabic,translation,format)
                assertEquals("Arabic was truncated for $key / $format",arabic,pages.joinToString("") { it.arabic })
                assertEquals("Translation was truncated for $key / $format",translation,pages.joinToString("") { it.translation })
                assertEquals("A full ayah must remain on a single card",1,pages.size)
                assertTrue("Both full texts belong to that one card",pages.single().arabic.isNotBlank() && pages.single().translation.isNotBlank())
                val page = pages.single()
                val contentHeight = verseCardContentHeight(context,page)
                val measuredHeight = verseCardHeight(context,page,format)
                assertTrue("Full shaped text overlaps the footer for $key",measuredHeight>=contentHeight+320)
                val reference = "Al-Baqarah • Ayah $key"
                val files = renderVerseCardsToFiles(context,key,arabic,translation,reference,ShareCardTheme.Ivory,format)
                try {
                    assertEquals("Export must produce one full-content image",1,files.size)
                    files.forEach { file ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath,bounds)
                        assertEquals(1080,bounds.outWidth)
                        assertEquals("Export height differs from complete text measurement",measuredHeight,bounds.outHeight)
                        assertTrue("Long ayah image did not expand for $key / $format",bounds.outHeight>format.height)
                        assertTrue("Runaway full-ayah image height for $key / $format",bounds.outHeight<=30_000)
                        assertTrue("Unreadable/empty full-ayah image for $key",file.length()>10_000)
                    }
                } finally { files.forEach { it.delete() } }
            }
        }
    }
}
