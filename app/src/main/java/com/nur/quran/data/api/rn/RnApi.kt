package com.nur.quran.data.api.rn

import com.nur.quran.data.api.ApiPagination
import com.nur.quran.data.api.ApiTajweedVerse
import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.api.ChaptersResponse
import com.nur.quran.data.api.TajweedResponse
import com.nur.quran.data.api.VersesResponse
import com.google.gson.JsonObject
import com.google.gson.Gson
import com.nur.quran.data.api.TafsirResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
        /** Explicit 0 opts out: the backend defaults to words=1. */
        @Query("words") words: Int? = 1,
        /** Optional per contract; omitted from the query when null. */
        @Query("tajweed") tajweed: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 300,
        @Query("mushaf") mushaf: Int = 5,
    ): VersesResponse

    /** Server-side copy of the bundled `assets/data/words/{ch}.json` pack. */
    @GET("v1/packs/words/{chapterId}.json")
    suspend fun getWordsPack(@Path("chapterId") chapterId: Int): JsonObject

    @GET("v1/tafsir/{tafsirId}/by_chapter/{chapterId}")
    suspend fun getTafsirByChapter(@Path("tafsirId") tafsirId: Int, @Path("chapterId") chapterId: Int): TafsirResponse

    @GET("v1/pages/{page}")
    suspend fun getPage(@Path("page") page: Int, @Query("translation") translation: Int = 20, @Query("mushaf") mushaf: Int = 5): VersesResponse

    @GET("v1/footnotes/{id}")
    suspend fun getFootnote(@Path("id") id: String): com.nur.quran.data.api.FootnoteResponse

    @GET("v1/reciters")
    suspend fun getReciters(): RnRecitersResponse

    /** Completion is persisted by the backend, which verifies the assignment owner. */
    @POST("v1/sauka/assignments/{id}/complete")
    suspend fun completeSaukaAssignment(@Path("id") assignmentId: String): JsonObject

    // ── sync (Bearer; document fields 1:1 from SyncContract.kt) ──────────────

    /** The user's one sync document (lenient envelope, see [RnSyncEnvelope]). */
    @GET("v1/sync")
    suspend fun getSync(): Map<String, Any?>

    /** Upsert with `userId` + `stateData` → `{ lastSyncAt }`. */
    @PUT("v1/sync")
    suspend fun putSync(@Body body: RnSyncPushPayload): RnSyncPutResponse
}

/**
 * Content DTO normalization: the pack contract has no `pagination` object and
 * the existing DTO declares both `verses` and `pagination` non-null (Gson
 * happily leaves them runtime-null), so every content read funnels through
 * [normalizeVersesResponse] before use.
 */
data class RnRecitersResponse(val reciters: List<RnReciter> = emptyList())
data class RnReciter(val id: String?, val name: String?, val style: String?, val audio_path: String?)

object RnContent {
    /** Packs preserve bundle strings; the regular verses API uses {text} objects. */
    fun parseWordsPack(raw: JsonObject): VersesResponse {
        val normalized = raw.deepCopy()
        normalized.get("verses")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { verse ->
            verse.asJsonObject.get("words")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { word ->
                val row = word.asJsonObject
                for (key in listOf("translation", "transliteration")) {
                    val value = row.get(key)
                    if (value != null && value.isJsonPrimitive) {
                        row.add(key, JsonObject().apply { add("text", value) })
                    }
                }
            }
        }
        return normalizeVersesResponse(Gson().fromJson(normalized, VersesResponse::class.java))
    }

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
