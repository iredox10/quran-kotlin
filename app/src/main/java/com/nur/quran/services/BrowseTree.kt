package com.nur.quran.services

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nur.quran.data.audio.Reciters

/**
 * Dependency-free Android Auto / Automotive browse tree.
 *
 * No database, no Hilt, no context: browses [Reciters.ALL] (no DB) and
 * synthesizes per-reciter surah entries. Surah titles are generic
 * ("Surah N") — there is no static surah-name table in the app
 * (QuranNavigation.kt holds Juz/Hizb starts only), and chapter names
 * otherwise come from Room via QuranRepository.
 *
 * mediaId scheme: "auto:<reciterId>:<surah>" for playable surah items,
 * "auto:<reciterId>" for browsable reciter folders.
 *
 * Surah items intentionally carry NO uri: the sibling service
 * (QuranAudioService) resolves "auto:r:s" into full playlists via its
 * existing VerseEntity flow on play. Do not point them at
 * Reciters.buildAudioUrl(reciterId, "$s:1") — that is a single-ayah URL.
 */
object BrowseTree {
    const val ROOT_ID = "root"
    const val RECITERS_ID = "reciters"

    const val PREFIX = "auto"
    const val SURAH_COUNT = 114

    /** Root → [Reciters folder]. */
    fun rootChildren(): List<MediaItem> = listOf(recitersFolder())

    /** Reciters folder → one browsable folder per reciter in [Reciters.ALL]. */
    fun recitersChildren(): List<MediaItem> =
        Reciters.ALL.map { reciter -> reciterFolder(reciter.id) }

    /**
     * Reciter folder → 114 playable surah items.
     * Each item carries ONLY mediaId ("auto:r:s") + title + artist;
     * uri is left empty so the service resolves full playlists on play.
     */
    fun reciterChildren(reciterId: Int): List<MediaItem> {
        val artist = Reciters.nameOf(reciterId)
        return (1..SURAH_COUNT).map { surah -> surahItem(reciterId, surah, artist) }
    }

    fun recitersFolder(): MediaItem =
        MediaItem.Builder()
            .setMediaId(RECITERS_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Reciters")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    fun reciterFolder(reciterId: Int): MediaItem {
        val reciter = Reciters.byId(reciterId)
        return MediaItem.Builder()
            .setMediaId(reciterMediaId(reciterId))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(reciter?.name ?: "Reciter $reciterId")
                    .setArtist(reciter?.style)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()
    }

    fun surahItem(reciterId: Int, surah: Int, artist: String = Reciters.nameOf(reciterId)): MediaItem =
        MediaItem.Builder()
            .setMediaId(surahMediaId(reciterId, surah))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Surah $surah")
                    .setArtist(artist)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    fun reciterMediaId(reciterId: Int): String = "$PREFIX:$reciterId"

    fun surahMediaId(reciterId: Int, surah: Int): String = "$PREFIX:$reciterId:$surah"

    /** Parse a playable "auto:r:s" id → (reciterId, surah), or null. */
    fun parseSurahItem(mediaId: String): Pair<Int, Int>? {
        val parts = mediaId.split(":")
        if (parts.size != 3 || parts[0] != PREFIX) return null
        val reciterId = parts[1].toIntOrNull() ?: return null
        val surah = parts[2].toIntOrNull()?.takeIf { it in 1..SURAH_COUNT } ?: return null
        return reciterId to surah
    }

    /** Parse a browsable "auto:r" reciter-folder id → reciterId, or null. */
    fun parseReciterFolder(mediaId: String): Int? {
        val parts = mediaId.split(":")
        if (parts.size != 2 || parts[0] != PREFIX) return null
        return parts[1].toIntOrNull()
    }
}
