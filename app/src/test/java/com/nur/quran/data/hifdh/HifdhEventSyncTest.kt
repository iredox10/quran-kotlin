package com.nur.quran.data.hifdh

import com.nur.quran.data.sync.mergeStateInto
import org.junit.Assert.*
import org.junit.Test

class HifdhEventSyncTest {
    @Test fun newEvidenceSectionUnionsEventIdsWithoutChangingLegacySchedulerCards() {
        val card = mapOf("card" to mapOf("due" to "2026-11-07T00:00:00Z", "reps" to 3))
        val a = mapOf("hifdhReviewEvents" to mapOf("event-first" to mapOf("verseKey" to "1:1", "rating" to 3)), "hifdhHistory" to mapOf("1:1" to card))
        val b = mapOf("hifdhReviewEvents" to mapOf("event-second" to mapOf("verseKey" to "1:2", "rating" to 1)))
        val merged = mergeStateInto(a, b) as Map<*, *>
        assertEquals(setOf("event-first", "event-second"), (merged["hifdhReviewEvents"] as Map<*, *>).keys)
        assertEquals(a["hifdhHistory"], merged["hifdhHistory"])
        assertEquals(merged, mergeStateInto(merged, b))
    }
}
