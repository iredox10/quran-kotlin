package com.nur.quran.data.audio

import android.content.Context
import com.nur.quran.data.db.entities.VerseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles per-verse audio downloads for offline listening, mirroring the web
 * app's "Download Audio" per-surah feature. Files are stored as
 * filesDir/audio/{SSS}{AAA}.mp3 (e.g. 001001.mp3), the same naming the web
 * app uses when building playlists.
 */
@Singleton
class AudioDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("audio_downloads", Context.MODE_PRIVATE)
    private val audioDir = File(context.filesDir, "audio")

    fun isDownloaded(chapterId: Int): Boolean =
        prefs.getStringSet(KEY_DOWNLOADED, emptySet())?.contains(chapterId.toString()) == true

    fun getDownloadedChapters(): Set<Int> =
        prefs.getStringSet(KEY_DOWNLOADED, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: emptySet()

    private fun markDownloaded(chapterId: Int) {
        val current = prefs.getStringSet(KEY_DOWNLOADED, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(chapterId.toString())
        prefs.edit().putStringSet(KEY_DOWNLOADED, current).apply()
    }

    /** Local file for a verse's audio, if it has been downloaded. */
    fun localFile(verseKey: String): File? {
        val file = File(audioDir, fileNameFor(verseKey))
        return if (file.exists() && file.length() > 0) file else null
    }

    /** Remote URL for a verse's audio (https://verses.quran.com/ prefix, like the web app). */
    fun remoteUrl(verse: VerseEntity): String? {
        val url = verse.audioUrl ?: return null
        return if (url.startsWith("http")) url else "https://verses.quran.com/$url"
    }

    /** Playable source for a verse: local file when downloaded, otherwise the remote URL. */
    fun playableSource(verse: VerseEntity): String? {
        localFile(verse.verseKey)?.let { return it.absolutePath }
        return remoteUrl(verse)
    }

    /**
     * Downloads every verse audio for a chapter in small chunks (like the web
     * app's chunkSize=5 loop). Returns true when everything downloaded.
     */
    suspend fun downloadChapter(
        chapterId: Int,
        verses: List<VerseEntity>,
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        audioDir.mkdirs()
        val targets = verses.mapNotNull { verse ->
            remoteUrl(verse)?.let { url -> url to File(audioDir, fileNameFor(verse.verseKey)) }
        }.filter { (_, file) -> !file.exists() || file.length() == 0L }

        val total = targets.size
        if (total == 0) {
            markDownloaded(chapterId)
            return@withContext true
        }

        var done = 0
        try {
            targets.chunked(5).forEach { chunk ->
                chunk.forEach { (url, file) ->
                    URL(url).openStream().use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    done++
                    onProgress(done, total)
                }
            }
            markDownloaded(chapterId)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun fileNameFor(verseKey: String): String {
        val parts = verseKey.split(":")
        val surah = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val ayah = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return "%03d%03d.mp3".format(surah, ayah)
    }

    private companion object {
        const val KEY_DOWNLOADED = "downloaded_chapters"
    }
}
