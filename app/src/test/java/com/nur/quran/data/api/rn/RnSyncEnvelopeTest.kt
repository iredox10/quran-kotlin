package com.nur.quran.data.api.rn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GET /v1/sync envelope tolerance: the contract freezes the sync DOC fields
 * (`userId` + `stateData`, ISO timestamp) and leaves the outer wrapper
 * minimal — the parse must denormalize whatever wrapper the backend ships.
 */
class RnSyncEnvelopeTest {

    @Test
    fun `null and empty map are no document`() {
        assertNull(RnSyncEnvelope.fromMap(null))
        assertNull(RnSyncEnvelope.fromMap(emptyMap()))
        // Error-shaped responses can't pretend to be the user's document.
        assertNull(RnSyncEnvelope.fromMap(mapOf("error" to "not_implemented_yet")))
        // userId echo alone is not a document either.
        assertNull(RnSyncEnvelope.fromMap(mapOf("userId" to "u1")))
    }

    @Test
    fun `top-level doc is taken`() {
        val doc = RnSyncEnvelope.fromMap(
            mapOf(
                "userId" to "u1",
                "stateData" to """{"theme":"dark"}""",
                "updatedAt" to "2023-11-14T22:13:20.000Z",
            ),
        )!!
        assertEquals("u1", doc.userId)
        assertEquals("""{"theme":"dark"}""", doc.stateData)
        assertEquals(1700000000000L, doc.updatedAtMs)
    }

    @Test
    fun `lastSyncAt is a timestamp too`() {
        val doc = RnSyncEnvelope.fromMap(
            mapOf(
                "userId" to "u1",
                "stateData" to "{}",
                "lastSyncAt" to "1769000000000",
            ),
        )!!
        assertEquals(1769000000000L, doc.updatedAtMs)
    }

    @Test
    fun `nested wrappers denormalize`() {
        val inner = linkedMapOf<String, Any?>(
            "userId" to "u1",
            "stateData" to """{"a":1}""",
            "updatedAt" to "2023-11-14T22:13:20.000Z",
        )
        for (key in listOf("doc", "document", "sync", "data", "userSync")) {
            val doc = RnSyncEnvelope.fromMap(mapOf(key to inner))!!
            assertEquals("u1", doc.userId)
            assertEquals("""{"a":1}""", doc.stateData)
            assertEquals(1700000000000L, doc.updatedAtMs)
        }
    }

    @Test
    fun `a literal-epoch timestamp counts as a document`() {
        // Timestamp-only docs (no stateData) parse; callers apply canary/empty state.
        val doc = RnSyncEnvelope.fromMap(mapOf("updatedAt" to "2023-11-14T22:13:20.000Z"))!!
        assertNull(doc.stateData)
        assertEquals(1700000000000L, doc.updatedAtMs)
    }

    @Test
    fun `stateData without any timestamp yields zero watermark (caller's gate decides)`() {
        val doc = RnSyncEnvelope.fromMap(mapOf("userId" to "u1", "stateData" to "{}"))!!
        assertEquals(0L, doc.updatedAtMs)
    }
}
