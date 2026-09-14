package com.nur.quran.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import com.nur.quran.R

/**
 * Playback notification [MediaNotification.Provider] for [QuranAudioService].
 *
 * Configures [DefaultMediaNotificationProvider] with:
 * - Small icon: [R.drawable.ic_logo] via [setSmallIcon].
 * - Notification Channel ID: [CHANNEL_ID] ("quran_playback_channel").
 * - Notification Channel Name: [CHANNEL_NAME] ("Quran Audio Playback", [R.string.quran_playback_channel_name]).
 *
 * Media3 1.2.1 compatibility:
 * - Extends [DefaultMediaNotificationProvider] invoking the 4-arg constructor:
 *   `context`, `notificationIdProvider`, `channelId`, and `channelNameResourceId`.
 * - Sets small icon to [R.drawable.ic_logo] in [init].
 * - Ensures the notification channel is created with low importance on Android O+ (API 26+).
 * - Artwork from [MediaMetadata] (configured as artwork data byte array and/or URI) is
 *   automatically loaded by [DefaultMediaNotificationProvider]'s bitmap loader and shown
 *   as the large icon in playback notifications.
 *
 * Wiring (in [QuranAudioService.onCreate], owned by the service):
 * ```
 * setMediaNotificationProvider(PlaybackNotificationProvider(this))
 * ```
 */
@UnstableApi
class PlaybackNotificationProvider(
    private val context: Context,
) : DefaultMediaNotificationProvider(
    context,
    NotificationIdProvider { DEFAULT_NOTIFICATION_ID },
    CHANNEL_ID,
    R.string.quran_playback_channel_name,
) {
    init {
        setSmallIcon(R.drawable.ic_logo)
        ensureNotificationChannel(context)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.quran_playback_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence? {
        return metadata.title ?: metadata.displayTitle ?: super.getNotificationContentTitle(metadata)
    }

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
        return metadata.artist ?: super.getNotificationContentText(metadata)
    }

    companion object {
        const val CHANNEL_ID = "quran_playback_channel"
        const val CHANNEL_NAME = "Quran Audio Playback"
        const val CHANNEL_DESCRIPTION = "Quran audio playback controls"
    }
}
