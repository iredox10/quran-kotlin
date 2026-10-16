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

/**
 * Deterministic JSON text used for value identity in `mergeValueArrays`
 * (web uses JSON.stringify) and for payload size accounting in
 * `slimStatePayload`. Object key order is preserved, matching JS.
 */
internal fun jsonString(value: Any?): String = StringBuilder().also { writeJson(it, value) }.toString()

private fun writeJson(sb: StringBuilder, value: Any?) {
    when (value) {
        null -> sb.append("null")
        true -> sb.append("true")
        false -> sb.append("false")
        is String -> writeJsonString(sb, value)
        is Number -> sb.append(formatNumber(value))
        is List<*> -> {
            sb.append('[')
            value.forEachIndexed { i, item ->
                if (i > 0) sb.append(',')
                writeJson(sb, item)
            }
            sb.append(']')
        }
        is Map<*, *> -> {
            sb.append('{')
            var first = true
            value.forEach { (k, v) ->
                if (!first) sb.append(',')
                first = false
                writeJsonString(sb, k.toString())
                sb.append(':')
                writeJson(sb, v)
            }
            sb.append('}')
        }
        else -> sb.append("null")
    }
}

/** JS JSON.stringify number formatting: integral doubles without ".0". */
private fun formatNumber(n: Number): String {
    val d = n.toDouble()
    if (!d.isFinite()) return "null"
    if (d == 0.0) return "0"
    if (d == Math.floor(d) && Math.abs(d) < 1e21) return d.toLong().toString()
    return d.toString()
}

private fun writeJsonString(sb: StringBuilder, s: String) {
    sb.append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
        }
    }
    sb.append('"')
}

/** JS mergeValue: undefined skips, null overwrites, objects deep-merge. */
private fun mergeValue(base: Any?, incoming: Any?): Any? {
    if (incoming == null) return base
    if (base == null) return incoming
    if (base is List<*> && incoming is List<*>) return mergeValueArrays(base, incoming)
    if (isPlainObject(base) && isPlainObject(incoming)) {
        val out = LinkedHashMap<String, Any?>()
        (base as Map<*, *>).forEach { (k, v) -> out[k.toString()] = v }
        (incoming as Map<*, *>).forEach { (k, v) -> out[k.toString()] = mergeValue(out[k], v) }
        return out
    }
    return incoming
}

/** JS hasDayNumber: plain object carrying a dayNumber key (null counts). */
private fun hasDayNumber(item: Any?): Boolean = isPlainObject(item) && (item as Map<*, *>).containsKey("dayNumber")

private fun isDayNumberArray(v: List<*>): Boolean = v.isNotEmpty() && v.all(::hasDayNumber)

/**
 * JS mergeValueArrays: dayNumber arrays union per dayNumber with the
 * latest updatedAt winning (ties go to the later item, `>=`); every other
 * array unions by JSON value identity, incoming replacing base in place.
 */
private fun mergeValueArrays(base: List<*>, incoming: List<*>): List<Any?> {
    if (isDayNumberArray(base) && isDayNumberArray(incoming)) {
        val map = LinkedHashMap<Any?, Any?>()
        (base + incoming).forEach { item ->
            val day = normalizeIdentity(fieldOf(item, "dayNumber"))
            val existing = map[day]
            if (existing == null || numberFiniteOrZero(fieldOf(item, "updatedAt")) >=
                numberFiniteOrZero(fieldOf(existing, "updatedAt"))
            ) {
                map[day] = item
            }
        }
        return map.values.toList()
    }
    val map = LinkedHashMap<String, Any?>()
    base.forEach { map[jsonString(it)] = it }
    incoming.forEach { map[jsonString(it)] = it }
    return map.values.toList()
}

/** JS mergeItem: recentlyRead keeps the newest timestamp per chapterId. */
private fun mergeItem(baseItem: Any?, incomingItem: Any?, field: String): Any? {
    if (baseItem == null) return incomingItem
    if (field == "recentlyRead") {
        val a = timestampOrZero(fieldOf(baseItem, "timestamp"))
        val b = timestampOrZero(fieldOf(incomingItem, "timestamp"))
        return if (b >= a) incomingItem else baseItem
    }
    return incomingItem
}

/**
 * JS mergeArray: identity-keyed union (dedupe, latest wins per key) with
 * post caps — recentlyRead sorted newest-first then trimmed to 5,
 * readingSessions/pomodoroHistory keep the last 500.
 */
private fun mergeArray(base: Any?, incoming: Any?, field: String): List<Any?> {
    val key = ARRAY_KEY[field]
    val baseList = (base as? List<*>) ?: emptyList<Any?>()
    val incList = (incoming as? List<*>) ?: emptyList<Any?>()
    if (key == null) {
        // No identity key: union by value itself, exactly like web mergeArray
        // (JS Map keyed by the item; primitives dedupe by value). Plain
        // concatenation would duplicate shared entries on every sync.
        val seen = LinkedHashMap<Any?, Any?>()
        (baseList + incList).forEach { seen[it] = it }
        return seen.values.toList()
    }
    val map = LinkedHashMap<Any?, Any?>()
    baseList.forEach { map[identityOf(it, key)] = it }
    incList.forEach { item ->
        val id = identityOf(item, key)
        map[id] = mergeItem(map[id], item, field)
    }
    var merged = map.values.toList()
    if (field == "recentlyRead") {
        merged = merged.sortedByDescending { timestampOrZero(fieldOf(it, "timestamp")) }.take(RECENTLY_READ_CAP)
    }
    if (field == "readingSessions" || field == "pomodoroHistory") merged = merged.takeLast(SESSION_CAP)
    return merged
}

/** JS mergeCollections: union by id, items keyed by verseKey only. */
private fun mergeCollections(base: Any?, incoming: Any?): List<Any?> {
    val map = LinkedHashMap<Any?, Any?>()
    (base as? List<*>)?.forEach { map[identityOf(it, "id")] = copyMap(it) }
    (incoming as? List<*>)?.forEach { inc ->
        val id = identityOf(inc, "id")
        val existing = map[id]
        if (existing == null || !isPlainObject(existing)) {
            map[id] = copyMap(inc)
            return@forEach
        }
        val items = LinkedHashMap<Any?, Any?>()
        listOfNotNull(fieldOf(existing, "items"), fieldOf(inc, "items")).forEach { itemsValue ->
            (itemsValue as? List<*>)?.forEach { item ->
                items[identityOf(item, "verseKey")] = item
            }
        }
        val out = LinkedHashMap<String, Any?>()
        (existing as Map<*, *>).forEach { (k, v) -> out[k.toString()] = v }
        out["items"] = items.values.toList()
        map[id] = out
    }
    return map.values.toList()
}

/** JS mergePlanners: union by id, then a full deep merge of both plans. */
private fun mergePlanners(base: Any?, incoming: Any?): List<Any?> {
    val map = LinkedHashMap<Any?, Any?>()
    (base as? List<*>)?.forEach { map[identityOf(it, "id")] = it }
    (incoming as? List<*>)?.forEach { inc ->
        val id = identityOf(inc, "id")
        val existing = map[id]
        map[id] = if (existing == null) inc else mergeValue(existing, inc)
    }
    return map.values.toList()
}

/** JS pageVisitCounts: per-key max with negatives and junk clamped to 0. */
private fun mergePageVisitCounts(base: Any?, incoming: Any?): Map<String, Any?> {
    val counts = LinkedHashMap<String, Any?>()
    (base as? Map<*, *>)?.forEach { (k, v) -> counts[k.toString()] = v }
    val keys = LinkedHashSet<String>()
    counts.keys.forEach { keys.add(it) }
    (incoming as? Map<*, *>)?.keys?.forEach { keys.add(it.toString()) }
    keys.forEach { k ->
        counts[k] = maxOf(visitCount(counts[k]), visitCount((incoming as? Map<*, *>)?.get(k)))
    }
    return counts
}

/** Single-field merge (same rules as [mergeStateInto], without planner re-derivation). */
fun mergeField(field: String, existing: Any?, value: Any?): Any? = when {
    field == "recentlyRead" -> mergeArray(existing, value, field)
    field == "collections" -> mergeCollections(existing, value)
    field == "planners" -> mergePlanners(existing, value)
    field == "pageVisitCounts" -> mergePageVisitCounts(existing, value)
    ARRAY_KEY.containsKey(field) -> mergeArray(existing, value, field)
    field in DEEP_MERGE_FIELDS -> mergeValue(
        existing.takeIf { isPlainObject(it) } ?: emptyMap<String, Any?>(),
        value
    )
    value is List<*> && existing is List<*> -> mergeValueArrays(existing, value)
    isPlainObject(value) && isPlainObject(existing) -> mergeValue(existing, value)
    else -> value
}

/**
 * Merge `incoming` state into `base` state (web mergeStateInto):
 * identity-keyed lists union, keyed maps deep-merge, pageVisitCounts
 * take a per-key max, scalars/positions let the incoming value win, and
 * `planner` is re-derived from `planners` + `activePlannerId`.
 */
fun mergeStateInto(base: Any?, incoming: Any?): Any? {
    if (!isPlainObject(base) || !isPlainObject(incoming)) {
        return if (truthy(incoming)) incoming else base
    }
    val out = LinkedHashMap<String, Any?>()
    (base as Map<*, *>).forEach { (k, v) -> out[k.toString()] = v }
    (incoming as Map<*, *>).forEach { (key, value) ->
        val field = key.toString()
        out[field] = mergeField(field, out[field], value)
    }
    val planners = out["planners"]
    if (planners is List<*>) {
        val activeId = out["activePlannerId"]
        val current = out["planner"]
        val byActive = planners.firstOrNull { isPlainObject(it) && fieldOf(it, "id") == activeId }
        val byCurrent = if (byActive == null && isPlainObject(current)) {
            planners.firstOrNull { isPlainObject(it) && fieldOf(it, "id") == fieldOf(current, "id") }
        } else {
            null
        }
        out["planner"] = byActive ?: byCurrent ?: current
    }
    return out
}

/** JS clampCount: trunc, clamp to 0..10000, drop non-numeric values. */
private fun clampCount(v: Any?): Double? {
    val n = when (v) {
        null, false -> 0.0
        true -> 1.0
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: return null
        else -> return null
    }
    if (!n.isFinite()) return null
    val truncated = if (n >= 0) Math.floor(n) else Math.ceil(n)
    return minOf(10000.0, maxOf(0.0, truncated))
}

/** JS dedupeAssignments: one entry per dayNumber, strictly newer updatedAt wins. */
private fun dedupeAssignments(items: List<*>): List<Any?> {
    val map = LinkedHashMap<Any?, Any?>()
    items.forEach { item ->
        val day = normalizeIdentity(fieldOf(item, "dayNumber"))
        val existing = map[day]
        if (existing == null || numberFiniteOrZero(fieldOf(item, "updatedAt")) >
            numberFiniteOrZero(fieldOf(existing, "updatedAt"))
        ) {
            map[day] = item
        }
    }
    return map.values.toList()
}

/** JS slimDeep: copy-on-write walk that dedupes assignment arrays in place. */
private fun slimDeep(value: Any?): Any? {
    if (value is List<*>) return value.map(::slimDeep)
    if (!isPlainObject(value)) return value
    val out = LinkedHashMap<String, Any?>()
    (value as Map<*, *>).forEach { (k, v) ->
        val key = k.toString()
        out[key] = if (key == "assignments" && v is List<*> && isDayNumberArray(v)) {
            dedupeAssignments(v)
        } else {
            slimDeep(v)
        }
    }
    return out
}

private fun trimTail(state: MutableMap<String, Any?>, field: String, keep: Int) {
    val list = state[field]
    if (list is List<*> && list.size > keep) state[field] = list.takeLast(keep)
}

/**
 * Web slimStatePayload: drop the derived `planner`, clamp pageVisitCounts,
 * dedupe assignment arrays, and if the JSON payload still exceeds 950k
 * chars trim sessions to their last 100 entries.
 */
fun slimStatePayload(state: Map<String, Any?>): Map<String, Any?> {
    val result = LinkedHashMap<String, Any?>()
    state.forEach { (k, v) -> if (k.toString() != "planner") result[k.toString()] = v }

    val counts = result["pageVisitCounts"]
    if (isPlainObject(counts)) {
        val clamped = LinkedHashMap<String, Any?>()
        (counts as Map<*, *>).forEach { (k, v) ->
            val n = clampCount(v)
            if (n != null) clamped[k.toString()] = n
        }
        result["pageVisitCounts"] = clamped
    }

    val slimmed = slimDeep(result) as MutableMap<String, Any?>
    var size = jsonString(slimmed).length
    if (size > PAYLOAD_SIZE_LIMIT) {
        trimTail(slimmed, "readingSessions", SLIM_SESSION_CAP)
        trimTail(slimmed, "pomodoroHistory", SLIM_SESSION_CAP)
        size = jsonString(slimmed).length
        if (size > PAYLOAD_SIZE_LIMIT) {
            throw IllegalStateException("sync payload too large: $size chars")
        }
    }
    return slimmed
}
