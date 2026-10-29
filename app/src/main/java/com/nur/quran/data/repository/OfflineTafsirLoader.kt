package com.nur.quran.data.repository

import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.api.TafsirResponse

/** Downloaded edition wins per ayah; bundled Ibn Kathir completes any missing local ayahs. */
internal suspend fun loadTafsirOfflineFirst(
    chapterId: Int,
    cached: List<ApiTafsirVerse>,
    bundled: List<ApiTafsirVerse>,
    network: suspend () -> TafsirResponse,
    persist: suspend (TafsirResponse) -> Unit,
): TafsirResponse {
    fun valid(rows: List<ApiTafsirVerse>) = rows.filter {
        it.verse_key.substringBefore(':').toIntOrNull() == chapterId && !it.text.isNullOrBlank()
    }
    val local = (valid(bundled).associateBy { it.verse_key } + valid(cached).associateBy { it.verse_key })
        .values.sortedBy { it.verse_key.substringAfter(':').toIntOrNull() ?: 0 }
    if (local.isNotEmpty()) return TafsirResponse(local)
    return network().also { persist(it) }
}

/** The shipped Ibn Kathir dataset stores grouped commentary on the group's first ayah only. */
internal fun completeBundledTafsir(rows: List<ApiTafsirVerse>): List<ApiTafsirVerse> =
    rows.groupBy { it.resource_id to it.verse_key.substringBefore(':') }.values.flatMap { chapter ->
        var explanation: String? = null
        chapter.groupBy { it.verse_key }.values.map { duplicates ->
            duplicates.firstOrNull { it.text.isNotBlank() } ?: duplicates.first()
        }.sortedBy { it.verse_key.substringAfter(':').toInt() }.map { row ->
            if (row.text.isNotBlank()) explanation = row.text
            if (row.text.isBlank() && explanation != null) row.copy(text = explanation!!) else row
        }
    }
