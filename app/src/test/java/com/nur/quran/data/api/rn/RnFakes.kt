package com.nur.quran.data.api.rn

/** JVM fakes for the shared backend; no network is used unless overridden. */
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
        page: Int,
        perPage: Int,
        mushaf: Int,
    ): com.nur.quran.data.api.VersesResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getWordsPack(chapterId: Int): com.google.gson.JsonObject =
        throw UnsupportedOperationException("not under test")

    override suspend fun getTafsirByChapter(tafsirId: Int, chapterId: Int): com.nur.quran.data.api.TafsirResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getPage(page: Int, translation: Int, mushaf: Int): com.nur.quran.data.api.VersesResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getFootnote(id: String): com.nur.quran.data.api.FootnoteResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun getReciters(): RnRecitersResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun completeSaukaAssignment(assignmentId: String): com.google.gson.JsonObject =
        throw UnsupportedOperationException("not under test")

    override suspend fun getSync(): Map<String, Any?> =
        throw UnsupportedOperationException("not under test")

    override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse =
        throw UnsupportedOperationException("not under test")
}

internal open class FakeRnAuthApi : RnAuthApi {
    override suspend fun providers(): RnAuthProviders = RnAuthProviders()
    override suspend fun googleChallenge(): RnGoogleChallenge =
        throw UnsupportedOperationException("not under test")
    override suspend fun google(body: RnGoogleRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun register(body: RnRegisterRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun login(body: RnLoginRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun requestRecovery(body: RnRecoveryRequest): RnRecoveryResponse =
        throw UnsupportedOperationException("not under test")

    override suspend fun refresh(body: RnRefreshRequest): RnAuthResponse =
        throw UnsupportedOperationException("not under test")
}

internal fun unusedAuthClient(store: RnTokenStore = FakeRnTokenStore()): RnAuthClient =
    RnAuthClient(FakeRnAuthApi(), store)

/** Signed-out backend facade for pure merge/apply test rigs. */
internal fun signedOutRnApiClient(
    store: RnTokenStore = FakeRnTokenStore(),
): RnApiClient = RnApiClient(
    backend = RnBackend().apply {
        injectedBaseUrl = "https://rn.test/"
    },
    api = FakeRnApi(),
    authClient = unusedAuthClient(store),
    tokenStore = store,
)
