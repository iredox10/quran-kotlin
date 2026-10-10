package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Resolution of the account's own document, and the watermark that gates a pull. */
class WebStateDataResolutionTest {
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
