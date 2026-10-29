package com.nur.quran.data.api.rn

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Token lifecycle with constructor-injected fakes (no new deps): persistence,
 * single-source refresh, and the 401-invalidates / network-stall-keeps session
 * split that backs the app's login-restore semantics.
 */
class RnAuthFlowTest {

    private class RecordingRnAuthApi : FakeRnAuthApi() {
        var logins = 0
        var registers = 0
        var refreshes = 0
        var loginResponse = RnAuthResponse(
            user = RnAuthUser(id = "u-reg", email = "reg@test.io", name = "Reg"),
            accessToken = "acc-reg",
            refreshToken = "ref-reg",
        )
        var registerResponse = loginResponse
        var refreshResponse = RnAuthResponse(
            user = RnAuthUser(id = "u-reg", email = "reg@test.io", name = "Reg"),
            accessToken = "acc-rotated",
            refreshToken = "ref-rotated",
        )
        var refreshError: Exception? = null

        override suspend fun register(body: RnRegisterRequest): RnAuthResponse {
            registers++
            return registerResponse
        }

        override suspend fun login(body: RnLoginRequest): RnAuthResponse {
            logins++
            return loginResponse
        }

        override suspend fun refresh(body: RnRefreshRequest): RnAuthResponse {
            refreshes++
            assertEquals("refresh must use the STORED refresh token", "ref-reg", body.refreshToken)
            refreshError?.let { throw it }
            return refreshResponse
        }
    }

    private fun httpError(code: Int): HttpException =
        HttpException(
            Response.error<Any>(code, "{}".toResponseBody(null)),
        )

    @Test
    fun `login persists tokens and identity (userId survives restarts)`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)

        client.login("USER@Test.io", "pw-12345678")

        assertEquals(1, api.logins)
        assertEquals("acc-reg", store.accessToken)
        assertEquals("ref-reg", store.refreshToken)
        assertEquals("u-reg", store.userId)
        assertEquals("reg@test.io", store.userEmail)
        assertTrue(store.hasSession())
    }

    @Test
    fun `refresh rotates both tokens`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")

        val token = client.refreshAccessToken()

        assertEquals(1, api.refreshes)
        assertEquals("acc-rotated", token)
        assertEquals("acc-rotated", store.accessToken)
        assertEquals("ref-rotated", store.refreshToken)
    }

    @Test
    fun `no refresh token means no refresh`() = runBlocking {
        val client = RnAuthClient(RecordingRnAuthApi(), FakeRnTokenStore())
        assertNull(client.refreshAccessToken())
    }

    @Test
    fun `server-declared invalid refresh (401) clears the session`() = runBlocking {
        val api = RecordingRnAuthApi().apply { refreshError = httpError(401) }
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")

        assertNull(client.refreshAccessToken())

        assertFalse(store.hasSession())
        assertNull(store.refreshToken)
        assertNull(store.userId)
    }

    @Test
    fun `transport failure on refresh keeps the session for the next attempt`() = runBlocking {
        val api = RecordingRnAuthApi().apply { refreshError = httpError(503) }
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")

        assertNull(client.refreshAccessToken())

        assertTrue(store.hasSession())
        assertEquals("ref-reg", store.refreshToken)
    }

    @Test
    fun `ensureValidAccessToken falls back to refresh when access is missing`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")
        store.accessToken = null

        assertEquals("acc-rotated", client.ensureValidAccessToken())
        assertEquals(1, api.refreshes)
    }

    @Test
    fun `register single-calls and persists the session`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)

        val response = client.register("new@test.io", "pw-12345678", "Nombre")

        assertEquals(1, api.registers)
        assertEquals(0, api.logins)
        assertEquals("u-reg", response.user?.id)
        assertTrue(store.hasSession())
    }

    @Test
    fun `clearSession resets the whole identity`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")

        client.clearSession()

        assertFalse(store.hasSession())
        assertNull(store.userId)
    }

    @Test
    fun `concurrent rejected requests reuse a single refreshed token`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")
        val results = kotlinx.coroutines.coroutineScope {
            (1..8).map {
                async {
                    client.refreshAccessToken("acc-reg")
                }
            }.map { it.await() }
        }
        assertEquals(1, api.refreshes)
        assertTrue(results.all { it == "acc-rotated" })
    }

    @Test
    fun `incomplete login does not overwrite a saved session`() = runBlocking {
        val api = RecordingRnAuthApi()
        val store = FakeRnTokenStore()
        val client = RnAuthClient(api, store)
        client.login("u@test.io", "pw-12345678")
        api.loginResponse = RnAuthResponse(user = RnAuthUser("bad", "bad@test.io"), accessToken = "partial")
        try {
            client.login("bad@test.io", "pw-12345678")
            org.junit.Assert.fail("Expected invalid auth response")
        } catch (_: IllegalArgumentException) { }
        assertEquals("u-reg", store.userId)
        assertEquals("acc-reg", store.accessToken)
        assertEquals("ref-reg", store.refreshToken)
    }
    @Test
    fun `refresh completing after logout cannot resurrect the session`() = runBlocking {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val store = FakeRnTokenStore().apply {
            userId = "u1"; accessToken = "old"; refreshToken = "refresh"
        }
        val api = object : FakeRnAuthApi() {
            override suspend fun refresh(body: RnRefreshRequest): RnAuthResponse {
                entered.complete(Unit)
                finish.await()
                return RnAuthResponse(RnAuthUser("u1", "u1@test.io"), "new", "rotated")
            }
        }
        val client = RnAuthClient(api, store)
        val pending = async { client.refreshAccessToken() }
        entered.await()
        client.clearSession()
        finish.complete(Unit)
        assertNull(pending.await())
        assertFalse(store.hasSession())
        assertNull(store.accessToken)
    }

    @Test
    fun `old rejected refresh cannot clear a newer account session`() = runBlocking {
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val store = FakeRnTokenStore().apply {
            userId = "old"; accessToken = "old-access"; refreshToken = "old-refresh"
        }
        val api = object : FakeRnAuthApi() {
            override suspend fun refresh(body: RnRefreshRequest): RnAuthResponse {
                entered.complete(Unit); finish.await(); throw httpError(401)
            }
            override suspend fun login(body: RnLoginRequest) =
                RnAuthResponse(RnAuthUser("new", "new@test.io"), "new-access", "new-refresh")
        }
        val client = RnAuthClient(api, store)
        val pending = async { client.refreshAccessToken() }
        entered.await()
        client.login("new@test.io", "password")
        finish.complete(Unit)
        assertNull(pending.await())
        assertEquals("new", store.userId)
        assertEquals("new-access", store.accessToken)
    }

}
