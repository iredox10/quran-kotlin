package com.nur.quran.data.hifdh

import org.junit.Assert.*
import org.junit.Test

class HifdhMistakesTest {
    private val mistake = HifdhReviewEvent(id = "original-mistake", verseKey = "2:136", timestamp = 1000, rating = 1, outcome = "confusion", relatedVerse = "3:84")
    @Test fun confusionPairNeedsBothVersesRecalledAfterTheMistake() {
        val a = HifdhReviewEvent(verseKey = "2:136", timestamp = 2000, rating = 3)
        val b = HifdhReviewEvent(verseKey = "3:84", timestamp = 3000, rating = 3)
        assertEquals(listOf(mistake), unresolvedHifdhMistakes(listOf(mistake,a)))
        assertTrue(unresolvedHifdhMistakes(listOf(mistake,a,b)).isEmpty())
    }
    @Test fun correctionOrPracticeCannotClearAMistake() {
        val review = HifdhReviewEvent(verseKey = "2:136", timestamp = 2000, rating = 3, task = "CORRECTION")
        assertEquals(listOf(mistake), unresolvedHifdhMistakes(listOf(mistake, review)))
        assertEquals(listOf(mistake), unresolvedHifdhMistakes(listOf(mistake, review.copy(outcome = "practice"))))
    }
    @Test fun explicitClosureOnlyResolvesTheSelectedObservation() {
        val second = mistake.copy(id = "second-mistake", timestamp = 4000)
        val closed = HifdhReviewEvent(verseKey = "2:136", rating = 3, outcome = "resolved", relatedEventId = mistake.id)
        assertEquals(listOf(second), unresolvedHifdhMistakes(listOf(mistake,second,closed)))
    }
}
