package com.nur.quran

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nur.quran.data.api.rn.RnApi
import com.nur.quran.data.api.rn.RnApiClient
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.ui.viewmodels.SurahViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class OfflineReaderContentTest {
    @Test fun ordinaryReadingNeverCallsTheContentApi() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var repository: QuranRepository
            lateinit var reader: SurahViewModel
            scenario.onActivity { activity ->
                val model = ViewModelProvider(activity)[SurahViewModel::class.java]
                reader = model
                repository = model.javaClass.getDeclaredField("repository").apply { isAccessible = true }.get(model) as QuranRepository
            }
            val client = repository.javaClass.getDeclaredField("rnApiClient").apply { isAccessible = true }.get(repository) as RnApiClient
            val apiField = client.javaClass.getDeclaredField("api").apply { isAccessible = true }
            val original = apiField.get(client)
            val calls = java.util.concurrent.CopyOnWriteArrayList<String>()
            apiField.set(client, Proxy.newProxyInstance(RnApi::class.java.classLoader, arrayOf(RnApi::class.java)) { _, method, args ->
                if (method.name !in setOf("getWordsPack", "getChapters", "getPage", "getVerses", "getReciters", "getTajweed", "getTranslations", "getTranslation")) method.invoke(original, *(args ?: emptyArray()))
                else { calls.add(method.name); throw AssertionError("Unexpected reading HTTP call: ${method.name}") }
            })
            val dao = repository.javaClass.getDeclaredField("quranDao").apply { isAccessible = true }.get(repository) as com.nur.quran.data.db.dao.QuranDao
            val testEdition = 987654321
            try {
                assertEquals(6236, repository.memorizationInventory().size)
                assertTrue(repository.memorizationPageTransitions(2).isNotEmpty())
                assertEquals(setOf("1:1", "2:255", "114:6"), reader.getVerseTexts(listOf("1:1", "2:255", "114:6")).keys)
                repository.refreshChapters()
                repository.refreshReciters()
                for (page in listOf(1, 2, 3, 50, 300, 603, 604)) {
                    val bundle = repository.getPageVerses(page, translationId = 999999)
                    assertTrue("Page $page", bundle.verses.isNotEmpty())
                    assertTrue(bundle.verses.all { !it.textUthmani.isNullOrBlank() && !it.translation.isNullOrBlank() })
                    assertTrue(bundle.wordsMap.isNotEmpty())
                    assertTrue(repository.getTajweedMapForVerses(bundle.verses).isNotEmpty())
                    for (chapter in bundle.verses.map { it.chapterId }.distinct()) repository.ensureChapterWordsCached(chapter)
                }
                reader.loadChapterDetails(2)
                withTimeout(20_000) { reader.uiState.first { state ->
                    state is com.nur.quran.ui.viewmodels.SurahUiState.Success && state.chapter.id == 2
                } }
                assertEquals(286, (reader.uiState.value as com.nur.quran.ui.viewmodels.SurahUiState.Success).verses.size)
                delay(500) // Catch deferred chapter upgrades as well as first-paint reads.
                dao.upsertTranslationTexts(listOf(com.nur.quran.data.db.entities.TranslationTextEntity(testEdition, "1:1", "Local translation fixture")))
                assertEquals("Local translation fixture", repository.getPageVerses(1, translationId = testEdition).verses.first().translation)
                assertEquals("Local translation fixture", repository.getShareTranslation(testEdition, "1:1"))
                val settings=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("hifdh_settings",0)
                val originalTranslation=settings.getInt("translation_id",20)
                try {
                    settings.edit().putInt("translation_id",testEdition).commit()
                    val historyVerse=reader.getHistoryVerseTexts(listOf("1:1"))["1:1"]!!
                    assertEquals("Local translation fixture",historyVerse.translation)
                    assertNotEquals(historyVerse.textUthmani,historyVerse.textQpcHafs)
                } finally { settings.edit().putInt("translation_id",originalTranslation).commit() }
                assertTrue("Unexpected HTTP: $calls", calls.isEmpty())
            } finally {
                apiField.set(client, original)
                dao.deleteTranslationPack(testEdition)
            }
        }
    }
}
