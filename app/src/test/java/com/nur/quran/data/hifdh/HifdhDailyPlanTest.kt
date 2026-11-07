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
}
