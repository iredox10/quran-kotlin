package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the pure web → Android key mappings used by
 * `SyncService.applyState`. Prefs/DAO writes stay in the service (Android
 * Context) and are not covered here.
 */
class SyncApplyMappingsTest {

    // ------------------------------------------------------- value coercion ---

    @Test
    fun `string values are trimmed and blanks dropped`() {
        assertEquals("dark", asString("  dark "))
        assertNull(asString("   "))
        assertNull(asString(42))
    }

    @Test
    fun `numbers and numeric strings both coerce to scalars`() {
        assertEquals(131, asInt(131.0))
        assertEquals(131, asInt("131"))
        assertEquals(131, asInt("131.0"))
        assertNull(asInt("abc"))
        assertNull(asInt(true))
        assertEquals(0.75f, asFloat("0.75"))
        assertNull(asFloat("nope"))
        assertEquals(true, asBoolean("True"))
        assertEquals(false, asBoolean("FALSE"))
        assertNull(asBoolean(1))
        assertEquals(1712L, asLong(1712.0))
        assertEquals(0L, asLong("nope"))
    }

    @Test
    fun `json strings parse into lists and maps`() {
        assertEquals(listOf("1:1"), asList("[\"1:1\"]"))
        assertEquals(mapOf("k" to 1.0), asMap("{\"k\":1}"))
        assertNull(asList("{\"k\":1}"))
        assertNull(asMap("[1,2]"))
        assertNull(asList("not json"))
        assertNull(asList(7))
    }

    // ------------------------------------------------------------ font sizes ---

    @Test
    fun `web font index maps onto the android scale range`() {
        assertEquals(1f, webFontIndexToScale(2))
        assertEquals(0.75f, webFontIndexToScale(1))
        assertEquals(2.5f, webFontIndexToScale(8))
        assertEquals(1.25f, webFontIndexToScale(3))
    }

    @Test
    fun `font index clamps outside the web range and drops junk`() {
        assertEquals(0.5f, webFontIndexToScale(0))
        assertEquals(3f, webFontIndexToScale(99))
        assertNull(webFontIndexToScale("big"))
    }

    // ------------------------------------------------- font / tooltip / mushaf ---

    @Test
    fun `web arabic font ids become android display names`() {
        assertEquals("Scheherazade New", webArabicFontName("scheherazade-new"))
        assertEquals("KFGQPC Hafs", webArabicFontName("KFGQPC-HAFS"))
    }

    @Test
    fun `unknown arabic font ids are dropped so local fonts survive`() {
        assertNull(webArabicFontName("comic-sans"))
        assertNull(webArabicFontName("Scheherazade New"))
        assertNull(webArabicFontName(null))
    }

    @Test
    fun `word tooltip behavior keeps only values android knows`() {
        assertEquals("tajweed", webWordTapBehavior(" Tajweed "))
        assertEquals("none", webWordTapBehavior("none"))
        assertNull(webWordTapBehavior("highlight"))
        assertNull(webWordTapBehavior(""))
    }

    @Test
    fun `web mushaf id canonicalizes to the android preset key`() {
        assertEquals("madani-tajweed", webMushafPreset("madani-tajweed"))
        assertEquals("madani-standard", webMushafPreset("uthmani"))
        assertEquals("indopak", webMushafPreset("indopak"))
        assertNull(webMushafPreset("  "))
    }

    @Test
    fun `daily reading goal is kept as a non-negative minute count`() {
        assertEquals(45, webDailyReadingGoal(45))
        assertEquals(0, webDailyReadingGoal(-5))
        assertNull(webDailyReadingGoal("hours"))
    }

    // ------------------------------------------------------ prayer settings ---

    @Test
    fun `prayer settings keep only valid prayers in canonical order`() {
        val settings = webPrayerSettings(
            mapOf(
                "activePrayers" to listOf("Isha", "Maghrib", "Tahajjud"),
                "readPreference" to "Split"
            )
        )!!
        assertEquals(listOf("Maghrib", "Isha"), settings.activePrayers)
        assertEquals("split", settings.readPreference)
    }

    @Test
    fun `prayer settings survive a half-readable object`() {
        val settings = webPrayerSettings(mapOf("readPreference" to "before"))!!
        assertNull(settings.activePrayers)
        assertEquals("before", settings.readPreference)
    }

    @Test
    fun `unreadable prayer settings are dropped entirely`() {
        assertNull(webPrayerSettings(mapOf("activePrayers" to listOf("Nope"))))
        assertNull(webPrayerSettings("nope"))
        assertNull(webPrayerSettings(null))
    }

    // -------------------------------------------------------- hifdh settings ---

    @Test
    fun `memorized surahs and ayahs become android string sets`() {
        assertEquals(setOf("1", "2", "2"), webMemorizedSurahSet(listOf(2.0, 1.0, "x")))
        assertNull(webMemorizedSurahSet("nope"))
        assertEquals(setOf("2:255"), webMemorizedAyahSet(listOf(" 2:255 ")))
        assertEquals(setOf("2:255"), webMemorizedAyahSet(listOf("2:255", "2:255")))
    }

    // -------------------------------------------------- tours / coachmarks ----

    @Test
    fun `tour and coachmark ids are trimmed and de-duplicated`() {
        assertEquals(listOf("home", "surah"), webFlagIds(listOf(" home ", "surah", "home", "")))
        assertNull(webFlagIds("nope"))
    }

    @Test
    fun `page visit counts drop blank keys and non-positive counts`() {
        assertEquals(
            mapOf("home" to 3, "surah" to 1),
            webPageVisits(mapOf("home" to 3.0, "surah" to "1", " " to 9, "library" to 0))
        )
        assertNull(webPageVisits("nope"))
        assertNull(webPageVisits(null))
    }

    // ------------------------------------------------------------- bookmarks ---

    @Test
    fun `bookmark rows map the web createdAt stamp`() {
        val rows = webBookmarkRows(
            listOf(mapOf("verseKey" to "2:255", "surahName" to "Al-Baqarah", "chapterId" to 2.0, "createdAt" to 1712.0))
        )!!
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("2:255", row.verseKey)
        assertEquals(2, row.chapterId)
        assertEquals("Al-Baqarah", row.surahName)
        assertEquals(1712L, row.createdAt)
    }

    @Test
    fun `bookmark rows accept the legacy android timestamp field`() {
        val rows = webBookmarkRows(listOf(mapOf("verseKey" to "1:1", "timestamp" to 99.0)))!!
        assertEquals(99L, rows.single().createdAt)
        assertEquals(1, rows.single().chapterId)
    }

    @Test
    fun `bookmark rows without a verse key or chapter are skipped`() {
        // chapterId falls back to the verse key prefix, so only an unusable
        // verse key drops the row.
        assertTrue(webBookmarkRows(listOf(mapOf("verseKey" to "abc")))!!.isEmpty())
        assertTrue(webBookmarkRows(listOf("nope", 7))!!.isEmpty())
        assertNull(webBookmarkRows("nope"))
    }

    // ----------------------------------------------------------- collections ---

    @Test
    fun `collection rows map nested web items`() {
        val rows = webCollectionRows(
            listOf(
                mapOf(
                    "id" to 1712.0,
                    "name" to " Dua ",
                    "createdAt" to 1700.0,
                    "items" to listOf(
                        mapOf("verseKey" to "2:255", "surahName" to "Al-Baqarah", "chapterId" to 2.0)
                    )
                )
            )
        )!!
        val row = rows.single()
        assertEquals(1712L, row.id)
        assertEquals("Dua", row.name)
        assertEquals(1700L, row.createdAt)
        // Web omits addedAt: the collection stamp is used, and chapterId is
        // derivable from the verse key when the payload leaves it null.
        val item = row.items.single()
        assertEquals("2:255", item.verseKey)
        assertEquals(2, item.chapterId)
        assertEquals(1700L, item.addedAt)
    }

    @Test
    fun `collection rows tolerate string ids and bad items`() {
        val rows = webCollectionRows(
            listOf(mapOf("id" to "plan-7", "items" to listOf("nope", mapOf("verseKey" to "1:1"))))
        )!!
        assertEquals("plan-7".hashCode().toLong(), rows.single().id)
        assertEquals(1, rows.single().items.size)
        assertTrue(webCollectionRows(listOf(mapOf("id" to "plan-7", "items" to listOf("nope", mapOf("verseKey" to "1:1")))))!!.let { it.size == 1 && it.single().items.size == 1 })
        assertTrue(webCollectionRows(listOf(mapOf("name" to "no id")))!!.isEmpty())
    }

    // ------------------------------------------------------ reading sessions ---

    @Test
    fun `reading session rows require the identity timestamp`() {
        val rows = webReadingSessionRows(
            listOf(
                mapOf("date" to "2026-01-01", "duration" to 30.0, "type" to "reading", "chapterId" to 2.0, "timestamp" to 1712.0),
                mapOf("duration" to 30.0)
            )
        )!!
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("2026-01-01", row.date)
        assertEquals(30L, row.duration)
        assertEquals(2, row.chapterId)
        assertEquals(1712L, row.timestamp)
        assertNull(webReadingSessionRows("nope"))
    }

    @Test
    fun `reading session rows default the type and date`() {
        val rows = webReadingSessionRows(listOf(mapOf("duration" to 5.0, "timestamp" to 3.0)))!!
        assertEquals("reading", rows.single().type)
        assertEquals("", rows.single().date)
    }

    // ---------------------------------------------------------- recently read ---

    @Test
    fun `recently read rows require a chapter id`() {
        val rows = webRecentlyReadRows(
            listOf(
                mapOf("chapterId" to 18.0, "chapterName" to "Al-Kahf", "verseKey" to "18:10", "timestamp" to 5.0),
                mapOf("chapterName" to "orphan")
            )
        )!!
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(18, row.chapterId)
        assertEquals("Al-Kahf", row.chapterName)
        assertEquals("18:10", row.verseKey)
        assertEquals(5L, row.timestamp)
        assertNull(webRecentlyReadRows(7))
    }

    // ------------------------------------------------------ planner extras ----

    @Test
    fun `planner bookmark rows drop the web only createdAt stamp`() {
        val rows = webPlannerBookmarkRows(
            listOf(
                mapOf(
                    "verseKey" to "2:255",
                    "surahName" to "Al-Baqarah",
                    "note" to " revise ",
                    "createdAt" to "2026-01-01T00:00:00.000Z"
                ),
                mapOf("note" to "no verse key")
            )
        )!!
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("2:255", row.verseKey)
        assertEquals("Al-Baqarah", row.surahName)
        assertEquals("revise", row.note)
        assertNull(webPlannerBookmarkRows("nope"))
    }

    @Test
    fun `planner session totals read web totalSeconds per day`() {
        val totals = webPlannerSessionTotals(
            mapOf("1" to mapOf("totalSeconds" to 120.0), "2" to mapOf("totalSeconds" to 0.0), "x" to 5)
        )!!
        assertEquals(mapOf(1 to 120L, 2 to 0L), totals)
        assertNull(webPlannerSessionTotals("nope"))
    }

    @Test
    fun `planner reflections read the web note text per day`() {
        val reflections = webPlannerReflections(
            mapOf("2" to mapOf("text" to " Revised today "), "x" to mapOf("text" to "ignored"), "3" to mapOf())
        )!!
        assertEquals(mapOf(2 to "Revised today"), reflections)
        assertNull(webPlannerReflections("nope"))
    }

    @Test
    fun `planner reflections group by plan and skip empties`() {
        val byPlan = webPlannerReflectionsByPlan(
            mapOf(
                "plan-a" to mapOf("1" to mapOf("text" to "note")),
                "plan-b" to mapOf(),
                " " to mapOf("1" to mapOf("text" to "note"))
            )
        )!!
        assertEquals(1, byPlan.size)
        assertEquals(mapOf(1 to "note"), byPlan["plan-a"])
    }

    @Test
    fun `reflections fold into a plan map without dropping other keys`() {
        val plan = mapOf("id" to "plan-a", "assignmentReflections" to mapOf("1" to "old"))
        val merged = applyWebReflectionsToPlan(plan, mapOf(1 to "new", 2 to "second"))
        assertEquals("plan-a", merged["id"])
        assertEquals(mapOf("1" to "new", "2" to "second"), merged["assignmentReflections"])
        assertEquals(plan, applyWebReflectionsToPlan(plan, null))
    }

    @Test
    fun `plans are unioned by id with the remote winning`() {
        val local = listOf(mapOf("id" to "a", "name" to "local a"), mapOf("id" to "b"))
        val remote = listOf(mapOf("id" to "a", "name" to "remote a"), mapOf("id" to "c"))
        val merged = mergePlansById(local, remote)!!
        assertEquals(listOf("a", "b", "c"), merged.map { (it as Map<*, *>)["id"] })
        assertEquals("remote a", (merged[0] as Map<*, *>)["name"])
        assertEquals(mergePlansById(local, null), local)
        assertEquals(mergePlansById(null, remote), remote)
        assertNull(mergePlansById(null, null))
    }

    @Test
    fun `partial planner rows do not erase required fields during cache application`() {
        val full = mapOf("id" to "a", "title" to "My plan", "durationDays" to 1,
            "assignments" to listOf(mapOf("dayNumber" to 1)), "assignmentProgress" to mapOf("1" to 1))
        val partial = mapOf("id" to "a", "assignmentReflections" to mapOf("1" to "Note"))
        val merged = mergePlansById(listOf(full), listOf(partial))!!.single() as Map<*, *>
        assertEquals("My plan", merged["title"])
        assertEquals(full["assignments"], merged["assignments"])
        assertEquals(full["assignmentProgress"], merged["assignmentProgress"])
        assertEquals(partial["assignmentReflections"], merged["assignmentReflections"])
    }

    @Test
    fun `synced timer cache remains readable after a planner refresh`() {
        val json = webPlannerTimerCacheJson("p", mapOf("1" to mapOf("totalSeconds" to 120), "2" to 30))!!
        val cached = com.nur.quran.data.planner.PlannerWire.sessionTimersFromJson(json)["p"]!!
        assertEquals(120L, cached[1]!!.totalSeconds)
        assertEquals(30L, cached[2]!!.totalSeconds)
        assertNull(webPlannerTimerCacheJson("p", "invalid"))
    }

    @Test
    fun `synced reflection cache updates the canonical note store`() {
        val json = webPlannerReflectionCacheJson("p", mapOf("1" to mapOf("text" to "Updated note", "createdAt" to "2026-10-29T10:00:00Z")))!!
        val cached = com.nur.quran.data.planner.PlannerWire.reflectionsFromJson(json)["p"]!!
        assertEquals("Updated note", cached[1]!!.text)
        assertTrue(cached[1]!!.createdAt > 0)
        val legacy = webPlannerReflectionCacheJson("p", mapOf("1" to "Legacy note"))!!
        assertEquals("Legacy note", com.nur.quran.data.planner.PlannerWire.reflectionsFromJson(legacy)["p"]!![1]!!.text)
        assertNull(webPlannerReflectionCacheJson("p", "invalid"))
    }

    // ------------------------------------------------------------- extras ids ---

    @Test
    fun `planner extra entries drop blank plan ids`() {
        assertEquals(
            listOf("plan-a" to emptyList<Any?>()),
            webPlannerExtraEntries(mapOf(" plan-a " to emptyList<Any?>(), "  " to listOf(1)))
        )
        assertNull(webPlannerExtraEntries("nope"))
    }
}
