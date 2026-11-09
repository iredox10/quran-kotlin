package com.nur.quran.data.reminders

import com.nur.quran.data.planner.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PlannerReminderEngineTest {
    private val zone = ZoneId.of("Africa/Lagos")
    private val date = LocalDate.of(2026, 11, 9)
    private fun at(time: String) = date.atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()
    private val settings = ReminderSettings(enabled = true, confirmed = true, times = PRAYER_NAMES.zip(listOf("05:30", "13:00", "16:00", "18:30", "20:00")).toMap())
    private fun plan(count: Int = 10): ReadingPlan {
        val items = (1..count).map { PlannerItem(it, "$it", "", "", "$it", it, it) }
        return ReadingPlan(id = "run-1", unitType = "page", title = "Daily", durationDays = 1, startDate = date.toString(), startUnit = 1, endUnit = count,
            assignments = listOf(PlannerAssignment(1, date.toString(), "page", "Day", "", 1, count, "", 1, count, items)))
    }
    private fun portions(plan: ReadingPlan? = plan(), config: ReminderSettings = settings, preference: String = "after", pace: PlannerPagePace? = null) =
        PlannerReminderEngine.portions(plan, config, PRAYER_NAMES, preference, at("05:00"), zone, pace)
    private val first get() = portions().first()
    private fun next(now: Long, receipts: Map<String, ReminderReceipt> = emptyMap(), config: ReminderSettings = settings, lastSent: Long = 0) =
        PlannerReminderEngine.next(listOf(first), config, receipts, now, lastSent)

    @Test fun disabledAndUnconfirmedNeverSchedule() {
        assertTrue(portions(config = settings.copy(enabled = false)).isEmpty())
        assertTrue(portions(config = settings.copy(confirmed = false)).isEmpty())
        assertTrue(portions(config = settings.copy(times = emptyMap())).isEmpty())
        assertTrue(portions(plan = null).isEmpty())
    }
    @Test fun eachSlotMatchesThePlannerPartitionForUnevenDays() {
        val plan = plan(7)
        val slots = PlannerEngine.buildPrayerSlots(plan, plan.assignments.first(), null)
        val portions = portions(plan)
        slots.forEach { slot -> assertEquals(plan.assignments.first().items.subList(slot.slotStart, slot.slotEnd).map { it.pageStart }, portions.first { it.prayer == slot.name }.pages) }
    }
    @Test fun fewerPagesThanPrayersDoesNotNotifyEmptySlots() {
        assertEquals(listOf("Fajr", "Asr"), portions(plan(2)).map { it.prayer })
    }
    @Test fun explicitAndLegacyProgressBothStopCompletedPortions() {
        assertEquals(listOf(2), portions(plan().copy(assignmentReadPages = mapOf(1 to listOf(1)))).first().pages)
        assertEquals("Dhuhr", portions(plan().copy(assignmentProgress = mapOf(1 to 2))).first().prayer)
        assertTrue(portions(plan().copy(assignmentReadPages = mapOf(1 to (1..10).toList()))).isEmpty())
    }
    @Test fun outsideLogCompletionUsesTheSameDurableProgress() {
        val p = plan().copy(lastReadPage = 1)
        val result = PlannerOutsideReadings.record(p, 1, 2, date.toString(), PlannerOutsideReadings.MANUAL,
            mapOf(1 to 60L, 2 to 60L), now = at("05:00"), today = date.toString())
        assertEquals("Dhuhr", portions(result.plan).first().prayer)
    }
    @Test fun staleCompletedDaysDoNotHideRebalancedWork() {
        assertEquals(5, portions(plan().copy(completedDays = listOf(1))).size)
    }
    @Test fun sharedSurahBoundaryStillRemindsForTheUnreadItem() {
        val original = plan(2)
        val a = original.assignments.first().copy(items = original.assignments.first().items.map { it.copy(pageStart = 1, pageEnd = 1) })
        val p = original.copy(assignments = listOf(a), assignmentCompletedItems = mapOf(1 to listOf("1")))
        assertEquals(listOf(1), portions(p).single().pages)
    }
    @Test fun noRestDayOrArchivedPlanAlerts() {
        assertTrue(portions(plan().copy(archivedAt = "2026-11-09")).isEmpty())
        val p = plan()
        assertTrue(portions(p.copy(assignments = p.assignments.map { it.copy(date = date.minusDays(4).toString()) })).isEmpty())
    }
    @Test fun newPlanRunHasIndependentReceipts() {
        val old = first
        val fresh = portions(plan().copy(id = "run-2")).first()
        assertNotEquals(old.key, fresh.key)
        assertNotNull(PlannerReminderEngine.next(listOf(fresh), settings, mapOf(old.key to ReminderReceipt(3)), at("05:30")))
    }
    @Test fun customDeadlineDoesNotDependOnReadingPreference() {
        assertEquals(at("05:30"), portions(preference = "before").first().deadline)
        assertEquals(at("05:30"), portions(preference = "split").first().deadline)
    }
    @Test fun measuredPaceSetsThePreparationLead() {
        val p = portions(pace = PlannerPagePace(600, 8)).first()
        val event = PlannerReminderEngine.next(listOf(p), settings, emptyMap(), at("04:00"))!!
        assertEquals(at("05:05"), event.at) // 2 pages x 10 min + 5 min buffer
        assertNull(first.seconds)
        assertEquals(at("05:20"), next(at("04:00"))!!.at)
    }
    @Test fun lateWakeupCollapsesPhasesInsteadOfReplaying() {
        assertEquals(2, next(at("05:45"))!!.phase)
        assertEquals(3, next(at("06:00"))!!.phase)
        assertNull(next(at("07:26")))
    }
    @Test fun deliveredPhasesAreNotRepeatedAndFinalIsBounded() {
        assertEquals(at("05:40"), next(at("05:30"), mapOf(first.key to ReminderReceipt(1, at("05:30"))))!!.at)
        assertNull(next(at("06:00"), mapOf(first.key to ReminderReceipt(3, at("05:55")))))
    }
    @Test fun snoozeRevalidatesAtTheRequestedTimeWithoutProgressMutation() {
        val receipt = mapOf(first.key to ReminderReceipt(1, at("05:30"), at("05:40")))
        val event = next(at("05:31"), receipt)!!
        assertEquals(at("05:40"), event.at)
        assertTrue(event.snoozed)
        assertEquals(listOf(1, 2), event.portion.pages)
    }
    @Test fun explicitSnoozeCanRepeatFinalButAutomaticCannot() {
        val event = next(at("06:00"), mapOf(first.key to ReminderReceipt(3, at("05:55"), at("06:10"))))!!
        assertEquals(3, event.phase); assertEquals(at("06:10"), event.at)
    }
    @Test fun multiplePortionsAreGloballyThrottled() {
        assertEquals(at("05:39"), next(at("05:30"), lastSent = at("05:29"))!!.at)
    }
    @Test fun readingSuppressesTheTargetWithoutMarkingAnythingRead() {
        assertNull(PlannerReminderEngine.next(listOf(first), settings, emptyMap(), at("05:30"), suppressed = { true }))
        assertNotNull(next(at("05:31")))
    }
    @Test fun gentleHasNoPreparationAndOnlyOneFollowup() {
        val gentle = settings.copy(intensity = "gentle")
        assertEquals(at("05:30"), next(at("05:00"), config = gentle)!!.at)
        assertNull(next(at("05:50"), mapOf(first.key to ReminderReceipt(2)), gentle))
    }
    @Test fun calculatedSchedulesRequireAConfirmedLocalZoneAndValidCoordinates() {
        val config = settings.copy(source = "calculated", latitude = 6.5244, longitude = 3.3792, zone = zone.id)
        assertTrue(config.valid(PRAYER_NAMES, zone))
        assertFalse(config.copy(zone = "Europe/London").valid(PRAYER_NAMES, zone))
        assertFalse(config.copy(latitude = Double.NaN).valid(PRAYER_NAMES, zone))
        assertTrue(PlannerReminderEngine.prayerTimes(config, date).values.all { it % 60_000 == 0L })
        assertEquals(PlannerReminderEngine.prayerTimes(config, date), PlannerReminderEngine.prayerTimes(config, date))
        val before = portions(config = config, preference = "before").first().deadline
        val after = portions(config = config, preference = "after").first().deadline
        val around = portions(config = config, preference = "split").first().deadline
        assertEquals(30 * 60_000L, after - before); assertEquals(15 * 60_000L, around - before)
        val hour = Instant.ofEpochMilli(before).atZone(zone).hour
        assertTrue("Fajr must be a local morning time", hour in 4..6)
    }
    @Test fun customTimesFollowDaylightSavingRules() {
        val ny = ZoneId.of("America/New_York")
        val p = plan().let { it.copy(assignments = it.assignments.map { a -> a.copy(date = "2026-11-01") }) }
        val now = Instant.parse("2026-11-01T04:00:00Z").toEpochMilli()
        val first = PlannerReminderEngine.portions(p, settings, PRAYER_NAMES, "after", now, ny).first()
        assertEquals(Instant.parse("2026-11-01T10:30:00Z").toEpochMilli(), first.deadline)
    }
}
