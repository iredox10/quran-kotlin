package com.nur.quran.ui.components.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDurationEstimatorTest {

    @Test
    fun testSurahAlBaqarahFullDurationMatchesMockup() {
        // Al-Baqarah has 286 ayahs
        // At 10s per ayah: 286 * 10 = 2860s = 47.6667 min -> 48 min
        val minutes = AudioDurationEstimator.estimateMinutes(ayahCount = 286, speed = 1.0f)
        assertEquals(48, minutes)

        val formatted = AudioDurationEstimator.estimateDuration(ayahCount = 286, speed = 1.0f)
        assertEquals("48 min", formatted)
    }

    @Test
    fun testSpeedVariations() {
        // 286 ayahs at 2.0x -> 2860 / 60 / 2 = 23.83 -> 24 min
        val minutes2x = AudioDurationEstimator.estimateMinutes(286, 2.0f)
        assertEquals(24, minutes2x)

        // 286 ayahs at 0.5x -> 2860 / 60 / 0.5 = 95.33 -> 95 min
        val minutesHalfSpeed = AudioDurationEstimator.estimateMinutes(286, 0.5f)
        assertEquals(95, minutesHalfSpeed)

        // 286 ayahs at 1.5x -> 2860 / 60 / 1.5 = 31.77 -> 32 min
        val minutes1_5x = AudioDurationEstimator.estimateMinutes(286, 1.5f)
        assertEquals(32, minutes1_5x)
    }

    @Test
    fun testShortRangeSecondsFormatting() {
        // Single ayah (10s) -> should format as "10 sec"
        val singleAyah = AudioDurationEstimator.estimateDuration(startAyah = 1, endAyah = 1, speed = 1.0f)
        assertEquals("10 sec", singleAyah)

        // 3 ayahs (30s) -> "30 sec"
        val threeAyahs = AudioDurationEstimator.estimateDuration(startAyah = 1, endAyah = 3, speed = 1.0f)
        assertEquals("30 sec", threeAyahs)

        // 5 ayahs (50s) -> "50 sec"
        val fiveAyahs = AudioDurationEstimator.estimateDuration(startAyah = 1, endAyah = 5, speed = 1.0f)
        assertEquals("50 sec", fiveAyahs)

        // 6 ayahs (60s) -> "1 min"
        val sixAyahs = AudioDurationEstimator.estimateDuration(startAyah = 1, endAyah = 6, speed = 1.0f)
        assertEquals("1 min", sixAyahs)
    }

    @Test
    fun testRangeAyahCalculation() {
        // 1 to 20 = 20 ayahs = 200s = 3.33 min -> 3 min
        val minutes = AudioDurationEstimator.estimateMinutes(startAyah = 1, endAyah = 20, speed = 1.0f)
        assertEquals(3, minutes)

        val durationStr = AudioDurationEstimator.estimateDuration(startAyah = 1, endAyah = 20, speed = 1.0f)
        assertEquals("3 min", durationStr)
    }

    @Test
    fun testEdgeCases() {
        // Zero or negative ayah counts coerce to at least 1
        val zeroCount = AudioDurationEstimator.estimateMinutes(ayahCount = 0, speed = 1.0f)
        assertEquals(1, zeroCount)

        // Inverted range (endAyah < startAyah)
        val invertedRange = AudioDurationEstimator.estimateMinutes(startAyah = 10, endAyah = 5, speed = 1.0f)
        assertEquals(1, invertedRange)

        // Zero or negative speed
        val zeroSpeed = AudioDurationEstimator.estimateMinutes(ayahCount = 10, speed = 0f)
        assertEquals(2, zeroSpeed) // Fallback to 1.0x
    }
}
