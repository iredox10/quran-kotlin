package com.nur.quran.data.hifdh

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins web new/learning flow with steps 1m/10m: Good 10m, Again 1m, Hard 6m, Easy graduates. */
class FsrsSchedulerLearningTest {

    private val scheduler = FsrsScheduler()
    private val now = 1_700_000_000_000L

    @Test
    fun `new Good enters learning due in 10m`() {
        val next = scheduler.next(scheduler.createEmptyCard(now), now, FsrsRating.GOOD)
        assertEquals(FsrsState.LEARNING, next.state)
        assertEquals(1, next.learningSteps)
        assertEquals(now + 10 * 60_000L, next.due)
        assertEquals(2.3065, next.stability, 1e-8)
        assertEquals(2.11810397, next.difficulty, 1e-8)
    }

    @Test
    fun `new Again is 1m and new Hard averages 6m`() {
        val again = scheduler.next(scheduler.createEmptyCard(now), now, FsrsRating.AGAIN)
        assertEquals(now + 60_000L, again.due)
        assertEquals(0, again.learningSteps)
        val hard = scheduler.next(scheduler.createEmptyCard(now), now, FsrsRating.HARD)
        assertEquals(now + 6 * 60_000L, hard.due)
    }

    @Test
    fun `new Easy graduates to review with big stability jump`() {
        val next = scheduler.next(scheduler.createEmptyCard(now), now, FsrsRating.EASY)
        assertEquals(FsrsState.REVIEW, next.state)
        assertEquals(8, next.scheduledDays)
        assertEquals(8.2956, next.stability, 1e-8)
        assertEquals(1.0, next.difficulty, 0.0)
    }

    @Test
    fun `learning Good graduates to review after 2d`() {
        val learning = scheduler.next(scheduler.createEmptyCard(now), now, FsrsRating.GOOD)
        val next = scheduler.next(learning, now, FsrsRating.GOOD)
        assertEquals(FsrsState.REVIEW, next.state)
        assertEquals(2, next.scheduledDays)
    }
}
