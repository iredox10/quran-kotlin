package com.nur.quran.data.api.rn

import com.nur.quran.data.api.ApiPagination
import com.nur.quran.data.api.ApiTajweedVerse
import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.api.ChaptersResponse
import com.nur.quran.data.api.TajweedResponse
import com.nur.quran.data.api.VersesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * quran-nur-api content + sync surface (Bearer where noted). Paths per
 * docs/contract-content.md:
 *   GET v1/chapters
 *   GET v1/verses/{ch}?translation=20[&words=1]
 *   GET v1/packs/words/{ch}.json            (bundle-shape single file)
 *
 * Content responses are the api.quran.com v4 shapes this app already parses
 * ([VersesResponse] etc.). NOT every field is present server-side (tajweed is
 * optional per the contract) — nullable DTO fields absorb the difference.
 * Sync endpoints (`v1/sync`) live on this same interface (RnModule mounts all
 * of it behind the auth interceptor); their contracts are
 * docs/contract-auth-sync.md.
 */
interface RnApi {
    @GET("v1/chapters")
    suspend fun getChapters(): ChaptersResponse

    @GET("v1/verses/{chapterId}")
    suspend fun getVerses(
        @Path("chapterId") chapterId: Int,
        @Query("translation") translation: Int = RnBackend.TRANSLATION_ID,
        /** Query dropped entirely when null (words add back at the caller's gate). */
        @Query("words") words: Int? = 1,
        /** Optional per contract; omitted from the query when null. */
        @Query("tajweed") tajweed: Int? = null,
    ): VersesResponse

    /** Server-side copy of the bundled `assets/data/words/{ch}.json` pack. */
    @GET("v1/packs/words/{chapterId}.json")
    suspend fun getWordsPack(@Path("chapterId") chapterId: Int): VersesResponse
}

/**
 * Content DTO normalization: the pack contract has no `pagination` object and
 * the existing DTO declares both `verses` and `pagination` non-null (Gson
 * happily leaves them runtime-null), so every content read funnels through
 * [normalizeVersesResponse] before use.
 */
object RnContent {
    fun normalizeVersesResponse(raw: VersesResponse): VersesResponse {
        if (raw.verses != null && raw.pagination != null) return raw
        val pagination = raw.pagination
            ?: ApiPagination(current_page = 1, next_page = null, total_pages = 1)
        return VersesResponse(verses = raw.verses ?: emptyList(), pagination = pagination)
    }

    /**
     * Verse-level `text_uthmani_tajweed` (optional per content contract) → the
     * same [TajweedResponse] the api.quran.com `quran/verses/uthmani_tajweed`
     * call produced, so tajweed HTML consumers and the offline cache key keep
     * working unchanged. Null when the server serves none.
     */
    fun tajweedFromVerses(verses: List<ApiVerse>?): TajweedResponse? {
        val rows = (verses ?: emptyList())
            .filter { !it.text_uthmani_tajweed.isNullOrBlank() }
            .map {
                ApiTajweedVerse(
                    id = it.id,
                    verse_key = it.verse_key,
                    text_uthmani_tajweed = it.text_uthmani_tajweed!!,
                )
            }
        return if (rows.isEmpty()) null else TajweedResponse(verses = rows)
    }
}
