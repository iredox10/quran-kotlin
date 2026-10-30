package com.nur.quran.i18n

import com.google.gson.Gson
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.PlannerWire
import org.junit.Assert.*
import org.junit.Test
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Interface changes must never translate stable plan identifiers or serialized dates. */
class LocalizedPlannerDataContractTest {
    @Test fun `Arabic interface locale keeps wire dates numeric and planner keys unchanged`() {
        val original = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("1970-01-01", PlannerEngine.formatPlannerDate(Date(0)))
            assertEquals("1970-01-01T00:00:00.000Z", PlannerWire.isoFromEpochMs(0))
            val plan = PlannerEngine.buildReadingPlanner(
                unitType = "page", durationDays = 2, startDate = "2026-10-30",
                startUnit = 1, endUnit = 4, chapters = emptyList()
            )
            assertEquals("page", plan.unitType)
            assertEquals(listOf(1, 2), plan.assignments.map { it.dayNumber })
            assertEquals(listOf("1", "2", "3", "4"), plan.assignments.flatMap { it.items.map { item -> item.rangeValue } })
            assertTrue(plan.assignments.all { it.date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) })
            assertEquals(plan, PlannerEngine.parseReadingPlan(Gson().toJson(plan), Gson()))
        } finally {
            Locale.setDefault(original)
            TimeZone.setDefault(originalZone)
        }
    }
}
