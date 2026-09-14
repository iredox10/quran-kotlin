package com.nur.quran.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nur.quran.MainActivity
import com.nur.quran.data.mushaf.ChapterMetadata

/**
 * Background audio service for Quran playback (music-player style).
 *
 * A [MediaLibraryService] (IS-A [MediaSessionService], so plain controllers
 * still work) that owns its ExoPlayer directly — no shared holder. Kept
 * around in the background so playback continues when the app is swiped
 * away from recents.
 *
 * Library browsing delegates to [BrowseTree]; the foreground notification
 * comes from [PlaybackNotificationProvider]; headset-unplug pauses come
 * from [PlaybackNoisyHandler].
 */
class QuranAudioService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaLibrarySession? = null
    private var noisyReceiver: BroadcastReceiver? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_SPEECH)
            .build()
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()
        player = exoPlayer

        val uniformPlayer = SurahUniformPlayer(exoPlayer)

        val sessionActivity = createSessionActivityPendingIntent(null)
        val session = MediaLibrarySession.Builder(this, uniformPlayer, LibraryCallback())
            .setId("quran-audio")
            .setSessionActivity(sessionActivity)
            .build()
        mediaSession = session

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val pendingIntent = createSessionActivityPendingIntent(mediaItem?.mediaId)
                mediaSession?.setSessionActivity(pendingIntent)
            }
        })

        setMediaNotificationProvider(PlaybackNotificationProvider(this))

        noisyReceiver = PlaybackNoisyHandler().register(this, exoPlayer)
    }

    private fun createSessionActivityPendingIntent(verseKey: String?): PendingIntent {
        val parts = verseKey?.split(":")
        val chapterId = parts?.getOrNull(0)?.toIntOrNull() ?: 1
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.nur.quran.action.VIEW_VERSE"
            putExtra("chapterId", chapterId)
            putExtra("verseKey", verseKey)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Intentionally nothing: keep playing like music players.
        // No pause, no stopSelf().
    }

    override fun onDestroy() {
        noisyReceiver?.let { PlaybackNoisyHandler().unregister(this, it) }
        noisyReceiver = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        runCatching { libraryDb.close() }
        libraryIo.shutdown()
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = BrowseTree.rootChildren().firstOrNull()
                ?: MediaItem.Builder().setMediaId(BrowseTree.ROOT_ID).build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when (parentId) {
                BrowseTree.ROOT_ID -> BrowseTree.rootChildren()
                BrowseTree.RECITERS_ID -> BrowseTree.recitersChildren()
                else -> BrowseTree.parseSurahItem(parentId)?.let { emptyList<MediaItem>() }
                    ?: BrowseTree.parseReciterFolder(parentId)?.let { BrowseTree.reciterChildren(it) }
                    ?: emptyList()
            }
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params),
            )
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            // Auto tap on a surah arrives with mediaId only ("auto:r:s").
            // Resolve it to the full per-ayah playlist here so playback works.
            return libraryIo.submit<List<MediaItem>> {
                mediaItems.flatMap { item ->
                    BrowseTree.parseSurahItem(item.mediaId)?.let { (reciterId, surah) ->
                        resolveSurahItems(reciterId, surah)
                    } ?: listOf(item)
                }
            }
        }
    }

    private val libraryIo =
        MoreExecutors.listeningDecorator(java.util.concurrent.Executors.newSingleThreadExecutor())

    private val libraryDb by lazy {
        androidx.room.Room.databaseBuilder(
            this,
            com.nur.quran.data.db.QuranDatabase::class.java,
            "quran_database",
        ).fallbackToDestructiveMigration().build()
    }

    private fun resolveSurahItems(reciterId: Int, surah: Int): List<MediaItem> {
        return runCatching {
            val context = this
            val artworkUri = Uri.parse("android.resource://${context.packageName}/${com.nur.quran.R.drawable.ic_logo}")
            val artworkBytes = runCatching { context.resources.openRawResource(com.nur.quran.R.drawable.ic_logo).use { it.readBytes() } }.getOrNull()
            val surahName = com.nur.quran.data.mushaf.ChapterMetadata.nameById(surah)
            val manager = com.nur.quran.data.audio.AudioDownloadManager(this)
            val verses = kotlinx.coroutines.runBlocking {
                libraryDb.quranDao().getVersesByChapterDirect(surah)
            }
            verses.mapNotNull { verse ->
                val uri = manager.localFile(reciterId, verse.verseKey)?.let {
                    Uri.fromFile(it).toString()
                } ?: com.nur.quran.data.audio.Reciters.buildAudioUrl(reciterId, verse.verseKey)
                    ?: return@mapNotNull null
                val formattedTitle = "$surahName $surah:${verse.verseNumber}"
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(verse.verseKey)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(formattedTitle)
                            .setDisplayTitle(formattedTitle)
                            .setArtist(com.nur.quran.data.audio.Reciters.nameOf(reciterId))
                            .setAlbumTitle("Surah $surahName")
                            .setArtworkUri(artworkUri)
                            .apply { if (artworkBytes != null) setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
                            .build(),
                    )
                    .build()
            }
        }.getOrDefault(emptyList())
    }
}
