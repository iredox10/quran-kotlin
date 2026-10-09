package com.nur.quran.data.sync

/**
 * The Kotlin↔web sync document contract — web truth is `quran-app/src/services/appwrite.js`
 * (`pushState` / `pullState`); the collection schema (`userId` + `stateData` only) is
 * `quran-app/scripts/setup-appwrite-new.js`.
 *
 * Exactly ONE document per account, found by `Query.equal("userId", userId)`: the id is
 * server-generated (`unique()` on create) and never derivable from the account id, which is
 * why old Android builds (documentId == userId) ended up with a second document per
 * account. The only attributes the web app can see are `userId` and `stateData` (a JSON
 * string of the syncable state); the flattened attributes those builds also wrote
 * (`theme`/`bookmarksJson`/`version`/`updatedAt`, …) are not in the schema, so they are
 * never written — but they are still read, so such documents migrate in place.
 * The document timestamp is the server's `$updatedAt`, and it is the sync watermark.
 *
 * Pure by design (no Appwrite/Android types, no I/O) so it is unit-testable on the JVM;
 * the transport glue lives in [AppwriteClient] and [SyncService].
 */
object SyncContract {
    /** Lookup key: the only attribute documents are queried by. */
    const val FIELD_USER_ID = "userId"

    /** JSON string of the whole syncable state; the only other attribute written. */
    const val FIELD_STATE_DATA = "stateData"

    /** Magic id the server replaces with a generated one (same value the web client sends). */
    const val UNIQUE_DOCUMENT_ID = "unique()"

    /** Document bookkeeping — never an attribute, never part of the state. */
    private val RESERVED_FIELDS = setOf(FIELD_USER_ID, FIELD_STATE_DATA, "version", "updatedAt")

    /** Minimal document view, so resolution stays free of Appwrite SDK types. */
    data class DocRef(
        val id: String,
        val data: Map<String, Any?>,
        /** Server stamp (`$updatedAt` in epoch ms); 0 when unparseable. */
        val updatedAtMillis: Long = 0L,
    )

    /** What a stored document carries: web state JSON, and/or legacy flattened attributes. */
    data class DocPayload(val stateDataRaw: String?, val legacyFields: Map<String, Any?>)

    /**
     * This account's document out of a `Query.equal("userId", userId)` result. The server
     * already filters, so the first row is the usual answer; a row that actually carries the
     * account id wins when unlabelled legacy rows are present.
     * Null = the account has no document: create one, never guess an id.
     */
    fun resolveDoc(documents: List<DocRef>, userId: String): DocRef? {
        if (documents.isEmpty()) return null
        return documents.firstOrNull { it.data[FIELD_USER_ID]?.toString() == userId }
            ?: documents.first()
    }

    /**
     * `stateData` when present (web shape), else the flattened attributes of a legacy
     * Android document — which the next push migrates behind `stateData`.
     */
    fun readDocPayload(data: Map<String, Any?>): DocPayload {
        val raw = data[FIELD_STATE_DATA]?.toString()?.takeIf { it.isNotBlank() }
        return DocPayload(stateDataRaw = raw, legacyFields = stripStateMetadata(data))
    }

    /**
     * Full state resolution. `stateData` wins when it parses (even to an empty map, as in
     * web); legacy attributes are used when it is missing or unparseable.
     */
    fun stateFromDoc(
        data: Map<String, Any?>,
        parseJsonObject: (String) -> Map<String, Any?>?,
    ): Map<String, Any?> {
        val payload = readDocPayload(data)
        return payload.stateDataRaw?.let(parseJsonObject) ?: payload.legacyFields
    }

    /**
     * Pull gate (web parity, `CloudSync.jsx` `performPull`):
     * `remote.updatedAt > lastSyncAt`, and a zero watermark means "never synced here" —
     * the web app's login-time force-apply.
     *
     * No local clock may enter this comparison: a locally built snapshot stamps itself with
     * "now", so a local timestamp in the watermark always outranks the remote document and
     * the pull can never fire.
     */
    fun shouldPull(remoteUpdatedAt: Long, lastSyncAt: Long): Boolean {
        if (lastSyncAt <= 0L) return remoteUpdatedAt > 0L
        return remoteUpdatedAt > lastSyncAt
    }

    /** The entire write payload: `userId` + `stateData`, nothing else. */
    fun buildPushPayload(userId: String, stateData: String): Map<String, Any> =
        linkedMapOf(FIELD_USER_ID to userId, FIELD_STATE_DATA to stateData)

    /**
     * Drop document bookkeeping: `version`/`updatedAt` describe the document (whose own
     * `$updatedAt` is authoritative), and `stateData` would nest the state inside itself.
     */
    fun stripStateMetadata(state: Map<String, Any?>): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>(state.size)
        for ((key, value) in state) if (key !in RESERVED_FIELDS) out[key] = value
        return out
    }
}
