package com.nur.quran.services

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Telegram-parity tests for the call/playback focus state machine in
 * [QuranFocusManager] (reference: DrKLO/Telegram MediaController.onAudioFocusChange).
 */
class QuranFocusManagerTest {

    private class FakeAudio : QuranFocusManager.FocusAudio {
        var abandoned = 0
        var nextResult = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        override fun requestFocus(listener: AudioManager.OnAudioFocusChangeListener): Int = nextResult
        override fun abandonFocus(listener: AudioManager.OnAudioFocusChangeListener) {
            abandoned++
        }
    }

    private class FakePlayer : QuranFocusManager.PlayerControl {
        var playing = false
        var appliedVolume = QuranFocusManager.VOLUME_NORMAL
        var pauses = 0
        var plays = 0
        override fun isPlaying() = playing
        override fun pause() {
            pauses++
            playing = false
        }

        override fun play() {
            plays++
            playing = true
        }

        override fun setVolume(volume: Float) {
            this.appliedVolume = volume
        }
    }

    private class Rig {
        val audio = FakeAudio()
        val player = FakePlayer()
        val manager = QuranFocusManager(audio, player)
    }

    @Test
    fun transientLossWhilePlayingPausesAndCallEndResumes() {
        val rig = Rig()
        rig.player.playing = true

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertEquals(1, rig.player.pauses)
        assertFalse(rig.player.playing)

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(1, rig.player.plays)
        assertTrue(rig.player.playing)
    }

    @Test
    fun gainWithoutResumeFlagDoesNotPlay() {
        val rig = Rig()
        // User-paused before the interruption: nothing to resume.
        rig.player.playing = false

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(0, rig.player.plays)
        assertFalse(rig.player.playing)
    }

    @Test
    fun manualPauseDuringCallDoesNotClearResumeFlag_telegramParity() {
        val rig = Rig()
        rig.player.playing = true

        // Call pauses it (resume flag set)…
        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        // …the user taps pause in the mini player during the call. The manual
        // pause goes through the player without clearing the pending resume
        // (same as Telegram, where the flag lives in MediaController)…
        rig.player.pause()
        // …so when the call ends, playback continues from the pause.
        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(1, rig.player.plays)
    }

    @Test
    fun canDuckLowersVolumeWithoutPausingAndGainRestores() {
        val rig = Rig()
        rig.player.playing = true

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertEquals(QuranFocusManager.VOLUME_DUCK, rig.player.appliedVolume, 0.001f)
        assertTrue(rig.player.playing)

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(QuranFocusManager.VOLUME_NORMAL, rig.player.appliedVolume, 0.001f)
        assertEquals(0, rig.player.pauses)
    }

    @Test
    fun permanentLossPausesAndNeverAutoResumes() {
        val rig = Rig()
        rig.player.playing = true

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertEquals(1, rig.player.pauses)

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(0, rig.player.plays)
        assertEquals(QuranFocusManager.VOLUME_NORMAL, rig.player.appliedVolume, 0.001f)
    }

    @Test
    fun delayedFocusCountsAsHeld_failedDoesNot() {
        val rig = Rig()
        rig.audio.nextResult = AudioManager.AUDIOFOCUS_REQUEST_DELAYED
        assertTrue(rig.manager.requestPlaybackFocus())

        rig.audio.nextResult = AudioManager.AUDIOFOCUS_REQUEST_FAILED
        assertFalse(rig.manager.requestPlaybackFocus())
    }

    @Test
    fun abandonPlaybackFocusAbandonsOnceAndLateGainNeverPlays() {
        val rig = Rig()
        rig.player.playing = true
        rig.manager.requestPlaybackFocus()

        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        rig.manager.abandonPlaybackFocus()
        rig.manager.abandonPlaybackFocus()

        assertEquals(1, rig.audio.abandoned)
        rig.manager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(0, rig.player.plays)
    }
}
