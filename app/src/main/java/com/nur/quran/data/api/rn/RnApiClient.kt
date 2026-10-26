package com.nur.quran.data.api.rn

import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level facade over the quran-nur-api surface — the single dependency
 * every flag-gated consumer holds ([com.nur.quran.data.sync.SyncService],
 * [com.nur.quran.data.sync.SyncRealtime], word packs, verses refresh, auth
 * UIs). [isEnabled] is the one switch: when it is false the consumers never
 * call into this class, so legacy paths stay byte-identical.
 *
 * Auth delegates to [RnAuthClient]; content methods normalize the wire
 * responses ([RnContent]) so callers read the same DTO structures they get
 * from api.quran.com; the sync-document methods (added with the sync client)
 * follow the app's own `SyncContract.kt` field names (userId/stateData) per
 * the auth/sync contract.
 */
@Singleton
class RnApiClient @Inject constructor(
    private val backend: RnBackend,
    private val api: RnApi,
    private val authClient: RnAuthClient,
    private val tokenStore: RnTokenStore,
) {
    /** The migration switch — every consumer's first branch. */
    val isEnabled: Boolean get() = backend.isRenderApiEnabled

    /** Normalized base URL ("https://quran-nur-api.onrender.com/"). */
    val baseUrl: String get() = backend.baseUrl

    /** The pinned translation resource (content contract — `translation=20`). */
    val translationId: Int get() = RnBackend.TRANSLATION_ID

    // ── session (fix/login-restore-pull: identity is persisted) ──────────────

    /** Persisted account id; null while signed out. */
    fun storedUserId(): String? {
        if (!tokenStore.hasSession()) return null
        return tokenStore.userId?.takeIf { it.isNotBlank() }
    }

    fun storedUserEmail(): String? = tokenStore.userEmail?.takeIf { it.isNotBlank() }

    fun hasSession(): Boolean = tokenStore.hasSession()

    /** Access token usable for a request now (refreshes when absent). */
    suspend fun ensureValidAccessToken(): String? = authClient.ensureValidAccessToken()

    /** POST /auth/register — tokens + user persisted inline (single call). */
    suspend fun register(email: String, password: String, name: String = ""): RnAuthUser? =
        authClient.register(email, password, name).user

    /** POST /auth/login — tokens + user persisted. */
    suspend fun login(email: String, password: String): RnAuthUser? =
        authClient.login(email, password).user

    /** Logout: forget the whole RN session (identity included). */
    fun clearSession() = authClient.clearSession()

    // ── content ──────────────────────────────────────────────────────────────

    /** `/v1/chapters` — the 114 api.quran.com-shaped chapter objects. */
    suspend fun getChapters(): com.nur.quran.data.api.ChaptersResponse = api.getChapters()

    /**
     * `/v1/verses/{ch}?translation=20` — verses refresh path; [words] toggles
     * the optional per-word array, [tajweed] the optional verse-level markup.
     */
    suspend fun getVerses(chapterId: Int, words: Boolean = true, tajweed: Boolean = false) =
        RnContent.normalizeVersesResponse(
            api.getVerses(
                chapterId = chapterId,
                translation = translationId,
                words = if (words) 1 else null,
                tajweed = if (tajweed) 1 else null,
            ),
        )

    /** `/v1/packs/words/{ch}.json` — bundle-shape single file, pagination-free. */
    suspend fun getWordsPack(chapterId: Int): com.nur.quran.data.api.VersesResponse =
        RnContent.normalizeVersesResponse(api.getWordsPack(chapterId))

    // ── sync document (field names 1:1 from the app's SyncContract.kt) ────────

    /**
     * GET /v1/sync → the user's one sync document, or null when none exists /
     * the payload isn't a sync doc (canary/empty-pull semantics stay with the
     * caller, exactly like the Appwrite "no document" case).
     */
    suspend fun getSyncDoc(): com.nur.quran.data.api.rn.RnSyncDoc? =
        RnSyncEnvelope.fromMap(api.getSync())

    /**
     * PUT /v1/sync with the [com.nur.quran.data.sync.SyncContract.buildPushPayload]
     * map (userId + stateData) — the same map Appwrite received. Returns the
     * server stamp (epoch ms) for the sync watermark, or null on protocol noise.
     */
    suspend fun putSyncDoc(payload: Map<String, Any>): Long? {
        val response = api.putSync(
            RnSyncPushPayload(
                userId = payload[RnSyncShapes.FIELD_USER_ID]?.toString().orEmpty(),
                stateData = payload[RnSyncShapes.FIELD_STATE_DATA]?.toString().orEmpty(),
            ),
        )
        return RnTime.parse(response.lastSyncAt ?: response.updatedAt).takeIf { it > 0 }
    }
}
