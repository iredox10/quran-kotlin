package com.nur.quran.data.api.rn

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RnGoogleAuthTest {
    @Test fun googleLoginPersistsBackendIdentityAndExchangesTheNonce() = runBlocking {
        val store = FakeRnTokenStore()
        var sent: RnGoogleRequest? = null
        val api = object : FakeRnAuthApi() {
            override suspend fun google(body: RnGoogleRequest): RnAuthResponse {
                sent = body
                return RnAuthResponse(RnAuthUser("original-appwrite-id", "person@gmail.com", "Person"), "access", "refresh")
            }
        }
        val response = RnAuthClient(api, store).signInWithGoogle("google-signed-token", "single-use-nonce")
        assertEquals(RnGoogleRequest("google-signed-token", "single-use-nonce"), sent)
        assertEquals("original-appwrite-id", response.user?.id)
        assertEquals("original-appwrite-id", store.userId)
        assertEquals("access", store.accessToken)
        assertEquals("refresh", store.refreshToken)
    }

    @Test fun incompleteGoogleSessionDoesNotOverwriteAnExistingSession() = runBlocking {
        val store = FakeRnTokenStore().apply { userId = "old-user"; accessToken = "old-access"; refreshToken = "old-refresh" }
        val api = object : FakeRnAuthApi() {
            override suspend fun google(body: RnGoogleRequest) = RnAuthResponse(RnAuthUser("new-user", "new@gmail.com"), "new-access", null)
        }
        try {
            RnAuthClient(api, store).signInWithGoogle("token", "nonce")
            fail("An incomplete session must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals("old-user", store.userId)
        assertEquals("old-access", store.accessToken)
        assertEquals("old-refresh", store.refreshToken)
    }

    @Test fun loadingProviderAndChallengeDoesNotEstablishASession() = runBlocking {
        val store = FakeRnTokenStore()
        val api = object : FakeRnAuthApi() {
            override suspend fun providers() = RnAuthProviders(RnGoogleProvider(true))
            override suspend fun googleChallenge() = RnGoogleChallenge("web.apps.googleusercontent.com", "nonce", 1234)
        }
        val auth = RnAuthClient(api, store)
        assertTrue(auth.googleProviders().google.enabled)
        assertEquals("web.apps.googleusercontent.com", auth.googleChallenge().clientId)
        assertNull(store.userId)
        assertNull(store.accessToken)
    }
}
