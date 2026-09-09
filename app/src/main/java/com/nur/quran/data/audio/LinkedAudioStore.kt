package com.nur.quran.data.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for GreenTech/quran_android audio linked in place via SAF (no copy).
 *
 * Expected on-disk layout behind each linked tree:
 * `Music/quran_android/<reciter>/001.mp3..114.mp3` + `timings.db`.
 *
 * This store only persists bookkeeping — it never touches SAF/DocumentFile.
 * The caller that receives `ACTION_OPEN_DOCUMENT_TREE` takes the persistable
 * permission; a separate scanner resolves tree Uris into chapters. Stored here:
 * - linked tree Uris ([KEY_TREES])
 * - folderUri → reciterId map ([KEY_FOLDER_MAP], `"folderUri|reciterId"` lines)
 * - linked chapters per reciter (`"linked_chapters_<reciterId>"` string sets)
 */
@Singleton
class LinkedAudioStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _linkedState = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    /** reciterId → linked surah numbers. Refreshed on every mutation. */
    val linkedState: StateFlow<Map<Int, Set<Int>>> = _linkedState.asStateFlow()

    init {
        refreshState()
    }

    // ── Linked trees ────────────────────────────────────────────────────

    /** Adds a tree Uri string (from `ACTION_OPEN_DOCUMENT_TREE`). */
    @Synchronized
    fun addTree(uriString: String) {
        val current = prefs.getStringSet(KEY_TREES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.add(uriString)) {
            prefs.edit().putStringSet(KEY_TREES, current).apply()
            refreshState()
        }
    }

    /** Removes a tree Uri string and drops its folder→reciter mapping, if any. */
    @Synchronized
    fun removeTree(uriString: String) {
        val current = prefs.getStringSet(KEY_TREES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val removed = current.remove(uriString)
        val map = readFolderMap().toMutableMap()
        val mapRemoved = map.remove(uriString) != null
        if (removed || mapRemoved) {
            prefs.edit()
                .putStringSet(KEY_TREES, current)
                .putString(KEY_FOLDER_MAP, writeFolderMap(map))
                .apply()
            refreshState()
        }
    }

    /** All linked tree Uri strings. */
    fun getTrees(): Set<String> =
        prefs.getStringSet(KEY_TREES, emptySet())?.toSet() ?: emptySet()

    // ── Folder → reciter map ────────────────────────────────────────────

    /** Associates a linked folder (tree Uri string) with a reciter id. */
    @Synchronized
    fun setFolderReciter(folderUri: String, reciterId: Int) {
        val map = readFolderMap().toMutableMap()
        map[folderUri] = reciterId
        prefs.edit().putString(KEY_FOLDER_MAP, writeFolderMap(map)).apply()
        refreshState()
    }

    /** Reciter id previously associated with [folderUri], or null. */
    fun getFolderReciter(folderUri: String): Int? = readFolderMap()[folderUri]

    /** All folder Uris mapped to [reciterId] (qari subfolders from linkTree). */
    fun getFolderUrisForReciter(reciterId: Int): List<String> =
        readFolderMap().filterValues { it == reciterId }.keys.toList()

    // ── Linked chapters ─────────────────────────────────────────────────

    /** Marks [surah] (1..114) as linked for [reciterId]. */
    @Synchronized
    fun markLinked(reciterId: Int, surah: Int) {
        val key = linkedChaptersKey(reciterId)
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.add(surah.toString())) {
            prefs.edit().putStringSet(key, current).apply()
            refreshState()
        }
    }

    /** Clears the linked mark for [surah] under [reciterId]. */
    @Synchronized
    fun unmarkLinked(reciterId: Int, surah: Int) {
        val key = linkedChaptersKey(reciterId)
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: return
        if (current.remove(surah.toString())) {
            prefs.edit().putStringSet(key, current).apply()
            refreshState()
        }
    }

    /** True when [surah] is marked linked for [reciterId]. */
    fun isLinked(reciterId: Int, surah: Int): Boolean =
        prefs.getStringSet(linkedChaptersKey(reciterId), emptySet())?.contains(surah.toString()) == true

    /** Linked surah numbers for [reciterId], or empty set. */
    fun getLinkedSurahs(reciterId: Int): Set<Int> =
        prefs.getStringSet(linkedChaptersKey(reciterId), emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    /** Clears all linked chapters for [reciterId]. */
    @Synchronized
    fun clearReciter(reciterId: Int) {
        prefs.edit().remove(linkedChaptersKey(reciterId)).apply()
        refreshState()
    }

    // ── Internal ────────────────────────────────────────────────────────

    /** Rebuilds [linkedState] from prefs. Called synchronously after every mutation. */
    private fun refreshState() {
        val out = mutableMapOf<Int, Set<Int>>()
        prefs.all.keys
            .filter { it.startsWith(KEY_LINKED_PREFIX) }
            .forEach { key ->
                val reciterId = key.removePrefix(KEY_LINKED_PREFIX).toIntOrNull() ?: return@forEach
                val surahs = prefs.getStringSet(key, emptySet())
                    ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
                if (surahs.isNotEmpty()) out[reciterId] = surahs
            }
        _linkedState.value = out
    }

    private fun readFolderMap(): Map<String, Int> {
        val raw = prefs.getString(KEY_FOLDER_MAP, null).orEmpty()
        if (raw.isBlank()) return emptyMap()
        val out = linkedMapOf<String, Int>()
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val sep = line.lastIndexOf('|')
                if (sep <= 0) return@forEach
                val uri = line.substring(0, sep)
                val id = line.substring(sep + 1).toIntOrNull() ?: return@forEach
                out[uri] = id
            }
        return out
    }

    private fun writeFolderMap(map: Map<String, Int>): String =
        map.entries.joinToString("\n") { (uri, id) -> "$uri|$id" }

    private fun linkedChaptersKey(reciterId: Int): String = "$KEY_LINKED_PREFIX$reciterId"

    private companion object {
        const val PREFS_NAME = "linked_audio"
        const val KEY_TREES = "linked_trees"
        const val KEY_FOLDER_MAP = "folder_map"
        const val KEY_LINKED_PREFIX = "linked_chapters_"
    }
}
