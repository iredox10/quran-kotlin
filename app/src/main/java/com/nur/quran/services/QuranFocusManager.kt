package com.nur.quran.services

import android.media.AudioManager

/**
 * Telegram-style audio focus management for Quran playback.
 *
 * Reference: Telegram Android (DrKLO/Telegram), MediaController.onAudioFocusChange
 * + MusicPlayerService — the model modern players use so a phone call pauses
 * playback, the player notification persists, and playback continues when the
 * call ends instead of the player closing.
 *
 * State machine (mirrors MediaController.onAudioFocusChange):
 * - AUDIOFOCUS_LOSS_TRANSIENT (incoming call rings, call audio takes over):
 *     pause the player and set resumeAudioOnFocusGain = true — but only when
 *     WE pause it. A manual user pause before/during the call stays paused.
 * - AUDIOFOCUS_GAIN (call ended, interruption over):
 *     restore ducked volume; if resumeAudioOnFocusGain, resume playback.
 * - AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK (navigation prompts, short dings):
 *     duck volume instead of pausing.
 * - AUDIOFOCUS_LOSS (permanent — another full player took over):
 *     pause; no auto-resume (Telegram behavior).
 *
 * Focus is kept while paused (Telegram keeps focus in pauseMessage and only
 * abandons in cleanupPlayer) so the transient-loss → GAIN sequence of a call
 * is always delivered back to us and playback can continue after the call.
 */
class QuranFocusManager(
    private val audio: FocusAudio,
    private val control: PlayerControl,
) : AudioManager.OnAudioFocusChangeListener {

    /** System audio seam so the state machine is unit-testable on the JVM. */
    interface FocusAudio {
        fun requestFocus(listener: AudioManager.OnAudioFocusChangeListener): Int
        fun abandonFocus(listener: AudioManager.OnAudioFocusChangeListener)
    }

    /** Player seam so the state machine is unit-testable on the JVM. */
    interface PlayerControl {
        fun isPlaying(): Boolean
        fun pause()
        fun play()
        fun setVolume(volume: Float)
    }

    private var resumeAudioOnFocusGain = false
    private var focusHeld = false

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                focusHeld = false
                resumeAudioOnFocusGain = false
                control.setVolume(VOLUME_NORMAL)
                if (control.isPlaying()) control.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusHeld = true
                control.setVolume(VOLUME_NORMAL)
                if (resumeAudioOnFocusGain) {
                    resumeAudioOnFocusGain = false
                    if (!control.isPlaying()) control.play()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (control.isPlaying()) {
                    control.pause()
                    resumeAudioOnFocusGain = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                control.setVolume(VOLUME_DUCK)
            }
        }
    }

    /**
     * Call right before starting playback. Returns true unless focus definitively
     * failed. DELAYED counts as success: the request is queued behind the current
     * holder (e.g. the call) and GAIN arrives when it clears.
     */
    fun requestPlaybackFocus(): Boolean {
        val result = audio.requestFocus(this)
        focusHeld = result != AudioManager.AUDIOFOCUS_REQUEST_FAILED
        return focusHeld
    }

    /** Call when playback stops entirely (never on pause). */
    fun abandonPlaybackFocus() {
        resumeAudioOnFocusGain = false
        if (focusHeld) {
            audio.abandonFocus(this)
            focusHeld = false
        }
    }

    companion object {
        const val VOLUME_NORMAL = 1.0f
        const val VOLUME_DUCK = 0.3f
    }
}
