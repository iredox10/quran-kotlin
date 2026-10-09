package com.nur.quran.ui.viewmodels

/**
 * Offline-first per-page cache + load guard for the planner reader.
 *
 * Pure JVM-testable policy: [SurahViewModel.loadPageVerses] serves [cached]
 * payloads synchronously (never emitting Loading for them) and uses the
 * generation counter so a superseded page load can't overwrite a newer page.
 */
data class PageCacheParams(val mushafId: String, val translationId: Int)

class PageReaderCache<T> {
    private val entries = java.util.concurrent.ConcurrentHashMap<Int, Pair<PageCacheParams, T>>()

    @Volatile
    private var generation: Long = 0L

    /** Cached payload for [page] only when stored under identical [params]. */
    fun cached(page: Int, params: PageCacheParams): T? {
        val (storedParams, value) = entries[page] ?: return null
        return if (storedParams == params) value else null
    }

    fun store(page: Int, params: PageCacheParams, value: T) {
        entries[page] = params to value
    }

    /** Starts a load generation; invalidates every previously started load. */
    fun beginLoad(): Long {
        generation += 1
        return generation
    }

    /** True when [gen] is still the latest started load. */
    fun isCurrent(gen: Long): Boolean = gen == generation

    /**
     * Loading spinners only for a truly empty screen: when visible content
     * (a previous Success page) exists, keep rendering it instead of Loading.
     */
    fun shouldEmitLoading(hasVisibleContent: Boolean): Boolean = !hasVisibleContent
}
