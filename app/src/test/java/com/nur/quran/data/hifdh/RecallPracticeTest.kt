package com.nur.quran.data.hifdh

import org.junit.Assert.*
import org.junit.Test

class RecallPracticeTest {
    @Test fun promptingCannotBecomeASuccessfulFsrsReview() {
        for (rating in 1..4) {
            val result = RecallAssessment(rating, RecallCue.ANSWER_PROMPT)
            assertEquals(FsrsRating.AGAIN, result.schedulerRating)
            assertFalse(result.independent)
        }
        assertEquals(FsrsRating.HARD, RecallAssessment(2).schedulerRating)
        assertTrue(RecallAssessment(2).independent)
        assertFalse(RecallAssessment(3, RecallCue.PREVIOUS_AYAH, RecallTask.TRANSITION).independent)
    }
    @Test fun failedItemsReturnAfterInterveningMaterialAndStopAfterOneCorrectionAttempt() {
        val practice = RecallPractice(listOf("1:1", "1:2", "1:3"))
        practice.grade(1); practice.next()
        assertEquals("1:2", practice.current)
        practice.grade(3); practice.next()
        assertEquals("1:3", practice.current)
        practice.grade(3); practice.next()
        assertEquals("1:1", practice.current)
        assertTrue(practice.isCorrection)
        practice.grade(1); practice.next()
        assertTrue(practice.finished)
        assertEquals(setOf("1:1"), practice.unresolved)
    }
    @Test fun aSingleItemStillHasABoundedRepairAttempt() {
        val practice = RecallPractice(listOf("1:1", "1:1"))
        practice.grade(1); practice.next(); practice.grade(3); practice.next()
        assertTrue(practice.finished)
        assertEquals(2, practice.reviewed)
        assertTrue(practice.unresolved.isEmpty())
    }
}
