package com.nur.quran.data.hifdh

/**
 * Pure web-parity helper for `toggleMemorizedSurah(chapterId, totalVerses)`
 * (quran-app/src/store/useAppStore.js): toggling a surah adds/removes its id
 * AND its full `"surah:ayah"` range. With `totalVerses == null` only the
 * surah id list changes, exactly like the web guard `if (totalVerses)`.
 */
data class MemorizedSurahToggleResult(
    val surahs: Set<Int>,
    val ayahs: Set<String>
)

fun toggleMemorizedSurah(
    currentSurahs: Set<Int>,
    currentAyahs: Set<String>,
    chapterId: Int,
    totalVerses: Int? = null
): MemorizedSurahToggleResult {
    val surahs = currentSurahs.toMutableSet()
    val ayahs = currentAyahs.toMutableSet()
    if (surahs.contains(chapterId)) {
        surahs.remove(chapterId)
        if (totalVerses != null) {
            for (i in 1..totalVerses) ayahs.remove("$chapterId:$i")
        }
    } else {
        surahs.add(chapterId)
        if (totalVerses != null) {
            for (i in 1..totalVerses) ayahs.add("$chapterId:$i")
        }
    }
    return MemorizedSurahToggleResult(surahs = surahs, ayahs = ayahs)
}
