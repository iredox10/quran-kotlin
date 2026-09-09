package com.nur.quran.data.audio

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the single shared [ExoPlayer] instance for the app.
 *
 * Both [com.nur.quran.services.QuranAudioService] and the ViewModels
 * (e.g. SurahViewModel) obtain the player through here, so background
 * playback and UI controls never create double instances.
 *
 * The player is created lazily on the first [getOrCreate] call and always
 * built with the application context, so passing an Activity/Service
 * context never leaks it.
 */
@Singleton
class AudioPlayerHolder @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private var player: ExoPlayer? = null

    /** Returns the shared player, creating it on first use. */
    @Synchronized
    fun getOrCreate(context: Context = appContext): ExoPlayer {
        player?.let { return it }
        // applicationContext: never hold a reference to an Activity/Service context.
        return ExoPlayer.Builder(context.applicationContext).build().also {
            player = it
        }
    }

    /** Returns the shared player if already created, null otherwise. */
    @Synchronized
    fun getOrNull(): ExoPlayer? = player

    /**
     * Releases the shared player. Called from the service's onDestroy.
     * The next [getOrCreate] call builds a fresh instance.
     */
    @Synchronized
    fun release() {
        player?.release()
        player = null
    }
}
