package com.nur.quran.data

/**
 * In-memory sajdah + division markers.
 *
 * Schema decision: NO Room schema change (see QuranDatabase version 8).
 * VerseEntity carries only juzNumber + pageNumber, and a migration (or
 * destructive fallback) for millions of cached rows was deemed riskier than
 * deriving markers in memory. The Quran.com API v4 already exposes
 * hizb_number / rub_el_hizb_number / ruku_number / manzil_number /
 * sajdah_number / sajdah_type (requested via [com.nur.quran.data.mushaf.Mushaf.verseFields]
 * and parsed into ApiVerse), so a future migration can persist them; until
 * then the UI derives everything below offline-first:
 * - sajdah: static 15-verse map (Hafs order, stable across mushafs).
 * - juz/hizb starts: [JUZ_STARTS] / [HIZB_STARTS] verseKey lookup.
 */
val SAJDAH_VERSES: Map<String, Int> = mapOf(
    "7:206" to 1,
    "13:15" to 2,
    "16:50" to 3,
    "17:109" to 4,
    "19:58" to 5,
    "22:18" to 6,
    "22:77" to 7,
    "25:60" to 8,
    "27:26" to 9,
    "32:15" to 10,
    "38:24" to 11,
    "41:38" to 12,
    "53:62" to 13,
    "84:21" to 14,
    "96:19" to 15
)

fun isSajdahVerse(verseKey: String): Boolean = SAJDAH_VERSES.containsKey(verseKey)

fun sajdahNumberFor(verseKey: String): Int? = SAJDAH_VERSES[verseKey]

private fun parseVerseKey(verseKey: String): Pair<Int, Int>? {
    val parts = verseKey.split(":")
    if (parts.size != 2) return null
    val surah = parts[0].toIntOrNull() ?: return null
    val ayah = parts[1].toIntOrNull() ?: return null
    return surah to ayah
}

/** Numeric verseKey ordering: "2:142" < "2:253" < "3:93". */
fun compareVerseKeys(a: String, b: String): Int {
    val pa = parseVerseKey(a)
    val pb = parseVerseKey(b)
    if (pa == null && pb == null) return a.compareTo(b)
    if (pa == null) return -1
    if (pb == null) return 1
    val surahCmp = pa.first.compareTo(pb.first)
    if (surahCmp != 0) return surahCmp
    return pa.second.compareTo(pb.second)
}

private val juzStartByVerseKey: Map<String, DivisionStart> by lazy {
    JUZ_STARTS.associateBy { it.verseKey }
}

private val hizbStartByVerseKey: Map<String, DivisionStart> by lazy {
    HIZB_STARTS.associateBy { it.verseKey }
}

/** Non-null when [verseKey] opens a new Juz (exact start-verse match). */
fun juzStartForVerseKey(verseKey: String): DivisionStart? = juzStartByVerseKey[verseKey]

/** Non-null when [verseKey] opens a new Hizb (exact start-verse match). */
fun hizbStartForVerseKey(verseKey: String): DivisionStart? = hizbStartByVerseKey[verseKey]

/** Juz containing [verseKey] (last Juz start at or before the verse). */
fun juzForVerseKey(verseKey: String): DivisionStart =
    JUZ_STARTS.lastOrNull { compareVerseKeys(it.verseKey, verseKey) <= 0 } ?: JUZ_STARTS.first()

/** Hizb containing [verseKey] (last Hizb start at or before the verse). */
fun hizbForVerseKey(verseKey: String): DivisionStart =
    HIZB_STARTS.lastOrNull { compareVerseKeys(it.verseKey, verseKey) <= 0 } ?: HIZB_STARTS.first()
