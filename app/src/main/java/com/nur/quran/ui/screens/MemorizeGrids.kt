package com.nur.quran.ui.screens

/**
 * Memorize-grid helpers mirroring web MemorizeIndex.jsx.
 *
 * - Per-surah counts split the "surah:ayah" key (never prefix-match, so
 *   surah 1 never absorbs surah 11's ayahs).
 * - Ayah lists collapse to web ranges like "1-5, 8".
 */
fun surahMemCount(memorizedAyahs: Set<String>, chapterId: Int): Int {
    var count = 0
    for (key in memorizedAyahs) {
        val sep = key.indexOf(':')
        if (sep < 0) continue
        if (key.substring(0, sep).toIntOrNull() == chapterId &&
            key.substring(sep + 1).toIntOrNull() != null
        ) {
            count++
        }
    }
    return count
}

/** Collapse ayah numbers to web format, e.g. [1,2,3,5,7,8] -> "1-3, 5, 7-8". */
fun collapseAyahRanges(ayahs: Collection<Int>): String {
    val nums = ayahs.distinct().sorted()
    if (nums.isEmpty()) return ""
    val ranges = mutableListOf<String>()
    var start = nums[0]
    var prev = nums[0]
    for (i in 1 until nums.size) {
        if (nums[i] == prev + 1) {
            prev = nums[i]
        } else {
            ranges.add(if (start == prev) "$start" else "$start-$prev")
            start = nums[i]
            prev = nums[i]
        }
    }
    ranges.add(if (start == prev) "$start" else "$start-$prev")
    return ranges.joinToString(", ")
}
