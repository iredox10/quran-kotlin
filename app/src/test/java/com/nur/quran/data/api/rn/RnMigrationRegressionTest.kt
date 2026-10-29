package com.nur.quran.data.api.rn

import com.google.gson.Gson
import com.nur.quran.data.api.*
import com.nur.quran.data.sync.syncRig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.Assert.*
import org.junit.Test

class RnMigrationRegressionTest {
    private fun client(api: FakeRnApi): RnApiClient {
        val store = FakeRnTokenStore().apply { userId = "u1"; refreshToken = "refresh" }
        return RnApiClient(RnBackend(), api, unusedAuthClient(store), store)
    }

    private fun verse(key: String, page: Int) = ApiVerse(
        id = key.substringBefore(":").toInt() * 1000 + key.substringAfter(":").toInt(),
        verse_number = key.substringAfter(":").toInt(), verse_key = key,
        page_number = page, juz_number = 1, words = null, translations = null, audio = null,
    )

    @Test
    fun `chapter follows every page and forwards translation and opt out flags`() = runBlocking {
        val pages = mutableListOf<Int>()
        val api = object : FakeRnApi() {
            override suspend fun getVerses(chapterId: Int, translation: Int, words: Int?, tajweed: Int?, page: Int, perPage: Int, mushaf: Int): VersesResponse {
                assertEquals(131, translation)
                assertEquals(0, words)
                assertEquals(1, tajweed)
                assertEquals(300, perPage)
                pages += page
                return VersesResponse(listOf(verse("2:$page", 2)), ApiPagination(page, if (page == 1) 2 else null, 2))
            }
        }
        val response = client(api).getVerses(2, words = false, tajweed = true, translation = 131)
        assertEquals(listOf(1, 2), pages)
        assertEquals(listOf("2:1", "2:2"), response.verses.map { it.verse_key })
        assertNull(response.pagination.next_page)
    }

    @Test
    fun `repeated pagination fails rather than publishing a partial chapter`() = runBlocking {
        val api = object : FakeRnApi() {
            override suspend fun getVerses(chapterId: Int, translation: Int, words: Int?, tajweed: Int?, page: Int, perPage: Int, mushaf: Int) =
                VersesResponse(listOf(verse("2:1", 2)), ApiPagination(1, 1, 2))
        }
        try {
            client(api).getVerses(2)
            fail("Expected invalid pagination failure")
        } catch (_: IllegalStateException) { }
    }

    @Test
    fun `printed page forwards selected translation and layout to backend`() = runBlocking {
        val api = object : FakeRnApi() {
            override suspend fun getPage(page: Int, translation: Int, mushaf: Int): VersesResponse {
                assertEquals(10, page); assertEquals(131, translation); assertEquals(19, mushaf)
                return VersesResponse(listOf(verse("2:1", 10)), ApiPagination(1, null, 1))
            }
        }
        assertEquals("2:1", client(api).getPage(10, 131, 19).verses.single().verse_key)
    }

    @Test
    fun `real bundled word pack parses flat translations without modifying source`() = runBlocking {
        val file = java.io.File("src/main/assets/data/words/1.json")
        val raw = Gson().fromJson(file.readText(), com.google.gson.JsonObject::class.java)
        val before = raw.toString()
        val api = object : FakeRnApi() {
            override suspend fun getWordsPack(chapterId: Int) = raw
        }
        val verses = client(api).getWordsPack(1).verses
        assertEquals(7, verses.size)
        assertTrue(verses.first().words!!.any { !it.translation?.text.isNullOrBlank() })
        assertEquals(before, raw.toString())
    }

    @Test
    fun `fresh backend canary triggers the first upload`() = runBlocking {
        var uploaded: RnSyncPushPayload? = null
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> = mapOf(
                "id" to null, "userId" to "u1", "stateData" to null,
                "state" to null, "updatedAt" to 0.0, "createdAt" to 0.0,
            )
            override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse {
                uploaded = body
                return RnSyncPutResponse(updatedAt = 1234L)
            }
        }
        val rig = syncRig(rnClient = client(api))
        assertEquals("pushed", rig.service.sync())
        assertEquals("u1", uploaded!!.userId)
        assertTrue(uploaded!!.stateData.isNotBlank())
    }

    @Test
    fun `migrated flattened state survives transport and metadata is excluded`() = runBlocking {
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> = mapOf(
                "id" to "old-doc", "userId" to "u1", "stateData" to null,
                "state" to null, "theme" to "dark", "updatedAt" to 1234L, "createdAt" to 1000L,
            )
        }
        val doc = client(api).getSyncDoc()!!
        assertEquals(mapOf("theme" to "dark"), doc.legacyFields)
        val rig = syncRig(rnClient = client(api))
        assertTrue(rig.service.pull())
        assertEquals("dark", rig.snapshot()["theme"])
    }

    @Test
    fun `wrong owner fails without uploading`() = runBlocking {
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> = mapOf("userId" to "other", "stateData" to "{}", "updatedAt" to 1234L)
            override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse = throw AssertionError("Must not upload")
        }
        assertTrue(syncRig(rnClient = client(api)).service.sync().startsWith("error:"))
    }

    @Test
    fun `missing write timestamp is a failed upload`() = runBlocking {
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> = emptyMap()
            override suspend fun putSync(body: RnSyncPushPayload) = RnSyncPutResponse()
        }
        assertTrue(syncRig(rnClient = client(api)).service.sync().startsWith("error:"))
    }

    @Test
    fun `foreground and polling sync serialize their read merge write operations`() = runBlocking {
        val running = java.util.concurrent.atomic.AtomicInteger()
        val peak = java.util.concurrent.atomic.AtomicInteger()
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> {
                val active = running.incrementAndGet()
                peak.updateAndGet { maxOf(it, active) }
                try {
                    kotlinx.coroutines.delay(20)
                    return emptyMap()
                } finally { running.decrementAndGet() }
            }
            override suspend fun putSync(body: RnSyncPushPayload) = RnSyncPutResponse(updatedAt = 1234L)
        }
        val service = syncRig(rnClient = client(api)).service
        val results = kotlinx.coroutines.coroutineScope {
            listOf(async { service.sync() }, async { service.sync() }).awaitAll()
        }
        assertEquals(listOf("pushed", "pushed"), results)
        assertEquals(1, peak.get())
    }
    @Test
    fun `local edits upload even when the server watermark has not changed`() = runBlocking {
        var remote: Map<String, Any?> = emptyMap()
        var writes = 0
        var clock = 1000L
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> = if (writes == 0) emptyMap() else mapOf(
                "userId" to "u1", "stateData" to Gson().toJson(remote), "updatedAt" to clock
            )
            override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse {
                remote = Gson().fromJson(body.stateData, object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type)
                writes++; clock++
                return RnSyncPutResponse(updatedAt = clock)
            }
        }
        val rig = syncRig(rnClient = client(api))
        assertEquals("pushed", rig.service.sync())
        assertEquals("up-to-date", rig.service.sync())
        rig.prefs.values["is_dark_theme"] = true
        assertEquals("pushed", rig.service.sync())
        assertEquals("dark", remote["theme"])
        assertEquals("up-to-date", rig.service.sync())
        assertEquals(2, writes)
    }

    @Test
    fun `changed account during a read cannot apply old data or upload it`() = runBlocking {
        val store = FakeRnTokenStore().apply { userId = "u1"; refreshToken = "refresh" }
        val api = object : FakeRnApi() {
            override suspend fun getSync(): Map<String, Any?> {
                store.userId = "u2"
                return mapOf("userId" to "u1", "stateData" to "{\"theme\":\"dark\"}", "updatedAt" to 1234L)
            }
            override suspend fun putSync(body: RnSyncPushPayload): RnSyncPutResponse = throw AssertionError("No upload allowed")
        }
        val client = RnApiClient(RnBackend(), api, unusedAuthClient(store), store)
        val rig = syncRig(rnClient = client)
        try {
            rig.service.sync()
            fail("Old account request must be cancelled")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        assertFalse(rig.prefs.contains("is_dark_theme"))
    }

    @Test
    fun `lost acknowledgment retries the original mutation without dropping a newer edit`() = runBlocking {
        val ids = mutableListOf<String>()
        val changes = mutableListOf<Map<String, Any?>>()
        val api = object : FakeRnApi() {
            override suspend fun patchSync(body: RnSyncPatchPayload): Map<String, Any?> {
                ids += body.mutationId
                changes += body.changes
                if (ids.size == 1) throw java.io.IOException("Lost acknowledgment")
                return super.patchSync(body)
            }
            override suspend fun putSync(body: RnSyncPushPayload) = RnSyncPutResponse(updatedAt = 1000L + ids.size)
        }
        val rig = syncRig(rnClient = client(api))
        rig.prefs.values["is_dark_theme"] = true
        assertFalse(rig.service.push())
        rig.prefs.values["is_dark_theme"] = false
        assertTrue(rig.service.push())
        assertEquals(ids[0], ids[1])
        assertEquals(changes[0], changes[1])
        assertTrue(ids.size >= 3)
        assertNotEquals(ids[1], ids[2])
        assertEquals("light", changes.last()["theme"])
    }

}
