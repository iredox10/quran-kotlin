package com.nur.quran.data.hifdh

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Account-scoped append-only journal in local SQLite. Audio never enters sync payloads. */
class HifdhJournal(context: Context, private val owner: String) {
    private val db = database(context.applicationContext)
    private val gson = Gson()
    fun append(event: HifdhReviewEvent) = merge(listOf(event))
    fun merge(events: List<HifdhReviewEvent>) {
        var changed = false
        db.writableDatabase.apply {
            beginTransaction()
            try {
                events.filter { it.isValid }.forEach { event ->
                    val row = ContentValues().apply {
                        put("owner", owner); put("id", event.id); put("verse_key", event.verseKey)
                        put("stamp", event.timestamp); put("json", gson.toJson(event))
                    }
                    if (insertWithOnConflict("events", null, row, SQLiteDatabase.CONFLICT_IGNORE) != -1L) changed = true
                }
                setTransactionSuccessful()
            } finally { endTransaction() }
        }
        if (changed) { revision.value = revision.value + 1 }
    }
    fun events(limit: Int = 100_000): List<HifdhReviewEvent> = db.readableDatabase.query("events", arrayOf("json"),
        "owner=?", arrayOf(owner), null, null, "stamp DESC,id", limit.coerceIn(1, 100_000).toString()).use { cursor ->
        buildList { while (cursor.moveToNext()) runCatching { gson.fromJson(cursor.getString(0), HifdhReviewEvent::class.java) }
            .getOrNull()?.takeIf { it.isValid }?.let(::add) }
    }
    /** Bounded sync window. Complete event history remains local; duplicate pulls are harmless. */
    fun syncEnvelope(): Map<String, Map<String, Any?>> = events(500).associate { event ->
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
        event.id to gson.fromJson<Map<String, Any?>>(gson.toJson(event), type)
    }
    fun mergeEnvelope(value: Any?) {
        if (value !is Map<*, *>) return
        val rows = value.entries.mapNotNull { (id, raw) ->
            runCatching { gson.fromJson(gson.toJson(raw), HifdhReviewEvent::class.java) }.getOrNull()
                ?.takeIf { it.isValid && it.id == id }
        }
        merge(rows.sortedByDescending { it.timestamp }.take(10_000))
    }
    companion object {
        private val revision = MutableStateFlow(0L)
        val changes = revision.asStateFlow()
        @Volatile private var helper: SQLiteOpenHelper? = null
        @Synchronized private fun database(context: Context): SQLiteOpenHelper = helper ?: object : SQLiteOpenHelper(context, "hifdh_journal.db", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE events(owner TEXT NOT NULL,id TEXT NOT NULL,verse_key TEXT NOT NULL,stamp INTEGER NOT NULL,json TEXT NOT NULL,PRIMARY KEY(owner,id))")
                db.execSQL("CREATE INDEX events_owner_stamp ON events(owner,stamp)")
            }
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }.also { helper = it }
    }
}
