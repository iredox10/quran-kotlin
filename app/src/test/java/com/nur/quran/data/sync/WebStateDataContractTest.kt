package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Web truth: appwrite.js `pullState` does `JSON.parse(doc.stateData)`, `pushState`
 * writes `JSON.stringify(mergedData)` back into the same attribute. Everything the
 * web app can see of an Android document therefore has to survive
 * `getSyncableState` JSON -> `stateData` -> `stateFromDoc` -> merge/apply unchanged.
 */
class WebStateDataContractTest {

    @Test
    fun `web stateData parses back into the identical state`() {
        val web = webState()
        val doc = mapOf(
            "userId" to "account-1",
            "stateData" to webStateData(),
            "version" to 1,
            "updatedAt" to "2026-05-08T09:00:00.000Z",
        )
        val parsed = SyncContract.stateFromDoc(doc) { parseState(it) }
        assertEquals(web, parsed)
    }

    @Test
    fun `push payload carries only userId and stateData`() {
        val payload = SyncContract.buildPushPayload("account-1", webStateData())
        assertEquals(setOf(SyncContract.FIELD_USER_ID, SyncContract.FIELD_STATE_DATA), payload.keys)
        val restored = parseState(payload[SyncContract.FIELD_STATE_DATA] as String)
        assertEquals(webState(), restored)
    }

    @Test
    fun `stripped state keeps every web key and drops document bookkeeping`() {
        val web = webState() + mapOf(
            "version" to 1,
            "updatedAt" to 1778300000000.0,
            "stateData" to "{}",
        )
        val stripped = SyncContract.stripStateMetadata(web)
        assertEquals(webState().keys, stripped.keys)
        assertFalse(stripped.containsKey("version"))
        assertFalse(stripped.containsKey("updatedAt"))
        assertFalse(stripped.containsKey("stateData"))
    }

    @Test
    fun `resolveDoc prefers the row labelled with this account`() {
        val web = webState()
        val rows = listOf(
            SyncContract.DocRef("other-doc", mapOf("userId" to "account-9"), 1),
            SyncContract.DocRef("mine", mapOf("userId" to "account-1", "stateData" to webStateData()), 2),
        )
        val doc = SyncContract.resolveDoc(rows, "account-1")
        assertEquals("mine", doc?.id)
        assertEquals(web, SyncContract.stateFromDoc(rows[1].data) { parseState(it) })
    }

    @Test
    fun `no labelled row still resolves without minting an id`() {
        val doc = SyncContract.resolveDoc(
            listOf(SyncContract.DocRef("legacy", mapOf(SyncContract.FIELD_STATE_DATA to webStateData()), 0)),
            "account-1",
        )
        assertEquals("legacy", doc?.id)
    }

    @Test
    fun `a zero watermark means never synced and pulls any stamped remote`() {
        assertTrue(SyncContract.shouldPull(remoteUpdatedAt = 1L, lastSyncAt = 0L))
        assertFalse(SyncContract.shouldPull(remoteUpdatedAt = 0L, lastSyncAt = 0L))
    }

    @Test
    fun `pull gate strictly compares remote and local stamps`() {
        assertTrue(SyncContract.shouldPull(remoteUpdatedAt = 2000L, lastSyncAt = 1000L))
        assertFalse(SyncContract.shouldPull(remoteUpdatedAt = 1000L, lastSyncAt = 1000L))
        assertFalse(SyncContract.shouldPull(remoteUpdatedAt = 999L, lastSyncAt = 1000L))
    }
}
