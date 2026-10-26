package com.nur.quran.data.api.rn

/**
 * Shared JVM fakes for the Rn API layer (constructor-injected; no new deps).
 * `unusedRnApiClient()` yields a facade whose flag is pinned OFF, so legacy
 * tests (the sync rig) can construct [com.nur.quran.data.sync.SyncService]
 * without touching the Render transport.
 */
internal class FakeRnTokenStore : RnTokenStore {
    var access: String? = null
    var refresh: String? = null
    var user: String? = null
    var email: String? = null

    override var accessToken: String?
        get() = access
        set(value) {
            access = value
        }

    override var refreshToken: String?
        get() = refresh
        set(value) {
            refresh = value
        }

    override var userId: String?
        get() = user
        set(value) {
            user = value
        }

    override var userEmail: String?
        get() = email
        set(value) {
            email = value
        }
}

/** Everything on the full surface throws unless a test overrides a member. */
internal open class FakeRnApi : RnApi {
    override suspend fun getChapters(): com.nur.quran.data.api.ChaptersResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getVerses(
        chapterId: Int,
        translation: Int,
        words: Int?,
        tajweed: Int?,
    ): com.nur.quran.data.api.VersesResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getWordsPack(chapterId: Int): com.nur.quran.data.api.VersesResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getSync(): Map<String, Any?> =
        throw UnsupportedOperationException("not under test")

    override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse =
        throw UnsupportedOperationException("not under test")
}

internal open class FakeRnAuthApi : RnAuthApi {
    override suspend fun register(body: RnRegisterRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun login(body: RnLoginRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun refresh(body: RnRefreshRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")
}

internal fun unusedAuthClient(store: RnTokenStore = FakeRnTokenStore()): RnAuthClient =
    RnAuthClient(FakeRnAuthApi(), store)

/** Fully disabled Rn facade for legacy (Appwrite-path) test rigs. */
internal fun disabledRnApiClient(
    store: RnTokenStore = FakeRnTokenStore(),
): RnApiClient = RnApiClient(
    backend = RnBackend().apply {
        injectedEnabled = false
        injectedBaseUrl = "https://rn.test/"
    },
    api = FakeRnApi(),
    authClient = unusedAuthClient(store),
    tokenStore = store,
)
