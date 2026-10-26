package com.nur.quran.data.api.rn

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.sync.SyncContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The sync-document facade with constructor-injected fakes: the wire body must
 * be byte-identical to `SyncContract.buildPushPayload`, and response stamps
 * must become the caller's watermark (epoch ms) — never "now".
 */
class RnApiClientSyncTest {

    private class KnobRnApi : FakeRnApi() {
        var syncResponse: Map<String, Any?>? = null
        var putResponse = RnSyncPutResponse(lastSyncAt = "1769000000000")
        val capturedPutBodies = mutableListOf<RnSyncPushPayload>()

        override suspend fun getSync(): Map<String, Any?> {
            return syncResponse ?: throw AssertionError("no sync response")
        }

        override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse {
            capturedPutBodies += body
            return putResponse
        }
    }

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

    /** Parses a wire-JSON body the way Gson parses the real response. */
    private fun mapOfJson(json: String): Map<String, Any?> =
        gson.fromJson<Map<String, Any?>>(json, mapType)!!

    @Test
    fun `putSyncDoc keeps the contract payload byte-identical`() = kotlinx.coroutines.runBlocking {
        val api = KnobRnApi()
        val client = RnApiClient(RnBackend().apply { injectedEnabled = false }, api, unusedAuthClient(), FakeRnTokenStore())
        val payload = SyncContract.buildPushPayload("u1", """{"theme":"dark"}""")

        client.putSyncDoc(payload)

        val sent = api.capturedPutBodies.single()
        assertEquals("u1", sent.userId)
        assertEquals("""{"theme":"dark"}""", sent.stateData)
    }

    @Test
    fun `putSyncDoc maps lastSyncAt (epoch ms number string) to the watermark`() = kotlinx.coroutines.runBlocking {
        val api = KnobRnApi().apply { putResponse = RnSyncPutResponse(lastSyncAt = "1769000000000") }
        val client = RnApiClient(RnBackend().apply { injectedEnabled = false }, api, unusedAuthClient(), FakeRnTokenStore())

        assertEquals(1769000000000L, client.putSyncDoc(mapOf("userId" to "u", "stateData" to "{}")))
    }

    @Test
    fun `putSyncDoc falls back to updatedAt (ISO) and null on nothing parseable`() = kotlinx.coroutines.runBlocking {
        val api = KnobRnApi().apply {
            putResponse = RnSyncPutResponse(updatedAt = "2026-01-20T12:00:00.000Z")
        }
        val client = RnApiClient(RnBackend().apply { injectedEnabled = false }, api, unusedAuthClient(), FakeRnTokenStore())
        assertEquals(
            java.time.Instant.parse("2026-01-20T12:00:00.000Z").toEpochMilli(),
            client.putSyncDoc(mapOf()),
        )

        api.putResponse = RnSyncPutResponse()
        assertNull(client.putSyncDoc(mapOf()))
    }

    @Test
    fun `getSyncDoc denormalizes the wire envelope`() = kotlinx.coroutines.runBlocking {
        val api = KnobRnApi().apply {
            syncResponse = mapOfJson(
                """{"userId":"u1","stateData":"{\"memorizedAyahs\":[\"1:1\"]}",""" +
                    """"updatedAt":"2023-11-14T22:13:20.000Z"}""",
            )
        }
        val client = RnApiClient(RnBackend().apply { injectedEnabled = false }, api, unusedAuthClient(), FakeRnTokenStore())

        val doc = client.getSyncDoc()!!
        assertEquals("u1", doc.userId)
        // stateData STAYS the JSON string the merge engine expects (web truth shape).
        assertEquals("""{"memorizedAyahs":["1:1"]}""", doc.stateData)
        assertEquals(1700000000000L, doc.updatedAtMs)
    }

    @Test
    fun `getSyncDoc propagates the no-document case as null`() = kotlinx.coroutines.runBlocking {
        val api = KnobRnApi().apply { syncResponse = mapOfJson("""{}""") }
        val client = RnApiClient(RnBackend().apply { injectedEnabled = false }, api, unusedAuthClient(), FakeRnTokenStore())

        assertNull(client.getSyncDoc())
    }

    @Test
    fun `session accessors read the store and honor the session gate`() {
        val store = FakeRnTokenStore()
        val client = RnApiClient(
            RnBackend().apply { injectedEnabled = false },
            KnobRnApi(),
            RnAuthClient(FakeRnAuthApi(), store),
            store,
        )

        store.refreshToken = "ref"
        assertNull("refresh token alone is not a session", client.storedUserId())
        assertEquals(false, client.hasSession())

        store.userId = "u1"
        store.accessToken = "acc"
        assertEquals("u1", client.storedUserId())
        assertEquals(true, client.hasSession())
    }
}
