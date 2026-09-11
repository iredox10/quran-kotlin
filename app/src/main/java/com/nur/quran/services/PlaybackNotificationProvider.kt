package com.nur.quran.services

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification

/**
 * Playback notification [MediaNotification.Provider] for [QuranAudioService].
 *
 * Thin extension point over [DefaultMediaNotificationProvider]: no behavior overrides.
 * The default provider already handles the channel creation, small icon (app icon),
 * content intent from the session activity, ongoing state, and media actions.
 *
 * Subclassing (rather than implementing [MediaNotification.Provider] manually or
 * delegating + overriding `getMediaNotification`) keeps this compiling against
 * media3-session 1.2.1, where only `DefaultMediaNotificationProvider(Context)` is
 * guaranteed stable — the `Builder`/channel-name APIs arrived later, and a manual
 * `getMediaNotification` override would couple us to its exact
 * `ListenableFuture` signature.
 *
 * Wiring (in [QuranAudioService.onCreate], owned by the service):
 * ```
 * setMediaNotificationProvider(PlaybackNotificationProvider(this))
 * ```
 *
 * @param context any Context; passed to the default provider for channel +
 * notification building. Kept as a private property per the service contract so
 * future overrides (custom small icon, channel name) can use it.
 */
@UnstableApi
class PlaybackNotificationProvider(
    private val context: Context,
) : DefaultMediaNotificationProvider(context)
