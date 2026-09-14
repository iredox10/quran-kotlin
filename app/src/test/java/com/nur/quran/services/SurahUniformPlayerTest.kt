package com.nur.quran.services

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.nur.quran.data.mushaf.ChapterMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@UnstableApi
class SurahUniformPlayerTest {

    private class FakePlayerState {
        var mediaItemCount: Int = 0
        var currentMediaItemIndex: Int = 0
        var currentPosition: Long = 0L
        var duration: Long = 0L
        var bufferedPosition: Long = 0L

        var lastSeekPositionMs: Long? = null
        var lastSeekMediaItemIndex: Int? = null
        var lastSeekMediaItemPositionMs: Long? = null
    }

    private fun createFakePlayer(state: FakePlayerState): Player {
        val handler = InvocationHandler { _, method: Method, args: Array<out Any>? ->
            when (method.name) {
                "getMediaItemCount" -> state.mediaItemCount
                "getCurrentMediaItemIndex" -> state.currentMediaItemIndex
                "getCurrentPosition" -> state.currentPosition
                "getDuration" -> state.duration
                "getBufferedPosition" -> state.bufferedPosition
                "getContentPosition" -> state.currentPosition
                "getContentDuration" -> state.duration
                "seekTo" -> {
                    if (args != null && args.size == 1) {
                        state.lastSeekPositionMs = args[0] as Long
                    } else if (args != null && args.size == 2) {
                        state.lastSeekMediaItemIndex = args[0] as Int
                        state.lastSeekMediaItemPositionMs = args[1] as Long
                    }
                    null
                }
                "toString" -> "FakePlayer"
                "hashCode" -> 1
                "equals" -> false
                else -> {
                    when (method.returnType) {
                        java.lang.Boolean.TYPE -> false
                        java.lang.Integer.TYPE -> 0
                        java.lang.Long.TYPE -> 0L
                        java.lang.Float.TYPE -> 0f
                        java.lang.Double.TYPE -> 0.0
                        else -> null
                    }
                }
            }
        }
        return Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            handler,
        ) as Player
    }

    /**
     * Requirement 1: Test uniform progress calculation across a surah playlist:
     * - Verify that for a playlist of 78 ayahs (Surah Ar-Rahman), total duration is 780,000 ms.
     * - Verify that when Ayah 1 is at 50% (5,000 ms of 10,000 ms), position is 5,000 ms (0.64%).
     * - Verify that when advancing to Ayah 2 at 0 ms, position is 10,000 ms (1.28%) and does NOT jump back to 0.
     * - Verify that at Ayah 39 (50% through surah), position is ~390,000 ms (50%).
     * - Verify seek mapping: seeking to 390,000 ms maps to ayah index 39.
     */
    @Test
    fun testUniformProgressCalculationForArRahman() {
        val state = FakePlayerState().apply {
            mediaItemCount = 78 // Surah Ar-Rahman has 78 ayahs
        }
        val fakePlayer = createFakePlayer(state)
        val uniformPlayer = SurahUniformPlayer(fakePlayer)

        // 1. Verify total duration is 780,000 ms (78 * 10,000 ms)
        assertEquals(780_000L, uniformPlayer.duration)
        assertEquals(780_000L, uniformPlayer.contentDuration)

        // 2. Ayah 1 (index 0) at 50% (5,000 ms of 10,000 ms):
        // Position must be 5,000 ms (~0.64% of total surah)
        state.currentMediaItemIndex = 0
        state.duration = 10_000L
        state.currentPosition = 5_000L

        val posAyah1 = uniformPlayer.currentPosition
        assertEquals(5_000L, posAyah1)
        val percentageAyah1 = (posAyah1.toDouble() / uniformPlayer.duration) * 100.0
        assertEquals(0.641, percentageAyah1, 0.01) // 5,000 / 780,000 = ~0.64%

        // 3. Advancing to Ayah 2 (index 1) at 0 ms:
        // Position must be 10,000 ms (~1.28%) and must NOT jump back to 0
        state.currentMediaItemIndex = 1
        state.currentPosition = 0L

        val posAyah2 = uniformPlayer.currentPosition
        assertEquals(10_000L, posAyah2)
        assertNotEquals(0L, posAyah2)
        assertTrue("Position must be strictly greater than 0", posAyah2 > 0L)
        val percentageAyah2 = (posAyah2.toDouble() / uniformPlayer.duration) * 100.0
        assertEquals(1.282, percentageAyah2, 0.01) // 10,000 / 780,000 = ~1.28%

        // 4. Ayah 39 (index 39, 50% through the 78-ayah surah):
        // Position must be ~390,000 ms (50% of 780,000 ms)
        state.currentMediaItemIndex = 39
        state.currentPosition = 0L

        val posAyah39 = uniformPlayer.currentPosition
        assertEquals(390_000L, posAyah39)
        val percentageAyah39 = (posAyah39.toDouble() / uniformPlayer.duration) * 100.0
        assertEquals(50.0, percentageAyah39, 0.1) // 390,000 / 780,000 = 50%

        // 5. Seek mapping: seeking to 390,000 ms maps to ayah index 39
        uniformPlayer.seekTo(390_000L)
        assertEquals(39, state.lastSeekMediaItemIndex)
        assertEquals(0L, state.lastSeekMediaItemPositionMs)
    }

    /**
     * Requirement 2: Test title formatting:
     * - Surah 1, verse 1 -> "Al-Fatihah 1:1"
     * - Surah 55, verse 1 -> "Ar-Rahman 55:1"
     */
    @Test
    fun testTitleFormatting() {
        // Surah 1, verse 1 -> "Al-Fatihah 1:1"
        val surah1Name = ChapterMetadata.nameById(1)
        assertEquals("Al-Fatihah", surah1Name)
        val title1 = SurahUniformPlayer.formatTitle(1, 1)
        assertEquals("Al-Fatihah 1:1", title1)

        // Surah 55, verse 1 -> "Ar-Rahman 55:1"
        val surah55Name = ChapterMetadata.nameById(55)
        assertEquals("Ar-Rahman", surah55Name)
        val title55 = SurahUniformPlayer.formatTitle(55, 1)
        assertEquals("Ar-Rahman 55:1", title55)

        // Additional surah verifications
        assertEquals("Al-Baqarah 2:255", SurahUniformPlayer.formatTitle(2, 255))
        assertEquals("Ya-Sin 36:1", SurahUniformPlayer.formatTitle(36, 1))
        assertEquals("Al-Ikhlas 112:1", SurahUniformPlayer.formatTitle(112, 1))
        assertEquals("An-Nas 114:6", SurahUniformPlayer.formatTitle(114, 6))
    }

    @Test
    fun testDelegationWhenMediaItemCountIsOneOrLess() {
        val state = FakePlayerState().apply {
            mediaItemCount = 1
            currentPosition = 4_500L
            duration = 12_000L
            bufferedPosition = 6_000L
        }
        val fakePlayer = createFakePlayer(state)
        val uniformPlayer = SurahUniformPlayer(fakePlayer)

        assertEquals(4_500L, uniformPlayer.currentPosition)
        assertEquals(12_000L, uniformPlayer.duration)
        assertEquals(4_500L, uniformPlayer.contentPosition)
        assertEquals(12_000L, uniformPlayer.contentDuration)
        assertEquals(6_000L, uniformPlayer.bufferedPosition)
        assertEquals(6_000L, uniformPlayer.contentBufferedPosition)

        uniformPlayer.seekTo(3_000L)
        assertEquals(3_000L, state.lastSeekPositionMs)
    }

    @Test
    fun testUniformSeekingCalculations() {
        val state = FakePlayerState().apply {
            mediaItemCount = 4 // totalDuration = 40_000L ms
        }
        val fakePlayer = createFakePlayer(state)
        val uniformPlayer = SurahUniformPlayer(fakePlayer)

        // Seek to start (0 ms) -> index 0, position 0
        uniformPlayer.seekTo(0L)
        assertEquals(0, state.lastSeekMediaItemIndex)
        assertEquals(0L, state.lastSeekMediaItemPositionMs)

        // Seek to 15_000 ms (fraction = 15_000 / 40_000 = 0.375)
        // targetIndex = 0.375 * 4 = 1.5 -> targetIndex = 1
        // remainder = 1.5 - 1 = 0.5
        // targetAyahPos = 0.5 * 10_000 = 5_000 ms
        uniformPlayer.seekTo(15_000L)
        assertEquals(1, state.lastSeekMediaItemIndex)
        assertEquals(5_000L, state.lastSeekMediaItemPositionMs)

        // Seek with (index, position) directly
        uniformPlayer.seekTo(3, 2_500L)
        assertEquals(3, state.lastSeekMediaItemIndex)
        assertEquals(2_500L, state.lastSeekMediaItemPositionMs)
    }

    @Test
    fun testCurrentPositionFallbackWhenDurationZero() {
        val state = FakePlayerState().apply {
            mediaItemCount = 3
            currentMediaItemIndex = 1
            duration = 0L // Unset or 0 duration
            currentPosition = 5_000L
        }
        val fakePlayer = createFakePlayer(state)
        val uniformPlayer = SurahUniformPlayer(fakePlayer)

        // Fallback duration is 10_000L, so 5_000 / 10_000 = 50%
        // surahPos = (1 * 10_000) + (0.5 * 10_000) = 15_000 ms
        assertEquals(15_000L, uniformPlayer.currentPosition)
    }

    @Test
    fun testBufferedPositionCalculation() {
        val state = FakePlayerState().apply {
            mediaItemCount = 10 // total duration = 100_000 ms
            currentMediaItemIndex = 3 // 4th ayah
            duration = 10_000L
            bufferedPosition = 7_000L // 70% of 4th ayah buffered
        }
        val fakePlayer = createFakePlayer(state)
        val uniformPlayer = SurahUniformPlayer(fakePlayer)

        // Buffered position: (3 * 10_000) + (0.7 * 10_000) = 37_000 ms
        assertEquals(37_000L, uniformPlayer.bufferedPosition)
        assertEquals(37_000L, uniformPlayer.contentBufferedPosition)
    }
}
