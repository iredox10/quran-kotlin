package com.nur.quran

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nur.quran.data.repository.QuranRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderTranslationSwitchTest {
    @Test fun pageReaderChangesEditionAndKeepsDownloadedText() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var repository: QuranRepository
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val model = androidx.lifecycle.ViewModelProvider(activity)[com.nur.quran.ui.viewmodels.SurahViewModel::class.java]
            val field = model.javaClass.getDeclaredField("repository").apply { isAccessible = true }
            repository = field.get(model) as QuranRepository
        }
        try {
        repository.ensureChaptersFromAssets()
        val english = repository.getPageVerses(1, translationId = 20).verses
        val french = repository.getPageVerses(1, translationId = 31).verses
        assertFalse(english.isEmpty())
        assertEquals(com.nur.quran.data.translation.BundledTranslation.texts(context)["1:1"], english.first().translation)
        assertEquals(english.first().translation, repository.getShareTranslation(20, "1:1"))
        assertEquals(english.map { it.verseKey }, french.map { it.verseKey })
        assertNotEquals(english.map { it.translation }, french.map { it.translation })
        assertFalse(french.any { it.translation == "Download this translation to read it offline" })
        assertEquals(french.first().translation, repository.getStoredTranslation(31, french.first().verseKey))
        assertEquals(french.first().translation, repository.getShareTranslation(31, french.first().verseKey))
        val offlineShare = repository.getShareTranslation(999999, english.first().verseKey)
        assertFalse(offlineShare.isNullOrBlank())
        assertNotEquals("Download this translation to read it offline", offlineShare)
        assertEquals(english.map { it.translation }, repository.getPageVerses(1, translationId = 20).verses.map { it.translation })
        } finally { scenario.close() }
    }
}
