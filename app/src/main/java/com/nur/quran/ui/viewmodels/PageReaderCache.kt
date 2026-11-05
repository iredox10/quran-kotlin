package com.nur.quran.ui.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * Offline-first per-page cache + load guard for the planner reader.
 *
 * Pure JVM-testable policy: [SurahViewModel.loadPageVerses] serves [cached]
 * payloads synchronously (never emitting Loading for them) and uses the
 * generation counter so a superseded page load can't overwrite a newer page.
 */
data class PageCacheParams(val mushafId: String, val translationId: Int)

class PageReaderCache<T> {
    private val entries = object : LinkedHashMap<Pair<Int, PageCacheParams>, T>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Int, PageCacheParams>, T>?): Boolean = size > 32
    }

    @Volatile
    private var generation: Long = 0L

    /** Cached payload for [page] only when stored under identical [params]. */
    @Synchronized fun cached(page: Int, params: PageCacheParams): T? {
        return entries[page to params]
    }

    @Synchronized fun store(page: Int, params: PageCacheParams, value: T) {
        entries[page to params] = value
    }

    /** Drops all cached pages (e.g. mushaf/translation switch). */
    @Synchronized fun clear() {
        entries.clear()
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

/** Prefer upcoming pages, while keeping backwards navigation ready too. */
internal fun pagePrefetchOrder(page: Int, first: Int = 1, last: Int = 604): List<Int> = buildList {
    for (offset in 1..4) if (page + offset in first..last) add(page + offset)
    for (offset in 1..2) if (page - offset in first..last) add(page - offset)
    for (offset in 5..8) if (page + offset in first..last) add(page + offset)
}

/** Foreground reads share work already started by the prefetcher. */
internal class PageLoadCoordinator<T : Any>(
    private val scope: CoroutineScope,
    private val cache: PageReaderCache<T>,
    private val load: suspend (Int, PageCacheParams) -> T,
) {
    private val inFlight = mutableMapOf<Pair<Int, PageCacheParams>, Deferred<T>>()

    suspend fun get(page: Int, params: PageCacheParams): T {
        cache.cached(page, params)?.let { return it }
        val key = page to params
        val task = synchronized(inFlight) {
            inFlight[key] ?: scope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
                cache.cached(page, params) ?: load(page, params).also { cache.store(page, params, it) }
            }.also { created ->
                inFlight[key] = created
                created.invokeOnCompletion { synchronized(inFlight) { if (inFlight[key] === created) inFlight.remove(key) } }
            }
        }
        return task.await()
    }
}
