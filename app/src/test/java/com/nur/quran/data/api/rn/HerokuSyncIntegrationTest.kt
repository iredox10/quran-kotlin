package com.nur.quran.data.api.rn

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.sync.syncRig
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Opt-in live acceptance check, restricted to a disposable example.test account. */
class HerokuSyncIntegrationTest {
    @Test
    fun `mobile and web payloads converge through the deployed backend`() = runBlocking {
        val configPath = System.getenv("QURAN_NUR_LIVE_TEST_CONFIG")
        assumeTrue("Set QURAN_NUR_LIVE_TEST_CONFIG to a private disposable-account config", !configPath.isNullOrBlank())
        val gson = Gson()
        val config = gson.fromJson(File(configPath!!).readText(), com.google.gson.JsonObject::class.java)
        val origin = config.get("apiUrl").asString.trimEnd('/') + "/"
        val email = config.get("email").asString
        require(email.endsWith("@example.test") && origin.startsWith("https://"))
        val store = FakeRnTokenStore()
        val http = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).addInterceptor { chain ->
                check(chain.request().url.toString().startsWith(origin)) { "Unexpected API origin" }
                chain.proceed(chain.request())
            }.build()
        fun <T> service(type: Class<T>, client: OkHttpClient): T = Retrofit.Builder()
            .baseUrl(origin).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(type)
        val auth = RnAuthClient(service(RnAuthApi::class.java, http), store)
        val authenticated = http.newBuilder().addInterceptor(RnAuthInterceptor(store))
            .authenticator(RnAuthenticator(auth)).build()
        val wire = service(RnApi::class.java, authenticated)
        val client = RnApiClient(RnBackend().apply { injectedBaseUrl = origin }, wire, auth, store)
        val stateType = object : TypeToken<MutableMap<String, Any?>>() {}.type
        try {
            client.login(email, config.get("password").asString)
            assertEquals(114, client.getChapters().chapters.size)
            val rig = syncRig(seedPrefs = mapOf("memorized_ayahs" to setOf("1:1")), rnClient = client)
            assertTrue(rig.service.push())
            val firstWebDoc = RnSyncEnvelope.fromMap(wire.getSync())!!
            val webState: MutableMap<String, Any?> = gson.fromJson(firstWebDoc.stateData, stateType)
            assertEquals(listOf("1:1"), webState["memorizedAyahs"])
            webState["memorizedAyahs"] = listOf("1:1", "1:2")
            webState["theme"] = "dark"
            webState["webOnlyMarker"] = "preserve-me"
            wire.putSync(RnSyncPushPayload(store.userId!!, gson.toJson(webState)))
            assertTrue(rig.service.pull())
            assertEquals(setOf("1:1", "1:2"), rig.prefs.getStringSet("memorized_ayahs", null))
            assertEquals(true, rig.prefs.getBoolean("is_dark_theme", false))
            rig.prefs.values["memorized_ayahs"] = setOf("1:1", "1:2", "1:3")
            assertEquals("pushed", rig.service.sync())
            // A real 401 must rotate the refresh token and replay this web-shaped read.
            val refreshBefore = store.refreshToken
            store.accessToken = "expired-test-access"
            val finalDoc = RnSyncEnvelope.fromMap(wire.getSync())!!
            val finalState: MutableMap<String, Any?> = gson.fromJson(finalDoc.stateData, stateType)
            assertEquals(setOf("1:1", "1:2", "1:3"), (finalState["memorizedAyahs"] as List<*>).toSet())
            assertEquals("preserve-me", finalState["webOnlyMarker"])
            assertEquals(store.userId, finalDoc.userId)
            assertNotEquals(refreshBefore, store.refreshToken)
        } finally {
            auth.clearSession()
            http.dispatcher.executorService.shutdown()
            http.connectionPool.evictAll()
        }
    }
}
