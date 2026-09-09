package com.nur.quran.data.audio

/**
 * Local-first playback options for [com.nur.quran.ui.viewmodels.SurahViewModel].
 *
 * Repeat convention (shared with the hifdh chunk loop):
 * - `1` = play once (default),
 * - `n > 1` = repeat n times,
 * - `-1` = repeat indefinitely.
 */
data class PlaybackSettings(
    val ayahRepeat: Int = 1,
    val rangeRepeat: Int = 1,
    val delayMs: Long = 0L,
    val speed: Float = 1f,
    val rangeStart: String? = null,
    val rangeEnd: String? = null,
    val streamOnly: Boolean = false
) {
    companion object {
        const val REPEAT_INFINITE = -1
    }
}
