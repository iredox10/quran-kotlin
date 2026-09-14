package com.nur.quran.services

import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class PlaybackNotificationProviderTest {

    @Test
    fun channelConfigurationConstants() {
        assertEquals("quran_playback_channel", PlaybackNotificationProvider.CHANNEL_ID)
        assertEquals("Quran Audio Playback", PlaybackNotificationProvider.CHANNEL_NAME)
        assertEquals("Quran audio playback controls", PlaybackNotificationProvider.CHANNEL_DESCRIPTION)
    }

    @Test
    fun providerHierarchyAndConstructor() {
        assertTrue(
            "PlaybackNotificationProvider must inherit from DefaultMediaNotificationProvider",
            DefaultMediaNotificationProvider::class.java.isAssignableFrom(PlaybackNotificationProvider::class.java),
        )
        assertTrue(
            "PlaybackNotificationProvider must implement MediaNotification.Provider",
            MediaNotification.Provider::class.java.isAssignableFrom(PlaybackNotificationProvider::class.java),
        )

        val contextConstructor = PlaybackNotificationProvider::class.java.getDeclaredConstructor(
            android.content.Context::class.java,
        )
        assertNotNull("Must provide a (Context) constructor for QuranAudioService wiring", contextConstructor)
    }

    @Test
    fun metadataTitleAndTextResolution() {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        val provider = allocateMethod.invoke(unsafe, PlaybackNotificationProvider::class.java) as PlaybackNotificationProvider

        val titleMethod = PlaybackNotificationProvider::class.java.getDeclaredMethod(
            "getNotificationContentTitle",
            MediaMetadata::class.java,
        ).apply { isAccessible = true }

        val textMethod = PlaybackNotificationProvider::class.java.getDeclaredMethod(
            "getNotificationContentText",
            MediaMetadata::class.java,
        ).apply { isAccessible = true }

        val metadata = MediaMetadata.Builder()
            .setTitle("Al-Fatihah 1:1")
            .setDisplayTitle("Al-Fatihah 1:1")
            .setArtist("Mishari Rashid al-`Afasy")
            .build()

        val title = titleMethod.invoke(provider, metadata) as CharSequence?
        val text = textMethod.invoke(provider, metadata) as CharSequence?

        assertEquals("Al-Fatihah 1:1", title?.toString())
        assertEquals("Mishari Rashid al-`Afasy", text?.toString())
    }

    @Test
    fun metadataTitleFallsBackToDisplayTitle() {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        val provider = allocateMethod.invoke(unsafe, PlaybackNotificationProvider::class.java) as PlaybackNotificationProvider

        val titleMethod = PlaybackNotificationProvider::class.java.getDeclaredMethod(
            "getNotificationContentTitle",
            MediaMetadata::class.java,
        ).apply { isAccessible = true }

        val metadata = MediaMetadata.Builder()
            .setDisplayTitle("Fallback Title")
            .build()

        val title = titleMethod.invoke(provider, metadata) as CharSequence?
        assertEquals("Fallback Title", title?.toString())
    }
}
