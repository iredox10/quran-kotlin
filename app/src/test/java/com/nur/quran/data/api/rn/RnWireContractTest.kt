package com.nur.quran.data.api.rn

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Exercise real Retrofit URLs and Gson conversion without a server or new test dependencies. */
class RnWireContractTest {
    @Test
    fun `content queries match server contract and decode bundle strings`() = runBlocking {
        val requests = mutableListOf<okhttp3.Request>()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            val body = when (request.url.encodedPath) {
                "/v1/verses/2" -> """{"verses":[],"pagination":{"current_page":2,"next_page":null,"total_pages":2}}"""
                "/v1/packs/words/1.json" -> java.io.File("src/main/assets/data/words/1.json").readText()
                "/v1/tafsir/169/by_chapter/1" -> """{"tafsirs":[{"verse_key":"1:1","text":"Explanation","resource_id":169}]}"""
                "/v1/reciters" -> """{"reciters":[{"id":7,"name":"Reciter","style":"Murattal","audio_path":"Alafasy/mp3"}]}"""
                "/v1/footnotes/42" -> """{"foot_note":{"id":42,"text":"Explanation"}}"""
                "/v1/sauka/assignments/assignment-1/complete" -> "{}"
                "/v1/pages/10" -> """{"verses":[],"pagination":{"current_page":1,"next_page":null,"total_pages":1}}"""
                else -> throw AssertionError(request.url)
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        try {
            val api = Retrofit.Builder().baseUrl("https://rn.test/").client(http)
                .addConverterFactory(GsonConverterFactory.create()).build().create(RnApi::class.java)
            api.getVerses(2, translation = 131, words = 0, tajweed = 1, page = 2, perPage = 300, mushaf = 19)
            val url = requests.last().url
            assertEquals("131", url.queryParameter("translation"))
            assertEquals("0", url.queryParameter("words"))
            assertEquals("1", url.queryParameter("tajweed"))
            assertEquals("2", url.queryParameter("page"))
            assertEquals("300", url.queryParameter("per_page"))
            assertEquals("19", url.queryParameter("mushaf"))
            assertEquals(7, RnContent.parseWordsPack(api.getWordsPack(1)).verses.size)
            assertEquals("Explanation", api.getTafsirByChapter(169, 1).tafsirs.single().text)
            assertEquals("7", api.getReciters().reciters.single().id)
            assertEquals("Explanation", api.getFootnote("42").foot_note.text)
            api.completeSaukaAssignment("assignment-1")
            assertEquals("POST", requests.last().method)
            assertEquals("/v1/sauka/assignments/assignment-1/complete", requests.last().url.encodedPath)
            api.getPage(10, 131, 19)
            assertEquals("131", requests.last().url.queryParameter("translation"))
            assertEquals("19", requests.last().url.queryParameter("mushaf"))
        } finally {
            http.dispatcher.executorService.shutdown()
            http.connectionPool.evictAll()
        }
    }
}
