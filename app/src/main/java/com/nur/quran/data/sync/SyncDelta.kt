package com.nur.quran.data.sync

import com.google.gson.Gson

/** Minimal mergeable changes: an unchanged library is not uploaded again. */
internal fun syncDelta(before: Map<String, Any?>, after: Map<String, Any?>): Map<String, Any?> {
    val keys = mapOf("bookmarks" to "verseKey", "collections" to "id", "planners" to "id",
        "hifdhGoals" to "id", "archivedPlanners" to "id", "readingSessions" to "timestamp",
        "recentlyRead" to "chapterId", "pomodoroHistory" to "completedAt")
    val gson = Gson()
    val unchanged = Any()
    fun equal(a: Any?, b: Any?): Boolean = gson.toJsonTree(a) == gson.toJsonTree(b)
    fun difference(base: Any?, incoming: Any?, field: String): Any? {
        if (equal(base, incoming)) return unchanged
        if (base is List<*> && incoming is List<*>) {
            fun id(row: Any?): Any? {
                val value = keys[field]?.let { (row as? Map<*, *>)?.get(it) } ?: row
                return if (value is Number) value.toDouble() else if (keys.containsKey(field)) value else gson.toJson(value)
            }
            val previous = base.associateBy(::id)
            val changes = incoming.filter { !previous.containsKey(id(it)) || !equal(previous[id(it)], it) }
            return changes.takeIf { it.isNotEmpty() } ?: unchanged
        }
        if (base is Map<*, *> && incoming is Map<*, *>) {
            val changes = linkedMapOf<String, Any?>()
            for ((key, value) in incoming) {
                val result = difference(base[key], value, key.toString())
                if (result !== unchanged) changes[key.toString()] = result
            }
            return changes.takeIf { it.isNotEmpty() } ?: unchanged
        }
        return incoming
    }
    @Suppress("UNCHECKED_CAST")
    return (difference(before, after, "") as? Map<String, Any?>) ?: emptyMap()
}
