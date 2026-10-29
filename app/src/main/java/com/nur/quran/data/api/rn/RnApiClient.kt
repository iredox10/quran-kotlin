package com.nur.quran.data.api.rn

import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton

/** Shared Heroku transport for authentication, Quran content, and cross-device sync. */
@Singleton
class RnApiClient @Inject constructor(
    private val backend: RnBackend,
    private val api: RnApi,
    private val authClient: RnAuthClient,
    private val tokenStore: RnTokenStore,
) {
    /** Normalized base URL ("https://qurannur-backend-d7d7312f118d.herokuapp.com/"). */
    val baseUrl: String get() = backend.baseUrl

    /** Default translation resource; callers can request their selected edition. */
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

    suspend fun refreshRejectedAccessToken(token: String): String? = authClient.refreshAccessToken(token)

    /** POST /auth/register — tokens + user persisted inline (single call). */
    suspend fun register(email: String, password: String, name: String = ""): RnAuthUser? =
        authClient.register(email, password, name).user

    /** POST /auth/login — tokens + user persisted. */
    suspend fun login(email: String, password: String): RnAuthUser? =
        authClient.login(email, password).user

    suspend fun googleProviders() = authClient.googleProviders()
    suspend fun googleChallenge() = authClient.googleChallenge()
    suspend fun signInWithGoogle(idToken: String, nonce: String): RnAuthUser? =
        authClient.signInWithGoogle(idToken, nonce).user

    suspend fun requestRecovery(email: String) = authClient.requestRecovery(email)

    suspend fun getFootnote(id: String) = api.getFootnote(id)

    /** Logout: forget the whole RN session (identity included). */
    fun clearSession() = authClient.clearSession()

    // ── content ──────────────────────────────────────────────────────────────

    /** `/v1/chapters` — the 114 api.quran.com-shaped chapter objects. */
    suspend fun getChapters(): com.nur.quran.data.api.ChaptersResponse = api.getChapters()

    /**
     * `/v1/verses/{ch}?translation={id}` — verses refresh path; [words] toggles
     * the optional per-word array, [tajweed] the optional verse-level markup.
     */
    suspend fun getVerses(
        chapterId: Int,
        words: Boolean = true,
        tajweed: Boolean = false,
        translation: Int = translationId,
        mushaf: Int = 5,
    ): com.nur.quran.data.api.VersesResponse {
        val verses = mutableListOf<com.nur.quran.data.api.ApiVerse>()
        val visited = mutableSetOf<Int>()
        var page = 1
        while (true) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(visited.add(page) && visited.size <= 1000) { "Invalid verses pagination" }
            val response = RnContent.normalizeVersesResponse(api.getVerses(
                chapterId = chapterId,
                translation = translation,
                words = if (words) 1 else 0,
                tajweed = if (tajweed) 1 else 0,
                page = page,
                perPage = 300,
                mushaf = mushaf,
            ))
            verses += response.verses
            page = response.pagination.next_page ?: break
        }
        return com.nur.quran.data.api.VersesResponse(
            verses, com.nur.quran.data.api.ApiPagination(1, null, 1)
        )
    }

    suspend fun getPage(page: Int, translation: Int = translationId, mushaf: Int = 5) =
        RnContent.normalizeVersesResponse(api.getPage(page, translation, mushaf))

    suspend fun getTafsirByChapter(tafsirId: Int, chapterId: Int) =
        api.getTafsirByChapter(tafsirId, chapterId)

    suspend fun getReciters() = api.getReciters()

    suspend fun completeSaukaAssignment(assignmentId: String) {
        require(hasSession()) { "Sign in to submit your group progress" }
        require(assignmentId.matches(Regex("[A-Za-z0-9._-]{1,64}"))) { "Invalid assignment" }
        api.completeSaukaAssignment(assignmentId)
    }

    /** `/v1/packs/words/{ch}.json` — bundle-shape single file, pagination-free. */
    suspend fun getWordsPack(chapterId: Int): com.nur.quran.data.api.VersesResponse =
        RnContent.parseWordsPack(api.getWordsPack(chapterId))

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
