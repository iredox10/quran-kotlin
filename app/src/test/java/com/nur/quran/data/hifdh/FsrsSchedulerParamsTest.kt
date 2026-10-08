package com.nur.quran.data.hifdh

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins ts-fsrs 5.4.1 defaults used by web `fsrs({})`: w, retention, intervals. */
class FsrsSchedulerParamsTest {

    private val scheduler = FsrsScheduler()

    @Test
    fun `weights match ts-fsrs FSRS-6 default_w`() {
        val expected = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 1e-3,
            1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
            1.8729, 0.5425, 0.0912, 0.0658, 0.1542
        )
        assertEquals(expected.size, FSRS6_DEFAULT_W.size)
        expected.forEachIndexed { i, v -> assertEquals("w[$i]", v, FSRS6_DEFAULT_W[i], 0.0) }
    }

    @Test
    fun `request_retention 0_9 yields interval modifier 1_0`() {
        assertEquals(1.0, scheduler.intervalModifier, 1e-9)
    }

    @Test
    fun `intervals scale by stability and clamp to maximum_interval 36500`() {
        assertEquals(1, scheduler.nextInterval(1.0))
        assertEquals(2, scheduler.nextInterval(2.3065))
        assertEquals(8, scheduler.nextInterval(8.2956))
        assertEquals(36500, scheduler.nextInterval(100_000.0))
    }

    @Test
    fun `initial stability is S0 per grade`() {
        assertEquals(0.212, scheduler.initStability(FsrsRating.AGAIN), 0.0)
        assertEquals(1.2931, scheduler.initStability(FsrsRating.HARD), 0.0)
        assertEquals(2.3065, scheduler.initStability(FsrsRating.GOOD), 0.0)
        assertEquals(8.2956, scheduler.initStability(FsrsRating.EASY), 0.0)
    }

    @Test
    fun `initial difficulty is D0 per grade`() {
        assertEquals(6.4133, scheduler.initDifficulty(FsrsRating.AGAIN), 1e-8)
        assertEquals(5.11217071, scheduler.initDifficulty(FsrsRating.HARD), 1e-8)
        assertEquals(2.11810397, scheduler.initDifficulty(FsrsRating.GOOD), 1e-8)
        assertEquals(-4.7716307, scheduler.initDifficulty(FsrsRating.EASY), 1e-8)
    }

    @Test
    fun `forgetting curve matches ts-fsrs R(t,S)`() {
        assertEquals(1.0, scheduler.forgettingCurve(0.0, 2.3065), 1e-8)
        assertEquals(0.83886143, scheduler.forgettingCurve(5.0, 2.3065), 1e-8)
    }
}
