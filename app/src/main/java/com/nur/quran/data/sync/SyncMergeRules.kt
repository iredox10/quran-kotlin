package com.nur.quran.data.sync

/**
 * Pure, JVM-testable port of the web sync truth:
 * quran-app/src/utils/syncMerge.js (mergeStateInto / slimStatePayload).
 *
 * Operates on JSON-ish values only: Map<String, Any?> for objects,
 * List<Any?> for arrays, String / Number / Boolean / null for scalars.
 * No Appwrite, Room or Android dependencies live here on purpose so the
 * merge semantics can be unit-tested on the JVM and stay in lockstep
 * with the web implementation.
 */
private val ARRAY_KEY: Map<String, String?> = mapOf(
    "bookmarks" to "verseKey",
    "memorizedAyahs" to null,
    "memorizedSurahs" to null,
    "downloadedSurahs" to null,
    "completedTours" to null,
    "dismissedCoachmarks" to null,
    "dismissedGestureTips" to null,
    "hifdhGoals" to "id",
    "archivedPlanners" to "id",
    "pomodoroHistory" to "completedAt",
    "readingSessions" to "timestamp",
    "recentlyRead" to "chapterId"
)

/** Maps that deep-merge recursively instead of array/scalar handling. */
private val DEEP_MERGE_FIELDS: Set<String> = setOf(
    "plannerBookmarks",
    "plannerReflections",
    "plannerSessionTimers",
    "saukaProgress",
    "hifdhHistory",
    "offlinePackStatus"
)

private const val PAYLOAD_SIZE_LIMIT = 950_000
private const val SESSION_CAP = 500
private const val SLIM_SESSION_CAP = 100
private const val RECENTLY_READ_CAP = 5

/** JS isPlainObject: non-null object that is not an array. */
internal fun isPlainObject(v: Any?): Boolean = v is Map<*, *>

/** JS truthiness used by `mergeStateInto`'s short circuit. */
private fun truthy(v: Any?): Boolean = when (v) {
    null, false -> false
    is String -> v.isNotEmpty()
    is Number -> {
        val d = v.toDouble()
        d != 0.0 && !d.isNaN()
    }
    else -> true
}

/** Shallow copy so merged results never alias the caller's maps. */
private fun copyMap(value: Any?): Any? {
    if (value !is Map<*, *>) return value
    val out = LinkedHashMap<String, Any?>()
    value.forEach { (k, v) -> out[k.toString()] = v }
    return out
}

/** JS `Number(v)` with NaN/Infinity collapsing to 0 (finite check). */
private fun numberFiniteOrZero(v: Any?): Double = when (v) {
    null, false -> 0.0
    true -> 1.0
    is Number -> v.toDouble().takeIf { it.isFinite() } ?: 0.0
    is String -> v.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
    else -> 0.0
}

/** JS `Number(v)`; 0 stays 0, NaN coerces to 0. */
private fun visitCount(v: Any?): Double = numberFiniteOrZero(v).let { if (it > 0) it else 0.0 }

/** JS `field.timestamp || 0` semantics. */
private fun timestampOrZero(v: Any?): Double = if (truthy(v)) numberFiniteOrZero(v) else 0.0

/**
 * JS identity: keys are compared with SameValueZero, so Int 1 / Long 1 /
 * Double 1.0 from different JSON parsers must collapse onto one identity.
 */
private fun normalizeIdentity(v: Any?): Any? = if (v is Number) v.toDouble() else v

/** JS `identityOf(item, key)`: null key means the item itself. */
private fun identityOf(item: Any?, key: String?): Any? = when {
    key == null -> item
    item is Map<*, *> -> normalizeIdentity(item[key])
    else -> null
}

private fun fieldOf(item: Any?, key: String): Any? = (item as? Map<*, *>)?.get(key)
