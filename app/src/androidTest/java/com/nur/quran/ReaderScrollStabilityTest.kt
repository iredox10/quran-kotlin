package com.nur.quran

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.ui.screens.SurahScreen
import com.nur.quran.ui.viewmodels.SurahUiState
import com.nur.quran.ui.viewmodels.SurahViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderScrollStabilityTest {
    @Test fun aMetadataUpdateDoesNotReplayTheOpeningVerseWhileReading() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var reader: SurahViewModel
        lateinit var dao: QuranDao
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            reader = ViewModelProvider(activity)[SurahViewModel::class.java]
            val repository = reader.javaClass.getDeclaredField("repository").apply { isAccessible = true }.get(reader) as QuranRepository
            dao = repository.javaClass.getDeclaredField("quranDao").apply { isAccessible = true }.get(repository) as QuranDao
        }
        val previous = dao.getRecentlyReadForChapter(2)
        try {
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme { SurahScreen(reader, 2, targetVerseKey = "2:1", onBackClick = {}) }
            } }
            withTimeout(30_000) { reader.uiState.first { it is SurahUiState.Success && it.chapter.id == 2 } }
            instrumentation.waitForIdleSync()
            Thread.sleep(500)
            repeat(3) {
                instrumentation.uiAutomation.executeShellCommand("input swipe 540 1800 540 650 300").let {
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() }
                }
            }
            fun settledPosition(): Pair<Int, Int> {
                val deadline = android.os.SystemClock.uptimeMillis() + 10_000
                var last = reader.getSurahScrollPosition(2)
                var stableSince = android.os.SystemClock.uptimeMillis()
                while (android.os.SystemClock.uptimeMillis() < deadline) {
                    Thread.sleep(100)
                    val current = reader.getSurahScrollPosition(2)
                    if (current != last) { last = current; stableSince = android.os.SystemClock.uptimeMillis() }
                    if (last != null && android.os.SystemClock.uptimeMillis() - stableSince >= 700) return last
                }
                error("Reader did not settle after the swipe")
            }
            val before = settledPosition()
            assertTrue("The reader must have moved beyond the opening verse", before.first > 3)
            scenario.onActivity {
                @Suppress("UNCHECKED_CAST")
                val state = reader.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }.get(reader) as MutableStateFlow<SurahUiState>
                val success = state.value as SurahUiState.Success
                state.value = success.copy(tajweedMap = success.tajweedMap.orEmpty() + ("unused-test-key" to ""))
            }
            instrumentation.waitForIdleSync()
            val after = settledPosition()
            assertEquals("An updated overlay must not replay the deep link", before, after)
        } finally {
            scenario.close()
            if (previous != null) dao.upsertRecentlyRead(previous) else dao.deleteRecentlyReadByChapter(2)
        }
    }
}
