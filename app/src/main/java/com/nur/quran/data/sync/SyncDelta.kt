package com.nur.quran.data.sync

import com.google.gson.Gson

/** Minimal mergeable changes: an unchanged library is not uploaded again. */
internal fun syncDelta(before: Map<String, Any?>, after: Map<String, Any?>): Map<String, Any?> {
    val keys = mapOf("bookmarks" to "verseKey", "collections" to "id", "planners" to "id",
        "hifdhGoals" to "id", "archivedPlanners" to "id", "readingSessions" to "timestamp",
        "recentlyRead" to "chapterId", "pomodoroHistory" to "completedAt", "assignments" to "dayNumber")
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
            val rows = if (field == "planners") changes.mapNotNull { row ->
                val old = previous[id(row)]
                if (old is Map<*, *> && row is Map<*, *>) {
                    val result = difference(old, row, "")
                    if (result === unchanged) return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val patch = mapOf("id" to row["id"]) + (result as Map<String, Any?>)
                    if (!equal(old["assignmentsUpdatedAt"], row["assignmentsUpdatedAt"]) && row["assignments"] is List<*>) {
                        patch + mapOf("assignments" to row["assignments"]) +
                            row.filterKeys { it in PLANNER_DAY_PROGRESS_FIELDS }.mapKeys { it.key.toString() }
                    } else if (patch.keys.any { it in PLANNER_DAY_PROGRESS_FIELDS } && row.containsKey("assignmentsUpdatedAt")) {
                        patch + ("assignmentsUpdatedAt" to row["assignmentsUpdatedAt"])
                    } else patch
                } else row
            } else changes
            return rows.takeIf { it.isNotEmpty() } ?: unchanged
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
