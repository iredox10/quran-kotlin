package com.nur.quran.data.audio

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SAF scanner for GreenTech/quran_android audio linked in place (no copy).
 *
 * Known user-verified layout:
 * `Music/quran_android/muaiqly_non_haramain_gapless/001.mp3..114.mp3`
 * (+ `timings.db` copied in by the user). Other folders: `idris-abkar`,
 * `husary`, `muaiqly_kfgqpc`.
 *
 * Per-SURAH filename (NOT per-ayah): `^(\d{3})\.mp3$` (001..114).
 */
@Singleton
class GreenTechScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class ScanResult(
        val reciterId: Int,
        val folderName: String,
        val folderUri: String,
        val surahsFound: Set<Int>,
        val hasTimingDb: Boolean,
        val timingDbName: String?
    )

    companion object {
        private val SURAH_FILE_REGEX = Regex("""^(\d{3})\.mp3$""", RegexOption.IGNORE_CASE)
        const val UNKNOWN_RECITER_ID = -1
    }

    /**
     * Scans [treeUri] (from `ACTION_OPEN_DOCUMENT_TREE`) for surah-holding folders.
     *
     * - The tree root itself is included when it directly holds `001.mp3`-style files.
     * - Descendant directories are scanned breadth-first down to [maxDepth] levels
     *   below the root (root = depth 0, its children = depth 1, ...).
     * - Only folders with at least one `001..114.mp3` file are returned.
     * - A `*.db` file in the same folder is reported via [ScanResult.hasTimingDb].
     * - Folders unknown to [Reciters.findByFolder] are still returned with
     *   `reciterId = -1`.
     */
    suspend fun scanTree(treeUri: Uri, maxDepth: Int = 2): List<ScanResult> =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            if (!root.isDirectory) return@withContext emptyList()

            val results = mutableListOf<ScanResult>()

            // Tree root itself (user may have picked the reciter folder directly).
            scanSingleFolder(root)?.let { results.add(it) }

            // Breadth-first over subdirectories down to maxDepth.
            val queue = ArrayDeque<Pair<DocumentFile, Int>>()
            root.listFiles()
                .filter { it.isDirectory }
                .forEach { queue.add(it to 1) }
            while (queue.isNotEmpty()) {
                val (dir, depth) = queue.removeFirst()
                scanSingleFolder(dir)?.let { results.add(it) }
                if (depth < maxDepth) {
                    dir.listFiles()
                        .filter { it.isDirectory }
                        .forEach { queue.add(it to depth + 1) }
                }
            }

            results
        }

    /**
     * Scans [treeUri] and persists the mapping into [store]:
     * [LinkedAudioStore.setFolderReciter] per linked folder plus
     * [LinkedAudioStore.markLinked] per surah found.
     *
     * Folders unknown to [Reciters.findByFolder] (`reciterId = -1`) are still
     * returned but are NOT persisted. Returns the full [scanTree] results.
     */
    suspend fun linkTree(treeUri: Uri, store: LinkedAudioStore): List<ScanResult> =
        withContext(Dispatchers.IO) {
            val results = scanTree(treeUri)
            results.forEach { result ->
                if (result.reciterId == UNKNOWN_RECITER_ID) return@forEach
                store.setFolderReciter(result.folderUri, result.reciterId)
                result.surahsFound.forEach { surah ->
                    store.markLinked(result.reciterId, surah)
                }
            }
            results
        }

    /** Returns a [ScanResult] when [dir] holds surah mp3s, else null. */
    private fun scanSingleFolder(dir: DocumentFile): ScanResult? {
        val files = try {
            dir.listFiles()
        } catch (_: Exception) {
            return null
        }
        val surahs = files
            .filter { it.isFile }
            .mapNotNull { it.name }
            .mapNotNull { fileName ->
                val match = SURAH_FILE_REGEX.matchEntire(fileName.trim()) ?: return@mapNotNull null
                val surah = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                if (surah in 1..114) surah else null
            }
            .toSet()
        if (surahs.isEmpty()) return null

        val timingDbName = files
            .filter { it.isFile }
            .mapNotNull { it.name }
            .firstOrNull { it.trim().endsWith(".db", ignoreCase = true) }

        val folderName = dir.name ?: return null
        val reciterId = Reciters.findByFolder(folderName)?.id ?: UNKNOWN_RECITER_ID
        return ScanResult(
            reciterId = reciterId,
            folderName = folderName,
            folderUri = dir.uri.toString(),
            surahsFound = surahs,
            hasTimingDb = timingDbName != null,
            timingDbName = timingDbName
        )
    }
}
