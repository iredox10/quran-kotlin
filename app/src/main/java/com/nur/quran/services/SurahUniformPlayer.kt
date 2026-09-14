package com.nur.quran.services

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

import com.nur.quran.data.mushaf.ChapterMetadata

/**
 * A [ForwardingPlayer] that presents a uniform progress bar across an entire Surah playlist.
 *
 * Rather than resetting the progress bar to 0 for each individual ayah, this player maps
 * playback uniformly across the entire Surah playlist:
 * - Each ayah is assigned a standard duration slice [AYAH_DURATION_SLICE_MS] (10 seconds).
 * - Total duration is `mediaItemCount * AYAH_DURATION_SLICE_MS`.
 * - Current position is `(currentIdx * AYAH_DURATION_SLICE_MS) + (ayahFraction * AYAH_DURATION_SLICE_MS)`.
 * - Seeking maps uniformly back to the corresponding ayah index and offset.
 *
 * When [mediaItemCount] <= 1, delegates directly to [basePlayer].
 */
@UnstableApi
class SurahUniformPlayer(
    private val basePlayer: Player,
) : ForwardingPlayer(basePlayer) {

    companion object {
        const val AYAH_DURATION_SLICE_MS: Long = 10_000L

        /**
         * Formats a verse title with the surah's proper name:
         * e.g., "Al-Fatihah 1:1" or "Ar-Rahman 55:1".
         */
        fun formatTitle(chapterId: Int, verseNumber: Int): String {
            val surahName = ChapterMetadata.nameById(chapterId)
            return "$surahName $chapterId:$verseNumber"
        }
    }

    override fun getCurrentPosition(): Long {
        if (mediaItemCount <= 1) {
            return basePlayer.currentPosition
        }
        val totalDuration = mediaItemCount * AYAH_DURATION_SLICE_MS
        val currentIdx = basePlayer.currentMediaItemIndex.coerceAtLeast(0)
        val ayahDuration = basePlayer.duration.takeIf { it > 0 } ?: AYAH_DURATION_SLICE_MS
        val ayahPos = basePlayer.currentPosition.coerceIn(0L, ayahDuration)
        val ayahFraction = (ayahPos.toDouble() / ayahDuration).coerceIn(0.0, 1.0)
        val surahPos = (currentIdx * AYAH_DURATION_SLICE_MS) + (ayahFraction * AYAH_DURATION_SLICE_MS).toLong()
        return surahPos.coerceIn(0L, totalDuration)
    }

    override fun getDuration(): Long =
        if (mediaItemCount > 1) mediaItemCount * AYAH_DURATION_SLICE_MS else super.getDuration()

    override fun getContentPosition(): Long = currentPosition

    override fun getContentDuration(): Long = duration

    override fun getBufferedPosition(): Long {
        if (mediaItemCount <= 1) {
            return super.getBufferedPosition()
        }
        val totalDuration = mediaItemCount * AYAH_DURATION_SLICE_MS
        val currentIdx = basePlayer.currentMediaItemIndex.coerceAtLeast(0)
        val ayahDuration = basePlayer.duration.takeIf { it > 0 } ?: AYAH_DURATION_SLICE_MS
        val ayahPos = basePlayer.bufferedPosition.coerceIn(0L, ayahDuration)
        val ayahFraction = (ayahPos.toDouble() / ayahDuration).coerceIn(0.0, 1.0)
        val surahPos = (currentIdx * AYAH_DURATION_SLICE_MS) + (ayahFraction * AYAH_DURATION_SLICE_MS).toLong()
        return surahPos.coerceIn(0L, totalDuration)
    }

    override fun getContentBufferedPosition(): Long = bufferedPosition

    override fun seekTo(positionMs: Long) {
        if (mediaItemCount <= 1) {
            basePlayer.seekTo(positionMs)
            return
        }
        val totalDuration = mediaItemCount * AYAH_DURATION_SLICE_MS
        val fraction = (positionMs.toDouble() / totalDuration).coerceIn(0.0, 0.9999)
        val targetIndex = (fraction * mediaItemCount).toInt().coerceIn(0, mediaItemCount - 1)
        val remainder = (fraction * mediaItemCount) - targetIndex
        val targetAyahDuration = AYAH_DURATION_SLICE_MS
        val targetAyahPos = (remainder * targetAyahDuration).toLong()
        basePlayer.seekTo(targetIndex, targetAyahPos)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        basePlayer.seekTo(mediaItemIndex, positionMs)
    }
}
