package com.nur.quran.data.api.rn

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Persisted RN session (tokens + account identity). Store pattern: the app's
 * own SharedPreferences (same as `sync_state` / `word_packs`) — the only
 * secret-persistence layer the project has; the Appwrite SDK used the same
 * mechanism under the hood for its session document.
 *
 * `userId` persistence is the `fix/login-restore-pull` semantic: the account
 * id survives app restarts, so [com.nur.quran.data.sync.SyncService] can
 * resolve the sync document without a network round trip, and a 401 on an
 * expired access token only needs the refresh token to recover.
 */
interface RnTokenStore {
    var accessToken: String?
    var refreshToken: String?
    var userId: String?
    var userEmail: String?

    /** A session the app can resume (tokens + identity persisted). */
    fun hasSession(): Boolean = !userId.isNullOrBlank() && !refreshToken.isNullOrBlank()

    /** Clear tokens AND identity — the logout semantic. */
    fun clearSession() {
        accessToken = null
        refreshToken = null
        userId = null
        userEmail = null
    }
}

private const val PREFS_NAME = "rn_auth"
private const val KEY_ACCESS = "access_token"
private const val KEY_REFRESH = "refresh_token"
private const val KEY_USER_ID = "user_id"
private const val KEY_USER_EMAIL = "user_email"

/** SharedPreferences implementation consumed by Hilt (see di/RnModule). */
class RnPrefsTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    backend: RnBackend,
) : RnTokenStore {
    private val originKey = java.security.MessageDigest.getInstance("SHA-256")
        .digest(backend.baseUrl.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private val prefs = context.getSharedPreferences("${PREFS_NAME}_$originKey", Context.MODE_PRIVATE)

    private fun get(key: String): String? =
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }

    private fun set(key: String, value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(key) else putString(key, value)
        }.apply()
    }

    override var accessToken: String?
        get() = get(KEY_ACCESS)
        set(value) = set(KEY_ACCESS, value)

    override var refreshToken: String?
        get() = get(KEY_REFRESH)
        set(value) = set(KEY_REFRESH, value)

    override var userId: String?
        get() = get(KEY_USER_ID)
        set(value) = set(KEY_USER_ID, value)

    override var userEmail: String?
        get() = get(KEY_USER_EMAIL)
        set(value) = set(KEY_USER_EMAIL, value)
}
