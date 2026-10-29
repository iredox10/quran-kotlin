package com.nur.quran.data.api.rn

import com.nur.quran.ui.viewmodels.authErrorMessage
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RnRecoveryTest {
    @Test
    fun `recovery trims email without creating or changing a session`() = runBlocking {
        val store = FakeRnTokenStore()
        val api = object : FakeRnAuthApi() {
            override suspend fun requestRecovery(body: RnRecoveryRequest): RnRecoveryResponse {
                assertEquals("person@test.io", body.email)
                return RnRecoveryResponse("If an account exists, a reset link will be sent.")
            }
        }
        val auth = RnAuthClient(api, store)
        assertTrue(auth.requestRecovery(" person@test.io ").message!!.contains("If an account exists"))
        assertFalse(store.hasSession())
        assertNull(store.accessToken)
    }

    @Test
    fun `migrated identity errors direct users to verified password setup`() {
        val error = HttpException(Response.error<Any>(409, """{"error":"account_requires_recovery"}""".toResponseBody()))
        assertTrue(authErrorMessage(error, "Failed").contains("Forgot password"))
    }

    @Test
    fun `unconfigured delivery is reported as unavailable rather than success`() {
        val error = HttpException(Response.error<Any>(503, """{"error":"recovery_not_configured"}""".toResponseBody()))
        assertTrue(authErrorMessage(error, "Failed").contains("temporarily unavailable"))
    }
}
