package com.nur.quran.ui.viewmodels

import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.api.TafsirResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class TafsirReaderTest {
    @Test fun `switching chapters and editions cannot reuse another chapter's explanation`() = runBlocking {
        val calls = mutableListOf<Pair<Int,Int>>()
        val reader = TafsirReader(this) { source, chapter ->
            calls += source to chapter
            TafsirResponse(listOf(ApiTafsirVerse("$chapter:1", "$source-$chapter", source)))
        }
        suspend fun show(key: String, source: Int): TafsirUiState.Visible {
            reader.open(key, source)
            return withTimeout(2000) { reader.state.first { it is TafsirUiState.Visible } as TafsirUiState.Visible }
        }
        assertEquals("169-6", show("6:1", 169).text)
        assertEquals("169-7", show("7:1", 169).text)
        assertEquals("168-6", show("6:1", 168).text)
        assertEquals("169-6", show("6:1", 169).text)
        assertEquals(listOf(169 to 6, 169 to 7, 168 to 6), calls)
        reader.dismiss()
        assertEquals(TafsirUiState.Hidden, reader.state.value)
    }

    @Test fun `dismissed or superseded requests cannot reopen a sheet`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val reader = TafsirReader(this) { source, chapter ->
            if (chapter == 6) withContext(NonCancellable) {
                started.complete(Unit)
                release.await()
                finished.complete(Unit)
            }
            TafsirResponse(listOf(ApiTafsirVerse("$chapter:1", "Explanation $chapter", source)))
        }
        reader.open("6:1",169)
        started.await()
        reader.open("7:1",169)
        withTimeout(2000) { reader.state.first { it is TafsirUiState.Visible } }
        reader.dismiss()
        release.complete(Unit)
        finished.await()
        assertEquals(TafsirUiState.Hidden, reader.state.value)
    }
}
