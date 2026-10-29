package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity tests for the web sync truth in
 * quran-app/src/utils/syncMerge.js (mergeStateInto / slimStatePayload).
 */
class SyncMergeRulesTest {

    @Suppress("UNCHECKED_CAST")
    private fun merge(base: Map<String, Any?>, incoming: Map<String, Any?>): Map<String, Any?> =
        mergeStateInto(base, incoming) as Map<String, Any?>

    private fun field(state: Map<String, Any?>, field: String): Any? = state[field]

    private fun listField(state: Map<String, Any?>, field: String): List<Any?> =
        state[field] as List<Any?>

    @Test
    fun bookmarksUnionByVerseKeyAndIncomingWinsPerKey() {
        val base = mapOf(
            "bookmarks" to listOf(
                mapOf("verseKey" to "2:1", "page" to 1, "note" to "keep"),
                mapOf("verseKey" to "2:5", "page" to 5)
            )
        )
        val incoming = mapOf(
            "bookmarks" to listOf(
                mapOf("verseKey" to "2:5", "page" to 5, "note" to "new"),
                mapOf("verseKey" to "2:9", "page" to 9)
            )
        )
        val out = merge(base, incoming)
        val bookmarks = listField(out, "bookmarks")
        assertEquals(3, bookmarks.size)
        assertEquals(
            "new",
            ((bookmarks[1] as Map<*, *>)["note"])
        )
        assertEquals("2:9", (bookmarks[2] as Map<*, *>)["verseKey"])
    }

    @Test
    fun nullIdentityArraysUnionByValueLikeWeb() {
        // Web mergeArray keys these lists by the item itself (JS Map), so
        // shared entries dedupe instead of concatenating on every sync.
        val base = mapOf("memorizedAyahs" to listOf("2:1", "2:2"))
        val incoming = mapOf("memorizedAyahs" to listOf("2:2", "2:3"))
        val out = merge(base, incoming)
        assertEquals(listOf("2:1", "2:2", "2:3"), listField(out, "memorizedAyahs"))
    }

    @Test
    fun recentlyReadUnionsByChapterIdWithLaterTimestampWinning() {
        val base = mapOf(
            "recentlyRead" to listOf(
                mapOf("chapterId" to "1", "verse" to "1:1", "timestamp" to 100),
                mapOf("chapterId" to "2", "verse" to "2:1", "timestamp" to 200)
            )
        )
        val incoming = mapOf(
            "recentlyRead" to listOf(
                mapOf("chapterId" to "2", "verse" to "2:9", "timestamp" to 400),
                mapOf("chapterId" to "3", "verse" to "3:1", "timestamp" to 300)
            )
        )
        val out = merge(base, incoming)
        val recent = listField(out, "recentlyRead")
        // Sorted newest timestamp first, chapter 2 taken from the newer device.
        assertEquals(listOf("2", "3", "1"), recent.map { (it as Map<*, *>)["chapterId"] })
        assertEquals("2:9", (recent[0] as Map<*, *>)["verse"])
    }

    @Test
    fun recentlyReadKeepsOnlyNewestEntriesAndCapsAtTwenty() {
        val base = mapOf(
            "recentlyRead" to (1..3).map { mapOf("chapterId" to "$it", "timestamp" to it.toLong()) }
        )
        val out = merge(base, base) // sanity: union keeps 3 entries under the cap

        val cooling = mapOf(
            "recentlyRead" to (1..25).map { mapOf("chapterId" to "$it", "timestamp" to it.toLong()) }
        )
        val capped = merge(mapOf("recentlyRead" to emptyList<Any?>()), cooling)
        assertEquals(20, listField(capped, "recentlyRead").size)
        assertEquals("25", (listField(capped, "recentlyRead").first() as Map<*, *>)["chapterId"])
        assertEquals(3, listField(out, "recentlyRead").size)
    }

    @Test
    fun pageVisitCountsTakesPerKeyMaxWithNegativesClamped() {
        val base = mapOf("pageVisitCounts" to mapOf("1" to 3, "2" to -5, "4" to 0))
        val incoming = mapOf("pageVisitCounts" to mapOf("1" to 9, "2" to 7, "3" to 2))
        val counts = merge(base, incoming)["pageVisitCounts"] as Map<*, *>
        assertEquals(9.0, counts["1"])
        assertEquals(7.0, counts["2"])
        assertEquals(2.0, counts["3"])
        assertEquals(0.0, counts["4"])
    }

    @Test
    fun collectionsUnionByIdWithItemsKeyedByVerseKey() {
        val base = mapOf(
            "collections" to listOf(
                mapOf(
                    "id" to "c1",
                    "name" to "Base",
                    "items" to listOf(mapOf("verseKey" to "2:1", "note" to "old"))
                )
            )
        )
        val incoming = mapOf(
            "collections" to listOf(
                mapOf(
                    "id" to "c1",
                    "name" to "Incoming",
                    "items" to listOf(mapOf("verseKey" to "2:1", "note" to "new"), mapOf("verseKey" to "2:5"))
                ),
                mapOf("id" to "c2", "items" to listOf(mapOf("verseKey" to "1:1")))
            )
        )
        val out = merge(base, incoming)
        val collections = listField(out, "collections")
        assertEquals(2, collections.size)
        val first = collections[0] as Map<*, *>
        assertEquals("Incoming", first["name"])
        val items = first["items"] as List<*>
        assertEquals(2, items.size)
        assertEquals("new", (items[0] as Map<*, *>)["note"])
        assertEquals("2:5", (items[1] as Map<*, *>)["verseKey"])
        assertEquals("c2", (collections[1] as Map<*, *>)["id"])
    }

    @Test
    fun plannersUnionByIdWithBothChangedDayAssignmentsMerged() {
        val base = mapOf(
            "planners" to listOf(
                mapOf(
                    "id" to "p1",
                    "name" to "Base",
                    "target" to 2,
                    "assignments" to listOf(
                        mapOf("dayNumber" to 1, "surah" to "S1", "updatedAt" to 100),
                        mapOf("dayNumber" to 2, "surah" to "S2", "updatedAt" to 100)
                    )
                )
            )
        )
        val incoming = mapOf(
            "planners" to listOf(
                mapOf(
                    "id" to "p1",
                    "target" to 5,
                    "assignments" to listOf(
                        mapOf("dayNumber" to 1, "surah" to "S1b", "updatedAt" to 250),
                        mapOf("dayNumber" to 2, "surah" to "S2b", "updatedAt" to 50),
                        mapOf("dayNumber" to 3, "surah" to "S3", "updatedAt" to 10)
                    )
                ),
                mapOf("id" to "p2", "name" to "Other")
            )
        )
        val out = merge(base, incoming)
        val planners = listField(out, "planners")
        assertEquals(2, planners.size)
        val p1 = planners[0] as Map<*, *>
        // Non-array scalars deep-merge: both sides changed, incoming wins.
        assertEquals("Base", p1["name"])
        assertEquals(5, p1["target"])
        // Day arrays union per dayNumber with the latest updatedAt winning.
        val assignments = p1["assignments"] as List<*>
        assertEquals(3, assignments.size)
        assertEquals(listOf("S1b", "S2", "S3"), assignments.map { (it as Map<*, *>)["surah"] })
        assertEquals(listOf(1, 2, 3), assignments.map { (it as Map<*, *>)["dayNumber"] })
    }

    @Test
    fun dayArrayTiesPreferTheLaterItem() {
        val base = mapOf(
            "planners" to listOf(
                mapOf("id" to "p1", "assignments" to listOf(mapOf("dayNumber" to 1, "surah" to "base", "updatedAt" to 100)))
            )
        )
        val incoming = mapOf(
            "planners" to listOf(
                mapOf("id" to "p1", "assignments" to listOf(mapOf("dayNumber" to 1, "surah" to "incoming", "updatedAt" to 100)))
            )
        )
        val p1 = listField(merge(base, incoming), "planners")[0] as Map<*, *>
        val assignments = p1["assignments"] as List<*>
        assertEquals(1, assignments.size)
        assertEquals("incoming", (assignments[0] as Map<*, *>)["surah"])
    }

    @Test
    fun otherArraysUnionByIdentityThenReplaceWholeItem() {
        val base = mapOf(
            "planners" to listOf(
                mapOf(
                    "id" to "p1",
                    "tags" to listOf("memorize", mapOf("id" to 9, "label" to "keep"))
                )
            )
        )
        val incoming = mapOf(
            "planners" to listOf(
                mapOf(
                    "id" to "p1",
                    "tags" to listOf("recite", mapOf("id" to 9, "label" to "replaced"))
                )
            )
        )
        // Union by JSON value identity: base order first, fresh items appended.
        val tags = (listField(merge(base, incoming), "planners")[0] as Map<*, *>)["tags"] as List<*>
        assertEquals(
            listOf("memorize", mapOf("id" to 9, "label" to "keep"), "recite", mapOf("id" to 9, "label" to "replaced")),
            tags
        )
    }

    @Test
    fun identicalArrayItemsKeepBasePositionInsteadOfDuplicating() {
        val base = mapOf(
            "planners" to listOf(
                mapOf("id" to "p1", "tags" to listOf("memorize", mapOf("id" to 9)))
            )
        )
        val incoming = mapOf(
            "planners" to listOf(
                mapOf("id" to "p1", "tags" to listOf("recite", mapOf("id" to 9)))
            )
        )
        val tags = (listField(merge(base, incoming), "planners")[0] as Map<*, *>)["tags"] as List<*>
        assertEquals(listOf("memorize", mapOf("id" to 9), "recite"), tags)
    }

    @Test
    fun deepMergeMapsUnionRecursivelyOnBothSides() {
        val base = mapOf(
            "plannerBookmarks" to mapOf(
                "p1" to mapOf("dayNumber" to 1, "note" to "base", "surah" to "keep"),
                "p2" to mapOf("dayNumber" to 2)
            )
        )
        val incoming = mapOf(
            "plannerBookmarks" to mapOf(
                "p1" to mapOf("dayNumber" to 1, "note" to "incoming"),
                "p3" to mapOf("dayNumber" to 3)
            )
        )
        val bookmarks = merge(base, incoming)["plannerBookmarks"] as Map<*, *>
        assertEquals(3, bookmarks.size)
        val p1 = bookmarks["p1"] as Map<*, *>
        // Recursive merge: per-key incoming wins, base-only keys survive.
        assertEquals("incoming", p1["note"])
        assertEquals("keep", p1["surah"])
    }

    @Test
    fun nullOnIncomingKeepsBaseWhileBaseNullIsOverwritten() {
        val base = mapOf(
            "hifdhHistory" to mapOf(
                "2:1" to mapOf("reps" to 3, "note" to "keep"),
                "2:2" to null
            ),
            "theme" to "dark",
            "currentPage" to 10
        )
        val incoming = mapOf(
            "hifdhHistory" to mapOf("2:2" to mapOf("reps" to 5)),
            "currentPage" to 42
        )
        val out = merge(base, incoming)
        val history = out["hifdhHistory"] as Map<*, *>
        // `incoming ?? base` (web line 89): an incoming null never clobbers.
        assertEquals(mapOf("reps" to 3, "note" to "keep"), history["2:1"])
        // A null on the base side is overwritten by the incoming value.
        assertEquals(mapOf("reps" to 5), history["2:2"])
        // Fields missing from incoming keep the base value.
        assertEquals("dark", out["theme"])
        // Scalars / positions: incoming wins.
        assertEquals(42, out["currentPage"])
    }

    @Test
    fun topLevelNullOverwritesScalarButArrayFieldsTreatNullAsNoOp() {
        val out = merge(mapOf("theme" to "dark", "recentlyRead" to emptyList<Any>()), mapOf("theme" to null))
        assertNull(out["theme"])
        assertEquals(emptyList<Any>(), out["recentlyRead"])
    }

    @Test
    fun sessionHistoryCapsKeepTheLastFiveHundred() {
        val sessions = (1..600).map { mapOf("timestamp" to it.toLong(), "surah" to "s") }
        val out = merge(
            mapOf("readingSessions" to sessions, "pomodoroHistory" to sessions),
            mapOf("readingSessions" to (601..1200).map { mapOf("timestamp" to it.toLong(), "surah" to "s") })
        )
        val merged = field(out, "readingSessions") as List<*>
        assertEquals(500, merged.size)
        assertEquals(701L, (merged.first() as Map<*, *>)["timestamp"])
        assertEquals(1200L, (merged.last() as Map<*, *>)["timestamp"])
        // Fields absent from incoming are never touched, so no cap applies.
        assertEquals(600, (field(out, "pomodoroHistory") as List<*>).size)
    }

    @Test
    fun pomodoroHistoryIsCappedOnMergeToo() {
        val sessions = (1..600).map { mapOf("completedAt" to "t$it") }
        val out = merge(
            mapOf("pomodoroHistory" to sessions),
            mapOf("pomodoroHistory" to (601..1200).map { mapOf("completedAt" to "t$it") })
        )
        val merged = field(out, "pomodoroHistory") as List<*>
        assertEquals(500, merged.size)
        assertEquals("t701", (merged.first() as Map<*, *>)["completedAt"])
    }

    @Test
    fun plannerIsReDerivedFromActivePlannerIdAfterMerge() {
        val base = mapOf(
            "planners" to listOf(mapOf("id" to "p1", "name" to "Old"), mapOf("id" to "p2", "name" to "P2")),
            "activePlannerId" to "p1",
            "planner" to mapOf("id" to "stale", "name" to "Stale")
        )
        val incoming = mapOf("planners" to listOf(mapOf("id" to "p1", "name" to "Fresh")))
        val out = merge(base, incoming)
        val planner = field(out, "planner") as Map<*, *>
        assertEquals("p1", planner["id"])
        assertEquals("Fresh", planner["name"])
    }

    @Test
    fun plannerFallsBackToCurrentPlannerThenPreservesIt() {
        val base = mapOf(
            "planners" to listOf(mapOf("id" to "p1", "name" to "P1"), mapOf("id" to "p2", "name" to "P2")),
            "activePlannerId" to "missing",
            "planner" to mapOf("id" to "p2", "name" to "P2")
        )
        val out = merge(base, mapOf("planners" to listOf(mapOf("id" to "p1", "name" to "P1"))))
        assertEquals("p2", (field(out, "planner") as Map<*, *>)["id"])

        val onlyCurrent = merge(
            mapOf(
                "planners" to listOf(mapOf("id" to "p9", "name" to "P9")),
                "activePlannerId" to "missing",
                "planner" to mapOf("id" to "other", "name" to "Other")
            ),
            mapOf("planners" to listOf(mapOf("id" to "p8", "name" to "P8")))
        )
        assertEquals("other", (field(onlyCurrent, "planner") as Map<*, *>)["id"])
    }

    @Test
    fun slimStatePayloadDropsPlannerAndClampsVisitCounts() {
        val state = mapOf(
            "planner" to mapOf("id" to "p1"),
            "activePlannerId" to "p1",
            "pageVisitCounts" to mapOf("1" to 99999, "2" to -7, "3" to "abc", "4" to 12.9),
            "planners" to listOf(
                mapOf(
                    "id" to "p1",
                    "assignments" to listOf(
                        mapOf("dayNumber" to 1, "surah" to "first", "updatedAt" to 50),
                        mapOf("dayNumber" to 1, "surah" to "second", "updatedAt" to 50),
                        mapOf("dayNumber" to 2, "surah" to "newer", "updatedAt" to 90)
                    )
                )
            )
        )
        val slim = slimStatePayload(state)
        assertTrue(slim.containsKey("activePlannerId"))
        assertTrue(!slim.containsKey("planner"))
        val counts = slim["pageVisitCounts"] as Map<*, *>
        // "3" held a non-numeric value, so the key is dropped entirely.
        assertEquals(3, counts.size)
        assertEquals(10000.0, counts["1"])
        assertEquals(0.0, counts["2"])
        assertEquals(12.0, counts["4"])
        assertEquals(setOf("1", "2", "4"), counts.keys)
        // Assignments dedupe per dayNumber; a tie keeps the earlier item.
        val assignments = (slim["planners"] as List<*>)[0] as Map<*, *>
        val days = assignments["assignments"] as List<*>
        assertEquals(2, days.size)
        assertEquals("first", (days[0] as Map<*, *>)["surah"])
        assertEquals("newer", (days[1] as Map<*, *>)["surah"])
    }

    @Test
    fun oversizedPayloadTrimsSessionsToLastHundred() {
        val big = "x".repeat(8_000)
        val sessions = (1..120).map { mapOf("timestamp" to it.toLong(), "note" to big) }
        val slim = slimStatePayload(mapOf("readingSessions" to sessions))
        val kept = slim["readingSessions"] as List<*>
        assertEquals(100, kept.size)
        assertEquals(21L, (kept.first() as Map<*, *>)["timestamp"])
        assertEquals(120L, (kept.last() as Map<*, *>)["timestamp"])
    }

    @Test
    fun payloadStillTooLargeAfterSlimThrows() {
        val state = mapOf("notes" to "y".repeat(1_000_000))
        val error = try {
            slimStatePayload(state)
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertTrue(error != null && error.message!!.startsWith("sync payload too large: "))
    }
}
