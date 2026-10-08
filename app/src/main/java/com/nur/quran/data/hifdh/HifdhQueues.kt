package com.nur.quran.data.hifdh

/**
 * Pure sabaq/sabqi/manzil queue rules, mirroring web
 * quran-app/src/pages/MemorizeIndex.jsx:
 * no-card | New | Learning | reps<3 => sabaq; same-surah-as-last-session
 * => sabqi; rest => manzil. Due = no card or due <= now.
 */
enum class HifdhQueue { SABAQ, SABQI, MANZIL }

data class HifdhQueueData(
    val sabaqAll: List<String> = emptyList(),
    val sabaqDue: List<String> = emptyList(),
    val sabqiAll: List<String> = emptyList(),
    val sabqiDue: List<String> = emptyList(),
    val manzilAll: List<String> = emptyList(),
    val manzilDue: List<String> = emptyList()
)

fun classifyHifdhQueue(
    verseKey: String,
    card: FsrsCard?,
    lastMemChapterId: Int?
): HifdhQueue {
    val isNewish = card == null ||
        card.state == FsrsState.NEW ||
        card.state == FsrsState.LEARNING ||
        card.reps < 3
    return when {
        isNewish -> HifdhQueue.SABAQ
        lastMemChapterId != null && verseKey.startsWith("$lastMemChapterId:") -> HifdhQueue.SABQI
        else -> HifdhQueue.MANZIL
    }
}

/** Web isDue: no history/card => due, else card.due <= now (boundary inclusive). */
fun isHifdhDue(card: FsrsCard?, nowMs: Long): Boolean = card == null || card.due <= nowMs

fun buildHifdhQueueData(
    memorizedAyahs: Set<String>,
    hifdhHistory: Map<String, HifdhHistoryEntry>,
    lastMemChapterId: Int?,
    nowMs: Long
): HifdhQueueData {
    val sortedKeys = memorizedAyahs.sortedBy { key ->
        val parts = key.split(":")
        (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 10000 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    val sabaqAll = mutableListOf<String>()
    val sabqiAll = mutableListOf<String>()
    val manzilAll = mutableListOf<String>()
    for (key in sortedKeys) {
        when (classifyHifdhQueue(key, hifdhHistory[key]?.card, lastMemChapterId)) {
            HifdhQueue.SABAQ -> sabaqAll.add(key)
            HifdhQueue.SABQI -> sabqiAll.add(key)
            HifdhQueue.MANZIL -> manzilAll.add(key)
        }
    }
    fun dueKeys(all: List<String>): List<String> =
        all.filter { key -> isHifdhDue(hifdhHistory[key]?.card, nowMs) }
    return HifdhQueueData(
        sabaqAll = sabaqAll, sabaqDue = dueKeys(sabaqAll),
        sabqiAll = sabqiAll, sabqiDue = dueKeys(sabqiAll),
        manzilAll = manzilAll, manzilDue = dueKeys(manzilAll)
    )
}

/** Web Memorization.jsx: (currentVerseIndex + currentVerses.length) / total. */
fun hifdhSessionProgress(currentVerseIndex: Int, shownCount: Int, total: Int): Float {
    if (total <= 0) return 0f
    return ((currentVerseIndex + shownCount).toFloat() / total).coerceIn(0f, 1f)
}
