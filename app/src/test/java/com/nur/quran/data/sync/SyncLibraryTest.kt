package com.nur.quran.data.sync

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class SyncLibraryTest {
    @Test fun `JSON double identities match local integer identities without repeated uploads`() {
        val fromWire = mapOf("readingSessions" to listOf(mapOf("timestamp" to 1793280475969.0,"duration" to 60.0)),
            "collections" to listOf(mapOf("id" to 1793280475969.0,"name" to "Saved")))
        val local = mapOf("readingSessions" to listOf(mapOf("timestamp" to 1793280475969L,"duration" to 60)),
            "collections" to listOf(mapOf("id" to 1793280475969L,"name" to "Saved")))
        assertEquals(emptyMap<String,Any?>(), syncDelta(fromWire,local))
    }

    @Test
    fun `dated removals defeat an old bookmark and a new add defeats an old removal`() {
        val row=mapOf("verseKey" to "2:255", "createdAt" to 1000L)
        val removed=mapOf("bookmarks" to listOf(row), "bookmark" to row, "bookmarkRemovals" to mapOf("2:255" to 2000L))
        assertEquals(emptyList<Any>(), normalizeSyncLibrary(removed)["bookmarks"])
        assertNull(normalizeSyncLibrary(removed)["bookmark"])
        val readded=removed + ("bookmarks" to listOf(row + ("createdAt" to 3000L)))
        assertEquals(1,(normalizeSyncLibrary(readded)["bookmarks"] as List<*>).size)
        assertEquals(mapOf("2:255" to 2000.0),mergeField("bookmarkRemovals",mapOf("2:255" to 2000L),mapOf("2:255" to 1000L)))
    }

    @Test
    fun `oversized legacy planner mirror is compacted without dropping saved timers or notes`() {
        val plan=mapOf("id" to "test-plan", "notes" to "q".repeat(700000))
        val legacy=Gson().toJson(mapOf("allPlans" to listOf(plan),"activePlan" to plan,"sessionTotals" to mapOf("test-plan" to mapOf("1" to 3678))))
        val state=mapOf("planners" to listOf(plan),"plansJson" to legacy)
        assertTrue(Gson().toJson(state).length>1_000_000)
        val compact=compactSyncPayload(state)
        assertFalse(compact.containsKey("plansJson"))
        assertFalse(compact.containsKey("planner"))
        assertTrue(Gson().toJson(compact).length<1_000_000)
        assertEquals(state["planners"],compact["planners"])
        val timers=compact["plannerSessionTimers"] as Map<*, *>
        assertEquals(3678.0,(((timers["test-plan"] as Map<*, *>)["1"] as Map<*, *>)["totalSeconds"] as Number).toDouble(),0.0)
        // An existing retry packet may contain only the old mirror, with the full snapshot beside it.
        assertFalse(compactSyncPayload(mapOf("plansJson" to legacy),state).containsKey("plansJson"))
    }
}
