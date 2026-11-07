package com.nur.quran.data.hifdh

import com.nur.quran.data.sync.mergeStateInto
import org.junit.Assert.*
import org.junit.Test

class HifdhEventSyncTest {
    @Test fun newEvidenceSectionUnionsEventIdsWithoutChangingLegacySchedulerCards() {
        val card = mapOf("card" to mapOf("due" to "2026-11-07T00:00:00Z", "reps" to 3))
        val a = mapOf("hifdhReviewEvents" to mapOf("event-first" to mapOf("verseKey" to "1:1", "timestamp" to 1, "rating" to 3)), "hifdhHistory" to mapOf("1:1" to card))
        val b = mapOf("hifdhReviewEvents" to mapOf("event-second" to mapOf("verseKey" to "1:2", "timestamp" to 2, "rating" to 1)))
        val merged = mergeStateInto(a, b) as Map<*, *>
        assertEquals(setOf("event-first", "event-second"), (merged["hifdhReviewEvents"] as Map<*, *>).keys)
        assertEquals(a["hifdhHistory"], merged["hifdhHistory"])
        assertEquals(merged, mergeStateInto(merged, b))
    }
    @Test fun recentWindowIsBoundedAndKeepsNewestEvidence() {
        val rows = (1..700).associate { "event-$it" to mapOf("timestamp" to it, "rating" to 3) }
        val merged = mergeStateInto(emptyMap<String, Any?>(), mapOf("hifdhReviewEvents" to rows)) as Map<*, *>
        val events = merged["hifdhReviewEvents"] as Map<*, *>
        assertEquals(500, events.size)
        assertFalse(events.containsKey("event-200"))
        assertTrue(events.containsKey("event-700"))
    }
    @Test fun duplicateIdCannotRewriteOriginalEvidence() {
        val base = mapOf("hifdhReviewEvents" to mapOf("immutable" to mapOf("timestamp" to 1, "rating" to 1)))
        val incoming = mapOf("hifdhReviewEvents" to mapOf("immutable" to mapOf("timestamp" to 2, "rating" to 3)))
        assertEquals(base, mergeStateInto(base, incoming))
    }
}
