package com.nur.quran.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface QuranApi {

    @GET("chapters")
    suspend fun getChapters(
        @Query("language") language: String = "en"
    ): ChaptersResponse

    @GET("chapters/{id}")
    suspend fun getChapter(
        @Path("id") chapterId: Int,
        @Query("language") language: String = "en"
    ): ChapterResponse

    @GET("verses/by_chapter/{chapterId}")
    suspend fun getVersesByChapter(
        @Path("chapterId") chapterId: Int,
        @Query("language") language: String = "en",
        @Query("words") words: Boolean = true,
        @Query("translations") translations: String? = null,
        @Query("audio") audio: Int? = null,
        @Query("fields") fields: String? = null,
        @Query("word_fields") wordFields: String? = null,
        @Query("mushaf") mushaf: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): VersesResponse

    @GET("verses/by_page/{pageNumber}")
    suspend fun getVersesByPage(
        @Path("pageNumber") pageNumber: Int,
        @Query("language") language: String = "en",
        @Query("words") words: Boolean = true,
        @Query("translations") translations: String? = null,
        @Query("fields") fields: String? = null,
        @Query("word_fields") wordFields: String? = null,
        @Query("mushaf") mushaf: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): VersesResponse

    @GET("quran/verses/uthmani_tajweed")
    suspend fun getUthmaniTajweed(
        @Query("chapter_number") chapterNumber: Int? = null,
        @Query("page_number") pageNumber: Int? = null
    ): TajweedResponse

    @GET("tafsirs/{tafsirId}/by_chapter/{chapterId}")
    suspend fun getTafsirByChapter(
        @Path("tafsirId") tafsirId: Int,
        @Path("chapterId") chapterId: Int
    ): TafsirResponse

    @GET("tafsirs/{tafsirId}/by_page/{pageNumber}")
    suspend fun getTafsirByPage(
        @Path("tafsirId") tafsirId: Int,
        @Path("pageNumber") pageNumber: Int
    ): TafsirResponse

    @GET("foot_notes/{footnoteId}")
    suspend fun getFootnote(
        @Path("footnoteId") footnoteId: String
    ): FootnoteResponse
}

// API DTO Definitions
data class ChaptersResponse(val chapters: List<ApiChapter>)
data class ChapterResponse(val chapter: ApiChapter)
data class ApiChapter(
    val id: Int,
    val name_simple: String,
    val name_arabic: String,
    val name_complex: String,
    val translated_name: ApiTranslatedName?,
    val revelation_place: String,
    val revelation_order: Int,
    val verses_count: Int,
    val pages: List<Int>
)

data class ApiTranslatedName(val language_name: String?, val name: String?)

data class VersesResponse(
    val verses: List<ApiVerse>,
    val pagination: ApiPagination
)

data class ApiVerse(
    val id: Int,
    val verse_number: Int,
    val verse_key: String,
    val page_number: Int,
    val juz_number: Int,
    val text_uthmani: String? = null,
    val text_indopak: String? = null,
    val text_qpc_hafs: String? = null,
    val words: List<ApiWord>?,
    val translations: List<ApiTranslation>?,
    val audio: ApiAudio?
)

data class ApiAudio(val url: String?)

data class ApiWord(
    val id: Int,
    val position: Int,
    val text_uthmani: String?,
    val text_indopak: String?,
    val text_qpc_hafs: String?,
    val text_uthmani_tajweed: String?,
    val char_type_name: String,
    val translation: ApiWordTranslation?,
    val transliteration: ApiWordTransliteration?,
    val line_number: Int? = null,
    val page_number: Int? = null
)

data class ApiWordTranslation(val text: String?)
data class ApiWordTransliteration(val text: String?)
data class ApiTranslation(val resource_id: Int, val text: String)
data class ApiPagination(val current_page: Int, val next_page: Int?, val total_pages: Int)

data class TajweedResponse(val verses: List<ApiTajweedVerse>)
data class ApiTajweedVerse(val id: Int, val verse_key: String, val text_uthmani_tajweed: String)

data class TafsirResponse(val tafsirs: List<ApiTafsirVerse>)
data class ApiTafsirVerse(val verse_key: String, val text: String, val resource_id: Int)

data class FootnoteResponse(val foot_note: ApiFootnote)
data class ApiFootnote(val id: Int, val text: String)
