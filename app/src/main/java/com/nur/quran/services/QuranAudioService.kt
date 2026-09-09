package com.nur.quran.services

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nur.quran.data.audio.AudioPlayerHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background audio service for Quran playback.
 *
 * Matches the `<service android:name=".services.QuranAudioService">` entry
 * in AndroidManifest.xml (MediaSessionService intent-filter,
 * foregroundServiceType="mediaPlayback").
 *
 * Uses the shared [AudioPlayerHolder] player so the Service and the
 * ViewModels control the same ExoPlayer instance. Contains no UI logic:
 * playback state is observed by the UI via the holder / a MediaController.
 */
@AndroidEntryPoint
class QuranAudioService : MediaSessionService() {

    @Inject
    lateinit var audioPlayerHolder: AudioPlayerHolder

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Lazy: the shared player is only built on first use, inside the holder.
        val player = audioPlayerHolder.getOrCreate(this)
        mediaSession = MediaSession.Builder(this, player)
            .setId("quran-audio")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Pause, don't stop: swiping the app away from recents keeps the
        // queue/session so playback can be resumed from UI or media controls.
        // Deliberately no stopSelf() here.
        audioPlayerHolder.getOrNull()?.pause()
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        audioPlayerHolder.release()
        super.onDestroy()
    }
}
