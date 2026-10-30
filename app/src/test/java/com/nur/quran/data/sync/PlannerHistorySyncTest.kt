package com.nur.quran.data.sync

import com.nur.quran.data.planner.PlannerWire
import org.junit.Assert.*
import org.junit.Test

class PlannerHistorySyncTest {
    @Test fun `archived-only library still exports all extra record owners`() {
        assertEquals(listOf("finished"), plannerExtraPlanIds(emptyList<Any>(), listOf(mapOf("id" to "finished"))))
        assertEquals(listOf("live", "finished"), plannerExtraPlanIds(listOf(mapOf("id" to "live")), listOf(mapOf("id" to "finished"), mapOf("id" to "live"))))
    }

    @Test fun `synced date ledger survives native cache conversion and legacy client updates`() {
        val original = mapOf("plannerSessionTimers" to mapOf("p" to mapOf("1" to mapOf("totalSeconds" to 60, "secondsByDate" to mapOf("2026-10-01" to 60)))))
        val incoming = mapOf("plannerSessionTimers" to mapOf("p" to mapOf("1" to mapOf("totalSeconds" to 90, "secondsByDate" to mapOf("2026-10-02" to 30)))))
        val merged = mergeField("plannerSessionTimers", original["plannerSessionTimers"], incoming["plannerSessionTimers"]) as Map<*, *>
        val json = webPlannerTimerCacheJson("p", merged["p"])!!
        val timer = PlannerWire.sessionTimersFromJson(json)["p"]!![1]!!
        assertEquals(90L, timer.totalSeconds)
        assertEquals(mapOf("2026-10-01" to 60L, "2026-10-02" to 30L), timer.secondsByDate)
    }
}
