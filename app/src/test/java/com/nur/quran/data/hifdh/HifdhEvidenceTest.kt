package com.nur.quran.data.hifdh

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class HifdhEvidenceTest {
    private val now = 1_700_000_000_000L
    private fun event(time: Long = now, rating: Int = 3, cue: String = "NONE", task: String = "AYAH", assessor: String = "self") =
        HifdhReviewEvent(verseKey = "2:1", timestamp = time, rating = rating, cue = cue, task = task, assessor = assessor, acquisition = true)
    @Test fun repeatedSameDayPracticeIsNotDelayedRetention() {
        val events = listOf(event(), event(now + 1000), event(now + 2000))
        assertEquals(HifdhMastery.RECALLED, evidenceFor(events, ZoneId.of("UTC")).mastery)
        assertEquals(HifdhMastery.RETAINED, evidenceFor(events + event(now + 86_400_000), ZoneId.of("UTC")).mastery)
    }
    @Test fun correctionAndPromptingCannotClearAFailureOrVerifyATeacher() {
        val failed = event(rating = 1)
        val repair = event(now + 100, task = "CORRECTION")
        assertEquals(HifdhMastery.REPAIR, evidenceFor(listOf(failed, repair)).mastery)
        assertNull(evidenceFor(listOf(event(cue = "PREVIOUS_AYAH", assessor = "teacher"))).teacherCheckedAt)
        assertNull(evidenceFor(listOf(event())).teacherCheckedAt)
        assertEquals(now, evidenceFor(listOf(event(assessor = "teacher"))).teacherCheckedAt)
    }
    @Test fun importsStayUnknownAndAcquisitionDoesNotFollowLastOpenedSurah() {
        assertEquals(HifdhMastery.IMPORTED, evidenceFor(emptyList()).mastery)
        assertEquals(HifdhQueue.MANZIL, classifyEvidenceQueue(evidenceFor(emptyList()), null, now))
        assertEquals(HifdhQueue.SABQI, classifyEvidenceQueue(evidenceFor(listOf(event(now - 2 * 86_400_000))), null, now))
        assertEquals(HifdhQueue.MANZIL, classifyEvidenceQueue(evidenceFor(listOf(event(now - 10 * 86_400_000))), null, now))
    }
    @Test fun reviewingAnImportedAyahDoesNotInventAnAcquisitionDate() {
        val reviewed = event().copy(acquisition = false)
        val evidence = evidenceFor(listOf(reviewed))
        assertNull(evidence.learnedAt)
        assertEquals(HifdhMastery.RECALLED, evidence.mastery)
        assertEquals(HifdhQueue.MANZIL, classifyEvidenceQueue(evidence, null, now))
    }
}
