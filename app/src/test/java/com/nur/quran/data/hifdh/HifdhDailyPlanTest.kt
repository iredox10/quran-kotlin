package com.nur.quran.data.hifdh

import org.junit.Assert.*
import org.junit.Test

class HifdhDailyPlanTest {
    private val now = 1_700_000_000_000L
    @Test fun longAyahsDoNotPretendToCostTheSameAsShortAyahs() {
        val plan = buildDailyHifdhPlan(listOf("2:1", "2:282"), emptySet(), emptyMap(), emptyList(), now,
            budgetMinutes = 5, wordCounts = mapOf("2:1" to 3, "2:282" to 128))
        assertEquals(listOf("2:1"), plan.newKeys)
    }
    @Test fun olderMaterialGetsCoverageEvenBeforeItsCardIsDue() {
        val card = FsrsScheduler().createEmptyCard(now).copy(due = now + 86_400_000, state = FsrsState.REVIEW)
        val plan = buildDailyHifdhPlan(emptyList(), setOf("1:1"), mapOf("1:1" to HifdhHistoryEntry(card)), emptyList(), now)
        assertEquals(listOf("1:1"), plan.olderKeys)
    }
    @Test fun overloadedReviewCreatesRecoveryWithoutDeletingAnythingOrAddingNewWork() {
        val known = (1..100).map { "2:$it" }.toSet()
        val plan = buildDailyHifdhPlan(listOf("3:1"), known, emptyMap(), emptyList(), now, budgetMinutes = 5)
        assertTrue(plan.recovery)
        assertTrue(plan.newKeys.isEmpty())
        assertTrue(plan.remainingReviewCount > 0)
        assertEquals(known.size, plan.reviewKeys.size + plan.remainingReviewCount)
    }
    @Test fun recordedMistakesArePrioritizedEvenWithoutAnExistingCard() {
        val note = HifdhReviewEvent(verseKey = "2:255", rating = 1, outcome = "mistake", timestamp = now)
        val plan = buildDailyHifdhPlan(listOf("2:255", "2:256"), emptySet(), emptyMap(), listOf(note), now)
        assertTrue("2:255" in plan.recentKeys)
        assertFalse("2:255" in plan.newKeys)
    }
    @Test fun writingANoteDoesNotHideMissedPracticeDays() {
        val reviewed = HifdhReviewEvent(verseKey = "1:1", rating = 3, timestamp = now - 5 * 86400000L)
        val note = HifdhReviewEvent(verseKey = "1:2", rating = 1, outcome = "mistake", timestamp = now)
        val plan = buildDailyHifdhPlan(listOf("1:3"), setOf("1:1"), emptyMap(), listOf(note,reviewed), now)
        assertTrue(plan.recovery)
        assertTrue(plan.newKeys.isEmpty())
    }
}
