package com.nur.quran.ui.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
