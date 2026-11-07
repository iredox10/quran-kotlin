package com.nur.quran.data.hifdh

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HifdhEvidenceBoundaryTest {
    @Test fun crossingMidnightWithoutSpacingDoesNotProveRetention() {
        val first = Instant.parse("2026-11-07T23:59:00Z").toEpochMilli()
        val a = HifdhReviewEvent(verseKey = "1:1", timestamp = first, rating = 3)
        val b = a.copy(id = java.util.UUID.randomUUID().toString(), timestamp = first + 120_000)
        assertEquals(2, evidenceFor(listOf(a,b), ZoneId.of("UTC")).independentDays)
        assertEquals(HifdhMastery.RECALLED, evidenceFor(listOf(a,b), ZoneId.of("UTC")).mastery)
        assertEquals(HifdhMastery.RECALLED, evidenceFor(listOf(a,b), ZoneId.of("Africa/Lagos")).mastery)
    }
    @Test fun practiceTimeAndManuallyClosingAnErrorNeverBecomeRecallEvidence() {
        val failed = HifdhReviewEvent(verseKey = "1:1", timestamp = 1000, rating = 1)
        val practice = failed.copy(id = "practice-event", timestamp = 2000, rating = 3, outcome = "practice", durationSeconds = 30)
        val resolved = practice.copy(id = "resolved-event", outcome = "resolved", relatedEventId = failed.id)
        assertTrue(practice.isValid)
        assertFalse(practice.independentPass)
        assertEquals(HifdhMastery.LEARNING, evidenceFor(listOf(practice)).mastery)
        assertEquals(HifdhMastery.REPAIR, evidenceFor(listOf(failed,practice,resolved)).mastery)
        assertNull(evidenceFor(listOf(practice,resolved)).learnedAt)
    }
    @Test fun clockMovingBackwardsDoesNotAgeNewlyLearnedMaterial() {
        val event = HifdhReviewEvent(verseKey = "1:1", timestamp = 1_700_000_000_000, rating = 3, acquisition = true)
        val evidence = evidenceFor(listOf(event,event))
        assertEquals(1, evidence.independentDays)
        assertEquals(HifdhQueue.SABAQ, classifyEvidenceQueue(evidence, null, event.timestamp - 86400000))
    }
}
