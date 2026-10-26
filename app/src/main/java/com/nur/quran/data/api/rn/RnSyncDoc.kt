package com.nur.quran.data.api.rn

/**
 * `/v1/sync` wire shapes (docs/contract-auth-sync.md). The SYNC DOCUMENT
 * contract field names are the app's own `com.nur.quran.data.sync.SyncContract.kt`
 * (source of truth): `userId` + `stateData` (stateData = the JSON string of the
 * whole syncable state), the document timestamp being the server's ISO-8601
 * `$updatedAt` parity. No other attributes are written — exactly what the
 * Appwrite collection carried.
 */
object RnSyncShapes {
    /** Lookup key: the only sync-document attribute queried by. */
    const val FIELD_USER_ID = "userId"

    /** JSON string of the whole syncable state; the only other attribute. */
    const val FIELD_STATE_DATA = "stateData"
}

/**
 * PUT /v1/sync body — byte-identical to
 * [com.nur.quran.data.sync.SyncContract.buildPushPayload] (`userId` +
 * `stateData`, nothing else).
 */
data class RnSyncPushPayload(
    val userId: String,
    val stateData: String,
)

/**
 * PUT /v1/sync response `{ lastSyncAt }` — the upsert's server timestamp
 * (`updatedAt` tolerated for the symmetric GET field name; ISO-8601 or epoch
 * ms; anywhere in that envelope).
 */
data class RnSyncPutResponse(
    val lastSyncAt: Any? = null,
    val updatedAt: Any? = null,
)

/**
 * The user's one sync document after the tolerant GET /v1/sync envelope parse
 * ([RnSyncEnvelope]). A missing document surfaces as null — the caller keeps
 * Appwrite's canary/empty-pull semantics.
 */
data class RnSyncDoc(
    val userId: String?,
    /** JSON string of the whole syncable state (SyncContract FIELD_STATE_DATA). */
    val stateData: String?,
    /** Server stamp epoch ms; 0 when the response carried nothing parseable. */
    val updatedAtMs: Long,
)

/**
 * Tolerant GET /v1/sync response mapping. The contract freezes the sync DOC
 * fields but leaves the outer envelope minimal ("the user's sync doc …
 * minimal envelope") — denormalize nested `doc`/`document`/`data` wrappers
 * when one is present, top-level otherwise. Pure so envelope decisions are
 * unit-testable against whatever the backend ships.
 */
object RnSyncEnvelope {
    private const val FIELD_USER_ID = RnSyncShapes.FIELD_USER_ID
    private const val FIELD_STATE_DATA = RnSyncShapes.FIELD_STATE_DATA

    val TIMESTAMP_FIELDS = listOf("updatedAt", "lastSyncAt")
    private val DOC_WRAPPERS = listOf("doc", "document", "sync", "data", "userSync")

    fun fromMap(map: Map<String, Any?>?): RnSyncDoc? {
        if (map == null) return null
        val doc = findDoc(map)
        val stateData = doc[FIELD_STATE_DATA]?.toString()
        // No stateData AND no timestamp anywhere: not a sync document (an error
        // envelope or a 200-empty canary — pull-on-empty semantics stay with the
        // caller).
        if (stateData == null && !TIMESTAMP_FIELDS.any { doc.containsKey(it) }) return null
        val userId = doc[FIELD_USER_ID]?.toString() ?: map[FIELD_USER_ID]?.toString()
        val timestamp = TIMESTAMP_FIELDS.firstNotNullOfOrNull { doc[it] ?: map[it] }
        return RnSyncDoc(userId, stateData, RnTime.parse(timestamp))
    }

    /** Top-level state wins; else the first nested wrapper carrying stateData/timestamps. */
    private fun findDoc(map: Map<String, Any?>): Map<String, Any?> {
        fun hasDocFields(m: Map<*, *>) =
            m[FIELD_STATE_DATA] != null || TIMESTAMP_FIELDS.any { m.containsKey(it) }
        if (hasDocFields(map)) return map
        for (key in DOC_WRAPPERS) {
            val nested = map[key] as? Map<*, *> ?: continue
            if (hasDocFields(nested)) {
                @Suppress("UNCHECKED_CAST")
                return nested as Map<String, Any?>
            }
        }
        return map
    }
}
