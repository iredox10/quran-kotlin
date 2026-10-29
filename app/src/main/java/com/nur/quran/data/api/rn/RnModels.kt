package com.nur.quran.data.api.rn

/**
 * Auth wire shapes of `https://qurannur-backend-d7d7312f118d.herokuapp.com` (quran-backend
 * Stream C contract — docs/contract-auth-sync.md):
 *
 *   POST /auth/register { email, password, name? }
 *   POST /auth/login    { email, password }
 *   POST /auth/refresh  { refreshToken }
 *   → 200 { user: { id, email, name? }, accessToken, refreshToken }
 *   errors: 409 register (email exists), 422 invalid input, 401 login/refresh.
 *
 * Sync-document shapes live in [RnSyncShapes]; content responses reuse the
 * api.quran.com-shaped DTOs in `com.nur.quran.data.api` — the RN content
 * contract mirrors exactly what this app consumes today.
 */
data class RnRegisterRequest(
    val email: String,
    val password: String,
    /** Optional per contract; omitted from the JSON when blank/null. */
    val name: String? = null,
)

data class RnLoginRequest(val email: String, val password: String)

data class RnRefreshRequest(val refreshToken: String)

data class RnAuthUser(
    val id: String?,
    val email: String?,
    val name: String? = null,
)

data class RnAuthResponse(
    val user: RnAuthUser? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

data class RnRecoveryRequest(val email: String)
data class RnRecoveryResponse(val message: String?)

/** Google tokens are sent only in HTTPS request bodies, never URLs or logs. */
data class RnGoogleRequest(val idToken: String, val nonce: String)
data class RnGoogleChallenge(val clientId: String, val nonce: String, val expiresAt: Long)
data class RnGoogleProvider(val enabled: Boolean = false)
data class RnAuthProviders(val google: RnGoogleProvider = RnGoogleProvider())
