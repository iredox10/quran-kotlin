package com.nur.quran.data.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Canonical list plus dated removals, so an offline snapshot cannot resurrect a bookmark. */
internal fun normalizeSyncLibrary(state: Map<String, Any?>): Map<String, Any?> {
    val out = state.toMutableMap()
    @Suppress("UNCHECKED_CAST")
    val removals = out["bookmarkRemovals"] as? Map<String, Any?> ?: emptyMap()
    @Suppress("UNCHECKED_CAST")
    val rows = (out["bookmarks"] as? List<Map<String, Any?>>)?.filter { row ->
        val deleted = (removals[row["verseKey"]] as? Number)?.toLong() ?: -1L
        val added = ((row["createdAt"] ?: row["timestamp"]) as? Number)?.toLong() ?: 0L
        deleted < added
    }
    if (rows != null) {
        out["bookmarks"] = rows
        if ("bookmark" in out) out["bookmark"] = rows.maxWithOrNull(compareBy<Map<String, Any?>> { ((it["createdAt"] ?: it["timestamp"]) as? Number)?.toLong() ?: 0L }.thenBy { it["verseKey"].toString() })
    }
    @Suppress("UNCHECKED_CAST")
    (out["recentlyRead"] as? List<Map<String, Any?>>)?.let { rows ->
        out["recentlyRead"] = rows.map { row ->
            val key = row["verseKey"] as? String
            if (key != null && key.substringBefore(":").toIntOrNull() != (row["chapterId"] as? Number)?.toInt()) row + ("verseKey" to null) else row
        }
    }
    return out
}

/** Legacy JSON mirrors double large planner/history payloads; canonical fields carry that data once. */
internal fun compactSyncPayload(state: Map<String, Any?>, context: Map<String, Any?> = state): Map<String, Any?> {
    val out = state.toMutableMap()
    if (context["bookmarks"] is List<*>) out.remove("bookmarksJson")
    if (context["collections"] is List<*>) out.remove("collectionsJson")
    if (context["readingSessions"] is List<*> && context["recentlyRead"] is List<*>) out.remove("readingStatsJson")
    if (context["planners"] is List<*>) {
        val legacy = (out["plansJson"] as? String)?.let { raw -> runCatching {
            Gson().fromJson<Map<String, Any?>>(raw, object : TypeToken<Map<String, Any?>>() {}.type)
        }.getOrNull() }
        if (legacy != null && legacy.keys.all { it in setOf("allPlans","activePlannerId","archivedPlans","activePlan","bookmarks","sessionTotals") }) {
            // Preserve fields older flat snapshots may not yet carry.
            for ((old, key) in mapOf("activePlannerId" to "activePlannerId", "archivedPlans" to "archivedPlanners", "bookmarks" to "plannerBookmarks")) {
                if (key !in context && legacy[old] != null) out[key] = legacy[old]
            }
            val active = legacy["activePlan"] as? Map<*, *>
            val plans = context["planners"] as List<*>
            if (active != null && plans.none { (it as? Map<*, *>)?.get("id") == active["id"] }) out["planners"] = plans + listOf(active)
            if ("plannerSessionTimers" !in context) {
                val totals = legacy["sessionTotals"] as? Map<*, *>
                if (!totals.isNullOrEmpty()) out["plannerSessionTimers"] = totals.mapKeys { it.key.toString() }.mapValues { (_, days) ->
                    (days as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { (_, seconds) ->
                        if (seconds is Number) mapOf("totalSeconds" to seconds, "startedAt" to 0L) else seconds
                    } ?: days
                }
            }
            out.remove("plansJson")
        }
    }
    val plans = (context["planners"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.associateBy { it["id"] }
    (out["planners"] as? List<*>)?.let { rows ->
        out["planners"] = rows.map { row ->
            if (row !is Map<*, *> || row.containsKey("assignmentsUpdatedAt") ||
                row.keys.none { it in PLANNER_DAY_PROGRESS_FIELDS }) return@map row
            val version = plans?.get(row["id"])?.get("assignmentsUpdatedAt") as? Number
            if (version != null) row + ("assignmentsUpdatedAt" to version) else row
        }
    }
    return out
}
