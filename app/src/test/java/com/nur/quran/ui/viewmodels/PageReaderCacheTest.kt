package com.nur.quran.ui.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

class PageReaderCacheTest {
    private val params = PageCacheParams("uthmani", 85)

    @Test
    fun cachedHit_servesSynchronously_withoutLoading() {
        val cache = PageReaderCache<String>()
        cache.store(5, params, "page-5")
        assertEquals("page-5", cache.cached(5, params))
        // Cached page must never need a spinner, even over visible content.
        assertFalse(cache.shouldEmitLoading(hasVisibleContent = true))
        // True first load (empty screen) is the only Loading case.
        assertTrue(cache.shouldEmitLoading(hasVisibleContent = false))
    }

    @Test
    fun cachedMiss_onParamsChange_treatsAsEmpty() {
        val cache = PageReaderCache<String>()
        cache.store(5, params, "page-5")
        assertNull(cache.cached(5, params.copy(mushafId = "indopak")))
        assertNull(cache.cached(5, params.copy(translationId = 20)))
        assertNull(cache.cached(6, params))
    }

    @Test
    fun generation_olderPageCannotOverwriteNewer() {
        val cache = PageReaderCache<String>()
        val stale = cache.beginLoad()
        val latest = cache.beginLoad()
        assertFalse(cache.isCurrent(stale))
        assertTrue(cache.isCurrent(latest))
        // Cache hit also invalidates an in-flight miss behind it.
        cache.beginLoad()
        assertFalse(cache.isCurrent(latest))
    }

    @Test
    fun prefetchOrder_prioritizesForwardPagesAndRespectsAssignmentBounds() {
        assertEquals(listOf(6, 7, 8, 9, 4, 3, 10, 11, 12, 13), pagePrefetchOrder(5))
        assertEquals(listOf(603, 604, 601, 600), pagePrefetchOrder(602, 600, 604))
        assertEquals(listOf(2, 3), pagePrefetchOrder(1, 1, 3))
    }

    @Test
    fun foregroundSharesPrefetchEvenWhenPrefetchWaiterIsCancelled() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val calls = AtomicInteger()
            val loader = PageLoadCoordinator(scope, PageReaderCache<String>()) { page, _ ->
                calls.incrementAndGet()
                started.complete(Unit)
                release.await()
                "page-$page"
            }
            val prefetch = async { loader.get(5, params) }
            started.await()
            val foreground = async { loader.get(5, params) }
            prefetch.cancel()
            release.complete(Unit)
            assertEquals("page-5", foreground.await())
            assertEquals(1, calls.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cachedPagesKeepTheirOwnReadingSettings() {
        val cache = PageReaderCache<String>()
        val other = params.copy(translationId = 20)
        cache.store(5, params, "first")
        cache.store(5, other, "second")
        assertEquals("first", cache.cached(5, params))
        assertEquals("second", cache.cached(5, other))
    }
}
