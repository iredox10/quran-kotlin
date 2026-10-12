package com.nur.quran.data.planner

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Web wire shapes (quran-app/src/store/useAppStore.js:97-102):
 * plannerReflections `{ [plannerId]: { [day]: { text, createdAt } } }`,
 * plannerSessionTimers `{ [plannerId]: { [day]: { startedAt, totalSeconds } } }`,
 * plannerBookmarks `{ [plannerId]: [{ verseKey, surahName, note, createdAt }] }`.
 * Timestamps are ISO-8601 strings on the web side, epoch ms on Android, so the
 * legacy Kotlin shapes (plain-string reflections, plain-int session totals,
 * epoch-ms bookmark createdAt) must also read back.
 */
class PlannerWireTest {

    private val iso = "2026-03-04T05:06:07.000Z"

    private fun jsonEquals(expected: String, actual: String) {
        assertEquals(
            JsonParser.parseString(expected).asJsonObject,
            JsonParser.parseString(actual).asJsonObject
        )
    }

    // ── ISO ↔ epoch ms ────────────────────────────────────────────────────
    @Test
    fun `iso round trip keeps instant`() {
        assertEquals(iso, PlannerWire.isoFromEpochMs(1_772_600_767_000L))
        assertEquals(1_772_600_767_000L, PlannerWire.epochMsFromIso(iso))
    }

    @Test
    fun `epoch ms from iso accepts second precision and legacy numbers`() {
        assertEquals(1_772_600_767_000L, PlannerWire.epochMsFromIso("2026-03-04T05:06:07Z"))
        assertEquals(1_772_600_767_000L, PlannerWire.epochMsFromIso("1772600767000"))
        assertNull(PlannerWire.epochMsFromIso(null))
        assertNull(PlannerWire.epochMsFromIso("not-a-date"))
    }

    // ── plannerReflections ────────────────────────────────────────────────
    @Test
    fun `reflections round trip web to kotlin to web`() {
        val web = """
            {"plan-1":{"1":{"text":"Day one note","createdAt":"$iso"},"3":{"text":"","createdAt":"$iso"}}}
        """.trimIndent()
        val parsed = PlannerWire.reflectionsFromJson(web)
        val kotlin = mapOf(
            "plan-1" to mapOf(
                1 to PlannerReflection("Day one note", 1_772_600_767_000L),
                3 to PlannerReflection("", 1_772_600_767_000L)
            )
        )
        jsonEquals(web, PlannerWire.reflectionsToJson(parsed))
        assertEquals(kotlin, parsed)
    }

    @Test
    fun `reflections reader tolerates missing created_at and empty plans`() {
        val parsed = PlannerWire.reflectionsFromJson("""{"plan-1":{"2":{"text":"hi"}}}""")
        assertEquals(mapOf("plan-1" to mapOf(2 to PlannerReflection("hi", 0L))), parsed)
        assertEquals(emptyMap<String, Map<Int, PlannerReflection>>(), PlannerWire.reflectionsFromJson("{}"))
        assertEquals(emptyMap<String, Map<Int, PlannerReflection>>(), PlannerWire.reflectionsFromJson("garbage"))
    }

    @Test
    fun `embedded reflection text converts both ways`() {
        val embedded = mapOf(1 to "Day one note", 2 to "  ", 3 to "Day three")
        val entries = PlannerWire.reflectionsFromEmbedded(embedded, 1_772_600_767_000L)
        assertEquals(setOf(1, 3), entries.keys)
        assertEquals(1_772_600_767_000L, entries.getValue(1).createdAt)
        assertEquals(embedded.filterKeys { it != 2 }, PlannerWire.embeddedFromReflections(entries))
    }

    // ── plannerSessionTimers ──────────────────────────────────────────────
    @Test
    fun `session timers round trip web to kotlin to web`() {
        val web = """
            {"plan-1":{"1":{"startedAt":1772600767000,"totalSeconds":95}}}
        """.trimIndent()
        val parsed = PlannerWire.sessionTimersFromJson(web)
        assertEquals(
            mapOf("plan-1" to mapOf(1 to PlannerSessionTimer(95L, 1_772_600_767_000L))),
            parsed
        )
        jsonEquals(web, PlannerWire.sessionTimersToJson(parsed))
    }

    @Test
    fun `session timers accumulate seconds without losing the start stamp`() {
        val running = PlannerSessionTimer(95L, 1_772_600_767_000L)
        val stopped = running.copy(startedAt = null).copy(totalSeconds = running.totalSeconds + 42L)
        jsonEquals(
            """{"plan-1":{"1":{"totalSeconds":137}}}""",
            PlannerWire.sessionTimersToJson(mapOf("plan-1" to mapOf(1 to stopped)))
        )
    }

    @Test
    fun `session timers read legacy plain seconds`() {
        val legacy = """{"plan-1":{"1":95,"2":"42","3":0.5}}"""
        assertEquals(
            mapOf(
                "plan-1" to mapOf(
                    1 to PlannerSessionTimer(95L, null),
                    2 to PlannerSessionTimer(42L, null),
                    3 to PlannerSessionTimer(0L, null)
                )
            ),
            PlannerWire.sessionTimersFromJson(legacy)
        )
    }

    @Test
    fun `session timers reader rejects junk seconds`() {
        val parsed = PlannerWire.sessionTimersFromJson("""{"plan-1":{"1":"nope"}}""")
        assertEquals(mapOf("plan-1" to mapOf(1 to PlannerSessionTimer(0L, null))), parsed)
    }

    // ── plannerBookmarks ──────────────────────────────────────────────────
    @Test
    fun `bookmarks round trip web to kotlin to web`() {
        val web = """
            [{"verseKey":"2:255","surahName":"Al-Baqarah","note":"Ayat al-Kursi","createdAt":"$iso"}]
        """.trimIndent()
        val parsed = PlannerWire.bookmarksFromJson(web)
        assertEquals(
            listOf(PlannerBookmark("2:255", "Al-Baqarah", "Ayat al-Kursi", 1_772_600_767_000L)),
            parsed
        )
        assertEquals(web, PlannerWire.bookmarksToJson(parsed))
    }

    @Test
    fun `bookmarks read legacy epoch ms and missing created_at`() {
        val legacy = """
            [{"verseKey":"1:1","surahName":"Al-Fatihah","note":"","createdAt":1772600767000},
             {"verseKey":"2:1"}]
        """.trimIndent()
        val parsed = PlannerWire.bookmarksFromJson(legacy)
        assertEquals(2, parsed.size)
        assertEquals(1_772_600_767_000L, parsed[0].createdAt)
        assertEquals(0L, parsed[1].createdAt)
        assertEquals(emptyList<PlannerBookmark>(), PlannerWire.bookmarksFromJson("{}"))
    }
}
