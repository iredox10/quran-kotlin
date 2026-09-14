package com.nur.quran.ui.components.audio

import kotlin.math.roundToInt

/**
 * Utility for estimating Quran audio recitation playback duration.
 *
 * An empirical average for a standard Murattal recitation is ~10 seconds per ayah at 1.0x speed.
 * For instance, Surah Al-Baqarah (286 ayahs) takes 286 * 10s = 2860s ≈ 48 minutes at 1.0x speed,
 * perfectly matching the reference design ("Play Full Surah • 48 min").
 */
object AudioDurationEstimator {

    /** Average recitation duration in seconds per ayah at 1.0x playback speed. */
    const val AVERAGE_SECONDS_PER_AYAH: Float = 10f

    /**
     * Estimates total duration in minutes given the number of ayahs and playback speed.
     * Always returns at least 1 minute.
     */
    fun estimateMinutes(ayahCount: Int, speed: Float = 1.0f): Int {
        val safeCount = ayahCount.coerceAtLeast(1)
        val safeSpeed = if (speed <= 0f) 1.0f else speed
        return ((safeCount * AVERAGE_SECONDS_PER_AYAH) / 60f / safeSpeed).roundToInt().coerceAtLeast(1)
    }

    /**
     * Estimates total duration in minutes given the start and end ayah numbers.
     */
    fun estimateMinutes(startAyah: Int, endAyah: Int, speed: Float = 1.0f): Int {
        val count = if (endAyah >= startAyah) (endAyah - startAyah + 1) else 1
        return estimateMinutes(count, speed)
    }

    /**
     * Estimates total duration in minutes with chapter and reciter context.
     */
    fun estimateMinutes(
        chapterId: Int,
        startAyah: Int,
        endAyah: Int,
        speed: Float = 1.0f,
        reciterId: Int = 7
    ): Int {
        return estimateMinutes(startAyah, endAyah, speed)
    }

    /**
     * Estimates total duration in seconds given the number of ayahs and playback speed.
     */
    fun estimateSeconds(ayahCount: Int, speed: Float = 1.0f): Int {
        val safeCount = ayahCount.coerceAtLeast(1)
        val safeSpeed = if (speed <= 0f) 1.0f else speed
        return ((safeCount * AVERAGE_SECONDS_PER_AYAH) / safeSpeed).roundToInt().coerceAtLeast(1)
    }

    /**
     * Estimates total duration in seconds given the start and end ayah numbers.
     */
    fun estimateSeconds(startAyah: Int, endAyah: Int, speed: Float = 1.0f): Int {
        val count = if (endAyah >= startAyah) (endAyah - startAyah + 1) else 1
        return estimateSeconds(count, speed)
    }

    /**
     * Formats duration as a human-readable string:
     * - If under 60 seconds: "$seconds sec" (e.g. "30 sec")
     * - Otherwise: "$minutes min" (e.g. "48 min")
     */
    fun estimateDuration(ayahCount: Int, speed: Float = 1.0f): String {
        val safeCount = ayahCount.coerceAtLeast(1)
        val safeSpeed = if (speed <= 0f) 1.0f else speed
        val totalSeconds = ((safeCount * AVERAGE_SECONDS_PER_AYAH) / safeSpeed).roundToInt().coerceAtLeast(1)

        return if (totalSeconds < 60) {
            "$totalSeconds sec"
        } else {
            val minutes = (totalSeconds / 60f).roundToInt().coerceAtLeast(1)
            "$minutes min"
        }
    }

    /**
     * Formats duration as a human-readable string given start and end ayah numbers.
     */
    fun estimateDuration(startAyah: Int, endAyah: Int, speed: Float = 1.0f): String {
        val count = if (endAyah >= startAyah) (endAyah - startAyah + 1) else 1
        return estimateDuration(count, speed)
    }

    /**
     * Formats duration as a human-readable string with chapter and reciter context.
     */
    fun estimateDuration(
        chapterId: Int,
        startAyah: Int,
        endAyah: Int,
        speed: Float = 1.0f,
        reciterId: Int = 7
    ): String {
        return estimateDuration(startAyah, endAyah, speed)
    }

    /**
     * Formats a given number of minutes into "$minutes min".
     */
    fun formatMinutes(durationMinutes: Int): String {
        return "${durationMinutes.coerceAtLeast(1)} min"
    }

    /**
     * Formats total seconds into a human-readable string:
     * - Under 60s: "$seconds sec"
     * - Under 60m: "$minutes min"
     * - 60m+: "$hours hr $minutes min" or "$hours hr"
     */
    fun formatHumanReadable(totalSeconds: Int): String {
        val safeSec = totalSeconds.coerceAtLeast(1)
        return when {
            safeSec < 60 -> "$safeSec sec"
            safeSec < 3600 -> "${(safeSec / 60f).roundToInt().coerceAtLeast(1)} min"
            else -> {
                val hours = safeSec / 3600
                val remainingMinutes = ((safeSec % 3600) / 60f).roundToInt()
                if (remainingMinutes == 0) "$hours hr" else "$hours hr $remainingMinutes min"
            }
        }
    }
}

/**
 * Top-level helper function to estimate duration in minutes.
 */
fun estimateAudioDurationMinutes(
    startAyah: Int,
    endAyah: Int,
    speed: Float = 1.0f
): Int = AudioDurationEstimator.estimateMinutes(startAyah, endAyah, speed)

/**
 * Top-level helper function to estimate duration formatted as a string.
 */
fun estimateAudioDuration(
    startAyah: Int,
    endAyah: Int,
    speed: Float = 1.0f
): String = AudioDurationEstimator.estimateDuration(startAyah, endAyah, speed)

/**
 * Top-level helper function to estimate duration formatted as a string with chapter context.
 */
fun estimateAudioDuration(
    chapterId: Int,
    startAyah: Int,
    endAyah: Int,
    speed: Float = 1.0f,
    reciterId: Int = 7
): String = AudioDurationEstimator.estimateDuration(chapterId, startAyah, endAyah, speed, reciterId)
