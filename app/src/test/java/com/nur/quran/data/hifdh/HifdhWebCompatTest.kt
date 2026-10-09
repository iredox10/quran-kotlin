package com.nur.quran.data.hifdh

import com.google.gson.Gson
import com.google.gson.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Web payload compatibility (quran-app sync): the web app persists ts-fsrs
 * `Card` objects verbatim (SNAKE_CASE keys, ISO-8601 dates) next to
 * `{lastReviewed, strength}` and goals with a numeric `id` and a
 * `"YYYY-MM-DD"` targetDate. Both shapes must load and re-serialize web-shaped.
 */
class HifdhWebCompatTest {

    private val gson = Gson()

    private val webCardJson = """
        {"due":"2026-05-01T09:30:00.000Z","stability":8.2956,"difficulty":1.0,
         "elapsed_days":3,"scheduled_days":12,"learning_steps":0,"reps":4,
         "lapses":1,"state":2,"last_review":"2026-04-05T00:00:00.000Z"}
    """.trimIndent()

    private val kotlinCardJson = """
        {"due":1777627800000,"stability":8.2956,"difficulty":1.0,"elapsedDays":3,
         "scheduledDays":12,"learningSteps":0,"reps":4,"lapses":1,"state":2,
         "lastReview":1775347200000}
    """.trimIndent()

    private fun parse(raw: String): JsonElement = gson.fromJson(raw, JsonElement::class.java)

    @Test
    fun `web snake_case card json parses with iso dates`() {
        val card = parseWebCard(parse(webCardJson))
        assertEquals(1777627800000L, card.due)
        assertEquals(1775347200000L, card.lastReview)
        assertEquals(8.2956, card.stability, 0.0)
        assertEquals(1.0, card.difficulty, 0.0)
        assertEquals(3.0, card.elapsedDays, 0.0)
        assertEquals(12, card.scheduledDays)
        assertEquals(0, card.learningSteps)
        assertEquals(4, card.reps)
        assertEquals(1, card.lapses)
        assertEquals(FsrsState.REVIEW, card.state)
    }

    @Test
    fun `kotlin camel_case card json still parses`() {
        assertEquals(parseWebCard(parse(webCardJson)), parseWebCard(parse(kotlinCardJson)))
    }

    @Test
    fun `card without last_review parses with null`() {
        val raw = """{"due":"2026-05-01T09:30:00.000Z","stability":2.3065,"difficulty":5.1,
                     "elapsed_days":0,"scheduled_days":1,"learning_steps":1,"reps":1,
                     "lapses":0,"state":1}"""
        assertNull(parseWebCard(parse(raw)).lastReview)
    }

    @Test
    fun `card round trips to web shaped json`() {
        val card = parseWebCard(parse(webCardJson))
        val json = card.toWebJson()

        assertEquals("2026-05-01T09:30:00Z", json.get("due").asString)
        assertEquals("2026-04-05T00:00:00Z", json.get("last_review").asString)
        assertEquals(3.0, json.get("elapsed_days").asDouble, 0.0)
        assertEquals(12, json.get("scheduled_days").asInt)
        assertEquals(0, json.get("learning_steps").asInt)
        assertNull("kotlin keys must not leak into the web shape", json.get("elapsedDays"))
        assertNull(json.get("lastReview"))
        assertEquals(card, parseWebCard(json))
    }

    @Test
    fun `history entry reads web fields and normalizes legacy good strength`() {
        val raw = """{"card":$webCardJson,"lastReviewed":1777627800123,"strength":"good"}"""
        val entry = parseWebHistoryEntry(parse(raw))

        assertEquals(1777627800123L, entry.lastReviewed)
        assertEquals("medium", entry.strength)
        assertEquals(1777627800000L, entry.card?.due)

        val json = entry.toWebJson()
        assertEquals(1777627800123L, json.get("lastReviewed").asLong)
        assertEquals("medium", json.get("strength").asString)
        assertEquals("2026-05-01T09:30:00Z", json.getAsJsonObject("card").get("due").asString)
    }

    @Test
    fun `legacy entry without card keeps its strength`() {
        val entry = parseWebHistoryEntry(parse("""{"lastReviewed":1777593600000,"strength":"strong"}"""))
        assertNull(entry.card)
        assertEquals("strong", entry.strength)
        assertEquals(1777593600000L, entry.lastReviewed)
    }

    @Test
    fun `one corrupt entry does not kill the history map`() {
        val raw = """
            {"1:1":{"card":$webCardJson,"lastReviewed":1777627800123,"strength":"strong"},
             "2:5":42,
             "3:7":{"lastReviewed":1777593600000,"strength":"good"}}
        """.trimIndent()

        val map = parseWebHistoryMap(parse(raw))

        assertEquals(2, map.size)
        assertEquals("strong", map["1:1"]?.strength)
        assertEquals(1777627800123L, map["1:1"]?.lastReviewed)
        assertEquals("medium", map["3:7"]?.strength)
        assertNull(map["2:5"])
    }
}
