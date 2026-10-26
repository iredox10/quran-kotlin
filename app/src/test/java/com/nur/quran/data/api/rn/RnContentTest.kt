package com.nur.quran.data.api.rn

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Content DTO normalization: the pack contract has no pagination object and
 * the app's DTO declares `verses`/`pagination` non-null — Gson leaves nulls in
 * those fields at runtime, so every read must survive a bare pack payload.
 */
class RnContentTest {

    private val gson = Gson()

    private fun packFromJson(json: String): com.nur.quran.data.api.VersesResponse {
        // A raw Gson parse (not a Kotlin construction) is what the network layer
        // produces — runtime-null fields land exactly as declared-broken.
        return gson.fromJson(
            json,
            com.nur.quran.data.api.VersesResponse::class.java,
        )
    }

    @Test
    fun `bare pack payload normalizes verses and pagination`() {
        val raw = packFromJson("""{"verses":null,"pagination":null}""")

        val out = RnContent.normalizeVersesResponse(raw)

        assertEquals(emptyList<Any?>(), out.verses)
        assertEquals(1, out.pagination.current_page)
        assertNull(out.pagination.next_page)
        assertEquals(1, out.pagination.total_pages)
    }

    @Test
    fun `verses survive a pagination-free pack`() {
        val raw = packFromJson(
            """{"verses":[{"id":1,"verse_number":1,"verse_key":"1:1","page_number":1,""" +
                """"juz_number":1}],"pagination":null}""",
        )

        val out = RnContent.normalizeVersesResponse(raw)

        assertEquals(1, out.verses.size)
        assertEquals("1:1", out.verses[0].verse_key)
        assertEquals(1, out.pagination.current_page)
    }

    @Test
    fun `already-complete responses pass through unchanged`() {
        val raw = packFromJson(
            """{"verses":[],"pagination":{"current_page":2,"next_page":null,"total_pages":3}}""",
        )

        val out = RnContent.normalizeVersesResponse(raw)

        assertEquals(2, out.pagination.current_page)
        assertEquals(3, out.pagination.total_pages)
    }

    @Test
    fun `verse-level tajweed text becomes a TajweedResponse`() {
        val verses = listOf(
            com.nur.quran.data.api.ApiVerse(
                id = 1,
                verse_number = 1,
                verse_key = "1:1",
                page_number = 1,
                juz_number = 1,
                text_uthmani_tajweed = "<tajweed/>",
                words = null,
                translations = null,
                audio = null,
            ),
        )

        val tajweed = RnContent.tajweedFromVerses(verses)!!

        assertEquals(1, tajweed.verses.size)
        assertEquals("1:1", tajweed.verses[0].verse_key)
        assertEquals("<tajweed/>", tajweed.verses[0].text_uthmani_tajweed)
    }

    @Test
    fun `no verse-level tajweed text means null (legacy try-catch flow)`() {
        val verses = listOf(
            com.nur.quran.data.api.ApiVerse(
                id = 1,
                verse_number = 1,
                verse_key = "1:1",
                page_number = 1,
                juz_number = 1,
                words = null,
                translations = null,
                audio = null,
            ),
        )

        assertNull(RnContent.tajweedFromVerses(verses))
        assertNull(RnContent.tajweedFromVerses(null))
    }
}
