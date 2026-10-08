package com.nur.quran.data.hifdh

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Web parity with quran-app/src/pages/MemorizeIndex.jsx queue split:
 * no-card | New | Learning | reps<3 => sabaq; same-surah-as-last-session
 * => sabqi; rest => manzil. Due = no card or due <= now (boundary inclusive).
 */
class HifdhQueuesTest {

    private val now = 1_700_000_000_000L

    private fun card(state: Int, reps: Int, due: Long) = FsrsCard(
        due = due, stability = 1.0, difficulty = 5.0, elapsedDays = 0.0,
        scheduledDays = 1, reps = reps, lapses = 0, learningSteps = 0, state = state
    )

    private fun entry(state: Int, reps: Int, due: Long) =
        HifdhHistoryEntry(card = card(state, reps, due))

    @Test
    fun noCardIsSabaqAndDue() {
        assertEquals(HifdhQueue.SABAQ, classifyHifdhQueue("2:5", null, 2))
    }

    @Test
    fun newAndLearningAreSabaqEvenInLastSessionSurah() {
        assertEquals(HifdhQueue.SABAQ, classifyHifdhQueue("2:5", card(FsrsState.NEW, 5, now), 2))
        assertEquals(HifdhQueue.SABAQ, classifyHifdhQueue("2:5", card(FsrsState.LEARNING, 5, now), 2))
    }

    @Test
    fun repsUnder3AreSabaqEvenWhenReview() {
        assertEquals(HifdhQueue.SABAQ, classifyHifdhQueue("3:1", card(FsrsState.REVIEW, 2, now), null))
    }

    @Test
    fun reviewReps3PlusInLastSessionSurahIsSabqi() {
        assertEquals(HifdhQueue.SABQI, classifyHifdhQueue("2:5", card(FsrsState.REVIEW, 3, now), 2))
    }

    @Test
    fun reviewReps3PlusElsewhereIsManzil() {
        assertEquals(HifdhQueue.MANZIL, classifyHifdhQueue("3:1", card(FsrsState.REVIEW, 9, now), 2))
        assertEquals(HifdhQueue.MANZIL, classifyHifdhQueue("3:1", card(FsrsState.REVIEW, 9, now), null))
        // Relearning with reps>=3 is not newish => manzil outside last surah.
        assertEquals(HifdhQueue.MANZIL, classifyHifdhQueue("3:1", card(FsrsState.RELEARNING, 4, now), 2))
    }

    @Test
    fun dueBoundaryIsInclusive() {
        assertEquals(true, isHifdhDue(null, now))
        assertEquals(true, isHifdhDue(card(FsrsState.REVIEW, 5, now), now))
        assertEquals(true, isHifdhDue(card(FsrsState.REVIEW, 5, now - 1), now))
        assertEquals(false, isHifdhDue(card(FsrsState.REVIEW, 5, now + 1), now))
    }

    @Test
    fun buildQueueDataSplitsAndFiltersDue() {
        val history = mapOf(
            "2:1" to entry(FsrsState.REVIEW, 5, now - 10), // sabqi, due
            "2:2" to entry(FsrsState.REVIEW, 5, now + 10), // sabqi, not due
            "3:1" to entry(FsrsState.REVIEW, 5, now - 10), // manzil, due
            "3:2" to entry(FsrsState.LEARNING, 9, now - 10) // sabaq, due
            // "3:3" has no card => sabaq, due
        )
        val q = buildHifdhQueueData(setOf("2:1", "2:2", "3:1", "3:2", "3:3"), history, 2, now)
        assertEquals(listOf("3:2", "3:3"), q.sabaqAll)
        assertEquals(listOf("3:2", "3:3"), q.sabaqDue)
        assertEquals(listOf("2:1", "2:2"), q.sabqiAll)
        assertEquals(listOf("2:1"), q.sabqiDue)
        assertEquals(listOf("3:1"), q.manzilAll)
        assertEquals(listOf("3:1"), q.manzilDue)
    }

    @Test
    fun sessionProgressUsesShownCount() {
        assertEquals(0.5f, hifdhSessionProgress(0, 5, 10))
        // Partial last chunk: (idx + shown) / total, not (idx + chunkSize).
        assertEquals(1.0f, hifdhSessionProgress(8, 2, 10))
        assertEquals(0f, hifdhSessionProgress(0, 0, 0))
    }
}
