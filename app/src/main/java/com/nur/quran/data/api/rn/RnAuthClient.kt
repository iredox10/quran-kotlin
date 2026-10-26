package com.nur.quran.data.api.rn

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

private const val TAG = "NurRnApi"

/**
 * "/auth/…" Retrofit surface — mounted on the plain OkHttpClient in di/RnModule.
 * A failed refresh must 401 out, never recurse through the client-side
 * authenticator, which is why this interface is separate from the full
 * [RnApi] (content + sync).
 */
interface RnAuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RnRegisterRequest): RnAuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: RnLoginRequest): RnAuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RnRefreshRequest): RnAuthResponse
}

/**
 * RN auth surface (register / login / refresh) + session persistence. This is
 * the only class that writes [RnTokenStore]; the transport-side pieces
 * ([RnAuthInterceptor]/[RnAuthenticator]) only read-or-refresh through it, so
 * token lifecycle stays in one place.
 *
 * 401 semantics: access tokens expire (contract TTL: 900s). The OkHttp
 * authenticator refreshes once and replays the original request; a refresh
 * itself failing 401 invalidates the session (server declared the refresh
 * token dead) — next reads report signed-out, matching the app's
 * `refresh()` → `signedIn=false` behavior. Transport failures leave the
 * session alone.
 *
 * `userId` persistence = fix/login-restore-pull: identity survives restarts
 * without a live account call.
 */
@Singleton
class RnAuthClient @Inject constructor(
    private val api: RnAuthApi,
    private val tokenStore: RnTokenStore,
) {
    /** POST /auth/register → tokens + user persisted inline (single call contract). */
    suspend fun register(email: String, password: String, name: String = ""): RnAuthResponse {
        val response = api.register(
            RnRegisterRequest(
                email = email.trim(),
                password = password,
                name = name.trim().takeIf { it.isNotBlank() },
            ),
        )
        persistSession(response)
        return response
    }

    /** POST /auth/login → tokens + user persisted. */
    suspend fun login(email: String, password: String): RnAuthResponse {
        val response = api.login(RnLoginRequest(email = email.trim(), password = password))
        persistSession(response)
        return response
    }

    /** Logout: forget the whole RN session (identity included). */
    fun clearSession() {
        tokenStore.clearSession()
    }

    /**
     * POST /auth/refresh — rotates both tokens. Null on any failure; on a
     * server-declared invalid refresh (401) the session is cleared, while a
     * network stall keeps it for the next attempt.
     */
    suspend fun refreshAccessToken(): String? {
        val stored = tokenStore.refreshToken
        if (stored.isNullOrBlank()) return null
        return try {
            val response = api.refresh(RnRefreshRequest(refreshToken = stored))
            persistSession(response)
            response.accessToken
        } catch (e: HttpException) {
            if (e.code() == 401) {
                Log.w(TAG, "refresh rejected (401) — session cleared")
                tokenStore.clearSession()
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "refresh failed: ${e.message}")
            null
        }
    }

    /**
     * Access token usable for a request right now: the stored one when present,
     * else a fresh one via /auth/refresh (token may have expired server-side and
     * the answer arrives on the 401 replay either way).
     */
    suspend fun ensureValidAccessToken(): String? {
        tokenStore.accessToken?.takeIf { it.isNotBlank() }?.let { return it }
        return refreshAccessToken() ?: tokenStore.accessToken
    }

    private fun persistSession(response: RnAuthResponse) {
        response.user?.id?.let { tokenStore.userId = it }
        response.user?.email?.let { tokenStore.userEmail = it }
        response.accessToken?.takeIf { it.isNotBlank() }?.let { tokenStore.accessToken = it }
        response.refreshToken?.takeIf { it.isNotBlank() }?.let { tokenStore.refreshToken = it }
    }
}

/** Adds `Authorization: Bearer <access>` to every request that lacks it. */
class RnAuthInterceptor @Inject constructor(
    private val tokenStore: RnTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.accessToken
            ?: return chain.proceed(chain.request())
        val request = chain.request()
        if (request.header("Authorization") != null) return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header("Authorization", "Bearer $token").build(),
        )
    }
}

/**
 * 401 → single refresh + replay. Returns null on the second round-trip
 * (`priorResponse` guard) or when a fresh token isn't obtainable, surfacing
 * the original 401 to the caller.
 */
class RnAuthenticator(
    private val authClient: RnAuthClient,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val token = runBlocking { authClient.refreshAccessToken() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}
