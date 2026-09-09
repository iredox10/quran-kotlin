package com.nur.quran.data.audio

import android.content.Context
import com.nur.quran.data.db.entities.VerseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-download progress snapshot, keyed in [AudioDownloadManager.downloadState]
 * by `"reciterId:chapterId"`.
 */
data class DownloadProgress(
    val downloaded: Int = 0,
    val total: Int = 0,
    val isDownloading: Boolean = false,
    val error: String? = null
)

/**
 * Handles per-verse audio downloads for offline listening, mirroring the web
 * app's "Download Audio" per-surah feature.
 *
 * GreenTech-style per-reciter storage: files are stored as
 * `filesDir/audio/<reciterId>/{SSS}{AAA}.mp3` (e.g. `audio/7/001001.mp3`),
 * the same SSSAAA naming the web app uses when building playlists.
 *
 * Backward compatibility: verses downloaded by the old reciter-agnostic
 * layout (`filesDir/audio/{SSS}{AAA}.mp3`) are still found by the legacy
 * [localFile]/[playableSource] overloads and count toward
 * [isFullyDownloaded].
 */
@Singleton
class AudioDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("audio_downloads", Context.MODE_PRIVATE)
    private val audioDir = File(context.filesDir, "audio")

    private val _downloadState = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    /** Live download progress per `"reciterId:chapterId"` key. */
    val downloadState: StateFlow<Map<String, DownloadProgress>> = _downloadState.asStateFlow()

    private val jobs = ConcurrentHashMap<String, Job>()

    // ── Downloaded-chapter prefs ──────────────────────────────────────────

    /** Legacy (reciter-agnostic) chapters. Kept so old callers keep working. */
    fun isDownloaded(chapterId: Int): Boolean {
        if (prefs.getStringSet(KEY_DOWNLOADED, emptySet())?.contains(chapterId.toString()) == true) {
            return true
        }
        // Aggregate per-reciter marks so legacy callers see new downloads too.
        return prefs.all.keys
            .filter { it.startsWith(KEY_DOWNLOADED_PREFIX) }
            .any { key ->
                prefs.getStringSet(key, emptySet())?.contains(chapterId.toString()) == true
            }
    }

    /** Union of legacy + all per-reciter downloaded chapters. */
    fun getDownloadedChapters(): Set<Int> {
        val out = mutableSetOf<Int>()
        prefs.getStringSet(KEY_DOWNLOADED, emptySet())?.mapNotNullTo(out) { it.toIntOrNull() }
        prefs.all.keys
            .filter { it.startsWith(KEY_DOWNLOADED_PREFIX) }
            .forEach { key ->
                prefs.getStringSet(key, emptySet())?.mapNotNullTo(out) { it.toIntOrNull() }
            }
        return out
    }

    fun isDownloaded(reciterId: Int, chapterId: Int): Boolean =
        prefs.getStringSet(downloadedKey(reciterId), emptySet())?.contains(chapterId.toString()) == true

    fun getDownloadedChapters(reciterId: Int): Set<Int> =
        prefs.getStringSet(downloadedKey(reciterId), emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: emptySet()

    private fun markDownloaded(reciterId: Int, chapterId: Int) {
        val key = downloadedKey(reciterId)
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(chapterId.toString())
        prefs.edit().putStringSet(key, current).apply()
    }

    private fun unmarkDownloaded(reciterId: Int, chapterId: Int) {
        val key = downloadedKey(reciterId)
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: return
        if (current.remove(chapterId.toString())) {
            prefs.edit().putStringSet(key, current).apply()
        }
    }

    // ── File locations ────────────────────────────────────────────────────

    private fun reciterDir(reciterId: Int): File = File(audioDir, reciterId.toString())

    private fun legacyFile(verseKey: String): File? {
        val file = File(audioDir, fileNameFor(verseKey))
        return if (file.exists() && file.length() > 0) file else null
    }

    /** Local file for a verse's audio under the given reciter, if downloaded. */
    fun localFile(reciterId: Int, verseKey: String): File? {
        val file = File(reciterDir(reciterId), fileNameFor(verseKey))
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Legacy reciter-agnostic lookup: checks the old
     * `filesDir/audio/SSSAAA.mp3` location first, then every per-reciter
     * `filesDir/audio/<reciterId>/SSSAAA.mp3` folder.
     */
    fun localFile(verseKey: String): File? {
        legacyFile(verseKey)?.let { return it }
        val name = fileNameFor(verseKey)
        audioDir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { dir ->
                val file = File(dir, name)
                if (file.exists() && file.length() > 0) return file
            }
        return null
    }

    /** Remote URL for a verse's audio (https://verses.quran.com/ prefix, like the web app). */
    fun remoteUrl(verse: VerseEntity): String? {
        val url = verse.audioUrl ?: return null
        return if (url.startsWith("http")) url else "https://verses.quran.com/$url"
    }

    /** Playable source for a verse: per-reciter local file when downloaded, otherwise the remote URL. */
    fun playableSource(reciterId: Int, verse: VerseEntity): String? {
        localFile(reciterId, verse.verseKey)?.let { return it.absolutePath }
        // Fall back to a legacy-layout file from before per-reciter storage.
        legacyFile(verse.verseKey)?.let { return it.absolutePath }
        return remoteUrl(verse)
    }

    /** Legacy playable source: local file when downloaded, otherwise the remote URL. */
    fun playableSource(verse: VerseEntity): String? {
        localFile(verse.verseKey)?.let { return it.absolutePath }
        return remoteUrl(verse)
    }

    // ── Downloads ─────────────────────────────────────────────────────────

    /**
     * Downloads every verse audio for a chapter under the given reciter,
     * skipping files that already exist. Progress counts already-present
     * files, so [onProgress]/[downloadState] start at the resumed position.
     *
     * Each file is written to `.tmp` then renamed (fsync-safe); a failed
     * file deletes its partial `.tmp` and is reported via
     * [DownloadProgress.error] while the rest continue. The chapter is only
     * marked downloaded when every file is present. Returns true when
     * everything downloaded.
     */
    suspend fun downloadChapter(
        reciterId: Int,
        chapterId: Int,
        verses: List<VerseEntity>,
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val key = keyFor(reciterId, chapterId)
        val currentJob = currentCoroutineContext()[Job]
        if (currentJob != null) jobs[key] = currentJob
        try {
            val dir = reciterDir(reciterId).apply { mkdirs() }
            val targets = verses.mapNotNull { verse ->
                remoteUrl(verse)?.let { url -> Triple(verse.verseKey, url, File(dir, fileNameFor(verse.verseKey))) }
            }
            val total = targets.size
            if (total == 0) {
                markDownloaded(reciterId, chapterId)
                _downloadState.update { it + (key to DownloadProgress(0, 0, false, null)) }
                return@withContext true
            }

            var done = targets.count { (_, _, file) -> file.exists() && file.length() > 0 }
            var lastError: String? = null
            _downloadState.update { it + (key to DownloadProgress(done, total, true, null)) }
            onProgress(done, total)

            targets.filter { (_, _, file) -> !file.exists() || file.length() == 0L }
                .chunked(5)
                .forEach { chunk ->
                    chunk.forEach { (_, url, file) ->
                        ensureActive()
                        try {
                            downloadUrlToFile(url, file)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            lastError = e.message ?: e::class.simpleName
                        }
                        if (file.exists() && file.length() > 0) done++
                        _downloadState.update {
                            it + (key to DownloadProgress(done, total, true, lastError))
                        }
                        onProgress(done, total)
                    }
                }

            val allPresent = targets.all { (_, _, file) -> file.exists() && file.length() > 0 }
            if (allPresent) {
                markDownloaded(reciterId, chapterId)
                _downloadState.update { it + (key to DownloadProgress(total, total, false, null)) }
                true
            } else {
                unmarkDownloaded(reciterId, chapterId)
                _downloadState.update {
                    it + (key to DownloadProgress(done, total, false, lastError ?: "download_failed"))
                }
                false
            }
        } catch (e: CancellationException) {
            _downloadState.update { current ->
                val prev = current[key]
                current + (key to (prev?.copy(isDownloading = false, error = null)
                    ?: DownloadProgress(0, 0, false, null)))
            }
            throw e
        } finally {
            if (currentJob != null) jobs.remove(key, currentJob) else jobs.remove(key)
        }
    }

    /** Legacy overload: downloads into the default reciter's folder. */
    suspend fun downloadChapter(
        chapterId: Int,
        verses: List<VerseEntity>,
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = downloadChapter(Reciters.DEFAULT_ID, chapterId, verses, onProgress)

    /** Cancels an in-flight [downloadChapter] for the given reciter + chapter. */
    fun cancelDownload(reciterId: Int, chapterId: Int) {
        jobs[keyFor(reciterId, chapterId)]?.cancel()
    }

    fun isDownloading(reciterId: Int, chapterId: Int): Boolean =
        _downloadState.value[keyFor(reciterId, chapterId)]?.isDownloading == true

    // ── Deletion / storage / completeness ─────────────────────────────────

    /** Deletes downloaded audio for one chapter under the given reciter. */
    fun deleteChapter(reciterId: Int, chapterId: Int): Boolean {
        val dir = reciterDir(reciterId)
        val prefix = "%03d".format(chapterId)
        var deletedAny = false
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(prefix) &&
                (file.name.endsWith(".mp3") || file.name.endsWith(".tmp"))
            ) {
                if (file.delete()) deletedAny = true
            }
        }
        unmarkDownloaded(reciterId, chapterId)
        _downloadState.update { it - keyFor(reciterId, chapterId) }
        return deletedAny
    }

    /** Deletes all downloaded audio for a reciter (cancels its in-flight downloads first). */
    fun deleteReciter(reciterId: Int): Boolean {
        val prefix = "$reciterId:"
        jobs.entries
            .filter { it.key.startsWith(prefix) }
            .forEach { it.value.cancel() }
        val ok = reciterDir(reciterId).deleteRecursively()
        prefs.edit().remove(downloadedKey(reciterId)).apply()
        _downloadState.update { current -> current.filterKeys { !it.startsWith(prefix) } }
        return ok
    }

    /**
     * Total bytes of downloaded audio. Pass null to sum every reciter
     * (plus any legacy root-level files); partial `.tmp` files are excluded.
     */
    fun getStorageBytes(reciterId: Int? = null): Long {
        val root = if (reciterId == null) audioDir else reciterDir(reciterId)
        if (!root.exists()) return 0L
        return root.walkTopDown()
            .filter { it.isFile && !it.name.endsWith(".tmp") }
            .sumOf { it.length() }
    }

    /**
     * True when every given verse has a downloaded file under the reciter
     * (legacy root-level files count too, for pre-migration downloads).
     */
    fun isFullyDownloaded(reciterId: Int, chapterId: Int, verses: List<VerseEntity>): Boolean {
        if (verses.isEmpty()) return isDownloaded(reciterId, chapterId)
        val dir = reciterDir(reciterId)
        return verses.all { verse ->
            val name = fileNameFor(verse.verseKey)
            val file = File(dir, name)
            if (file.exists() && file.length() > 0) {
                true
            } else {
                val legacy = File(audioDir, name)
                legacy.exists() && legacy.length() > 0
            }
        }
    }

    // ── Network + naming helpers ──────────────────────────────────────────

    @Throws(IOException::class)
    private fun downloadUrlToFile(url: String, dest: File) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parent, "${dest.name}.tmp")
        var connection: HttpURLConnection? = null
        try {
            connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "QuranApp/1.0")
                connect()
                if (responseCode !in 200..299) {
                    throw IOException("HTTP $responseCode for $url")
                }
            }
            connection.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            if (tmp.length() == 0L) throw IOException("Empty response for $url")
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun fileNameFor(verseKey: String): String {
        val parts = verseKey.split(":")
        val surah = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val ayah = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return "%03d%03d.mp3".format(surah, ayah)
    }

    private fun keyFor(reciterId: Int, chapterId: Int): String = "$reciterId:$chapterId"

    private fun downloadedKey(reciterId: Int): String = "$KEY_DOWNLOADED_PREFIX$reciterId"

    private companion object {
        const val KEY_DOWNLOADED = "downloaded_chapters"
        const val KEY_DOWNLOADED_PREFIX = "downloaded_chapters_"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 15_000
        const val BUFFER_SIZE = 8192
    }
}
