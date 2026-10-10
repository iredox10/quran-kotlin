package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deep-merge and union rules on the keyed maps and primitive arrays a web document sends. */
class WebMergeKeyedMapTest {

    private val service = syncRig().service
    private val web = webState()

    @Test
    fun `keyed maps keep every entry from both devices`() {
        val base = mapOf(
            "plannerReflections" to mapOf("plan-a" to mapOf("1" to mapOf("text" to "android day 1"))),
            "plannerSessionTimers" to mapOf("plan-a" to mapOf("1" to mapOf("totalSeconds" to 120))),
            "plannerBookmarks" to mapOf("plan-a" to listOf(mapOf("verseKey" to "1:1"))),
            "pageVisitCounts" to mapOf("home" to 12, "surah" to 45),
            "saukaProgress" to mapOf("sauka-1" to 12),
        )
        val incoming = mapOf(
            "plannerReflections" to mapOf(
                "plan-a" to mapOf("2" to mapOf("text" to "web day 2")),
                "plan-b" to mapOf("1" to mapOf("text" to "web plan b")),
            ),
            "plannerSessionTimers" to mapOf("plan-b" to mapOf("2" to mapOf("totalSeconds" to 60))),
            "plannerBookmarks" to mapOf("plan-b" to listOf(mapOf("verseKey" to "2:1"))),
            "pageVisitCounts" to mapOf("surah" to 45, "planner" to 7),
            "saukaProgress" to mapOf("sauka-2" to 3),
        )
        val merged = service.mergeStateInto(base, incoming)

        @Suppress("UNCHECKED_CAST")
        val reflections = merged["plannerReflections"] as Map<String, Map<String, Map<String, Any?>>>
        assertEquals("android day 1", reflections["plan-a"]?.get("1")?.get("text"))
        assertEquals("web day 2", reflections["plan-a"]?.get("2")?.get("text"))
        assertEquals("web plan b", reflections["plan-b"]?.get("1")?.get("text"))

        @Suppress("UNCHECKED_CAST")
        val timers = merged["plannerSessionTimers"] as Map<String, Any?>
        assertEquals(setOf("plan-a", "plan-b"), timers.keys)

        @Suppress("UNCHECKED_CAST")
        val bookmarksByPlan = merged["plannerBookmarks"] as Map<String, Any?>
        assertEquals(setOf("plan-a", "plan-b"), bookmarksByPlan.keys)

        @Suppress("UNCHECKED_CAST")
        val counts = merged["pageVisitCounts"] as Map<String, Any?>
        assertEquals(setOf("home", "surah", "planner"), counts.keys)

        @Suppress("UNCHECKED_CAST")
        val sauka = merged["saukaProgress"] as Map<String, Any?>
        assertEquals(setOf("sauka-1", "sauka-2"), sauka.keys)
    }

    @Test
    fun `primitive arrays are unioned, never replaced`() {
        val merged = service.mergeStateInto(
            mapOf("completedTours" to listOf("home-tour"), "memorizedSurahs" to listOf(112)),
            web,
        )
        assertEquals(listOf("home-tour", "surah-tour"), merged["completedTours"])
        val surahs = merged["memorizedSurahs"] as List<*>
        assertTrue(surahs.contains(112))
        assertTrue(surahs.contains(112.0))
    }

    @Test
    fun `a base-only web field is never dropped on the floor`() {
        val merged = service.mergeStateInto(mapOf("dismissedGestureTips" to listOf("android-tip")), web)
        assertTrue((merged["dismissedGestureTips"] as List<*>).contains("android-tip"))
        assertTrue((merged["completedTours"] as List<*>).containsAll(listOf("home-tour", "surah-tour")))
    }
}
