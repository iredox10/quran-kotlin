package com.nur.quran.data.sync

import com.google.gson.Gson
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.ApiResponseCacheEntity
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Records the rows `SyncService.applyState` writes, so a test can assert exactly what a
 * pull restored. `QuranDao` is a Room interface with no JVM binding, so it is backed by a
 * `Proxy` whose suspend members answer on a side thread: resuming the continuation
 * re-entrantly from inside the invocation makes the coroutine machinery abort.
 */
internal class RecordingQuranDao : InvocationHandler {
    val bookmarks = mutableListOf<BookmarkEntity>()
    val collections = mutableListOf<CollectionEntity>()
    val items = mutableListOf<CollectionItemEntity>()
    val sessions = mutableListOf<ReadingSessionEntity>()
    val recentlyRead = mutableListOf<RecentlyReadEntity>()
    val cache = LinkedHashMap<String, String>()

    val dao: QuranDao = Proxy.newProxyInstance(
        QuranDao::class.java.classLoader,
        arrayOf(QuranDao::class.java),
        this,
    ) as QuranDao

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
        val completion = args?.lastOrNull() as? Continuation<*>
        val call = args?.dropLast(if (completion == null) 0 else 1)
        val result: Any? = when (method.name) {
            "insertBookmark" -> { bookmarks += call!![0] as BookmarkEntity; Unit }
            "upsertBookmarkByVerseKey" -> { val row=call!![0] as BookmarkEntity;bookmarks.removeAll { it.verseKey==row.verseKey };bookmarks+=row;Unit }
            "deleteBookmark" -> { bookmarks.removeAll { it.verseKey==call!![0] };Unit }
            "insertCollection" -> { collections += call!![0] as CollectionEntity; Unit }
            "insertCollectionItem" -> { items += call!![0] as CollectionItemEntity; Unit }
            "insertReadingSession" -> { sessions += call!![0] as ReadingSessionEntity; Unit }
            "upsertRecentlyRead" -> { recentlyRead += call!![0] as RecentlyReadEntity; Unit }
            "insertCacheEntry" -> { val entry = call!![0] as ApiResponseCacheEntity; cache[entry.key] = entry.dataJson; Unit }
            "toString" -> return "RecordingQuranDao"
            "hashCode" -> return 0
            "equals" -> return false
            else -> null
        }
        if (completion == null) return result
        @Suppress("UNCHECKED_CAST")
        val completion2 = completion as Continuation<Any?>
        Thread { completion2.resumeWith(Result.success(result)) }.start()
        return COROUTINE_SUSPENDED
    }
}
