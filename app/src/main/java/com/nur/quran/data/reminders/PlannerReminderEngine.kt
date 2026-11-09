package com.nur.quran.data.reminders

import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.nur.quran.data.planner.*
import java.time.*
import kotlin.math.ceil

/** Device-local consent and schedule. Empty times deliberately have no fallback. */
data class ReminderSettings(
    val enabled: Boolean = false,
    val intensity: String = "focused",
    val source: String = "custom",
    val times: Map<String, String> = emptyMap(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val method: String = "MUSLIM_WORLD_LEAGUE",
    val hanafi: Boolean = false,
    val windowMinutes: Int = 30,
    val confirmed: Boolean = false,
    val zone: String = ""
) {
    fun valid(prayers: List<String>, zoneId: ZoneId): Boolean = confirmed &&
        intensity in listOf("gentle", "focused", "strong") && windowMinutes in 10..180 && prayers.isNotEmpty() &&
        if (source == "custom") prayers.all { runCatching { LocalTime.parse(times[it]) }.isSuccess }
        else source == "calculated" && zone == zoneId.id && latitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
            longitude?.let { it.isFinite() && it in -180.0..180.0 } == true &&
            runCatching { CalculationMethod.valueOf(method) }.isSuccess
}

data class ReminderReceipt(val phase: Int = -1, val sentAt: Long = 0, val snoozeUntil: Long = 0)
data class ReminderPortion(val key: String, val planId: String, val day: Int, val prayer: String,
    val deadline: Long, val pages: List<Int>, val seconds: Long?)
data class ReminderEvent(val portion: ReminderPortion, val phase: Int, val at: Long, val snoozed: Boolean = false) {
    val token: String get() = "${portion.key}:$phase:${if (snoozed) at else 0}"
}

object PlannerReminderEngine {
    private const val MINUTE = 60_000L

    fun prayerTimes(settings: ReminderSettings, date: LocalDate): Map<String, Long> {
        val params = CalculationMethod.valueOf(settings.method).parameters.apply {
            madhab = if (settings.hanafi) Madhab.HANAFI else Madhab.SHAFI
        }
        val times = PrayerTimes(Coordinates(settings.latitude!!, settings.longitude!!),
            DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
        return mapOf("Fajr" to times.fajr, "Dhuhr" to times.dhuhr, "Asr" to times.asr,
            "Maghrib" to times.maghrib, "Isha" to times.isha).mapValues { it.value.time / 60_000 * 60_000 }
    }

    fun portions(plan: ReadingPlan?, settings: ReminderSettings, prayers: List<String>, preference: String,
        now: Long, zone: ZoneId, pace: PlannerPagePace? = null): List<ReminderPortion> {
        if (plan == null || plan.archivedAt != null || !settings.enabled || !settings.valid(prayers, zone)) return emptyList()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return plan.assignments.filter { runCatching { LocalDate.parse(it.date) in today.minusDays(1)..today.plusDays(1) }.getOrDefault(false) }
            .flatMap { assignment ->
                val progress = PlannerEngine.getAssignmentProgress(plan, assignment)
                if (progress.isComplete) return@flatMap emptyList()
                val read = plan.assignmentReadPages[assignment.dayNumber].orEmpty().toSet()
                val date = LocalDate.parse(assignment.date)
                val calculated = if (settings.source == "calculated") runCatching { prayerTimes(settings, date) }.getOrDefault(emptyMap()) else emptyMap()
                prayers.mapIndexedNotNull { index, prayer ->
                    val start = ceil(index.toDouble() / prayers.size * assignment.items.size).toInt()
                    val end = ceil((index + 1).toDouble() / prayers.size * assignment.items.size).toInt()
                    val items = assignment.items.subList(start, end)
                    val pages = items.filterNot { it.rangeValue in progress.completedRangeValues }
                        .flatMap { (it.pageStart..it.pageEnd).toList() }.distinct().filterNot { it in read }
                    if (pages.isEmpty()) return@mapIndexedNotNull null
                    val deadline = if (settings.source == "custom") date.atTime(LocalTime.parse(settings.times.getValue(prayer))).atZone(zone).toInstant().toEpochMilli()
                    else calculated[prayer]?.plus(when (preference) {
                        "before" -> 0L
                        "split" -> settings.windowMinutes * MINUTE / 2
                        else -> settings.windowMinutes * MINUTE
                    }) ?: return@mapIndexedNotNull null
                    ReminderPortion("${plan.id}:${assignment.date}:${assignment.dayNumber}:$prayer:$deadline", plan.id,
                        assignment.dayNumber, prayer, deadline, pages, pace?.let { it.secondsPerPage * pages.size })
                }
            }
    }

    /** Collapse missed phases to one, bounded by expiry and a global notification gap. */
    fun next(portions: List<ReminderPortion>, settings: ReminderSettings, receipts: Map<String, ReminderReceipt>,
        now: Long, lastSent: Long = 0, suppressed: (ReminderPortion) -> Boolean = { false }): ReminderEvent? =
        portions.filterNot(suppressed).mapNotNull { portion ->
            val receipt = receipts[portion.key] ?: ReminderReceipt()
            val expiry = portion.deadline + 115 * MINUTE
            if (now > expiry) return@mapNotNull null
            val lead = ((portion.seconds ?: 300) * 1000 + 5 * MINUTE).coerceIn(10 * MINUTE, 120 * MINUTE)
            val phases = if (settings.intensity == "gentle") listOf(1 to portion.deadline, 2 to portion.deadline + 10 * MINUTE)
                else listOf(0 to portion.deadline - lead, 1 to portion.deadline, 2 to portion.deadline + 10 * MINUTE, 3 to portion.deadline + 25 * MINUTE)
            val snoozed = receipt.snoozeUntil > 0
            val eligibleAt = maxOf(now, lastSent.takeIf { it > 0 }?.plus(10 * MINUTE) ?: now,
                receipt.snoozeUntil)
            if (eligibleAt > expiry) return@mapNotNull null
            val eligible = phases.filter { (phase, _) -> phase > receipt.phase || snoozed }
            if (eligible.isEmpty()) return@mapNotNull null
            val phase = eligible.lastOrNull { it.second <= eligibleAt } ?: eligible.first()
            ReminderEvent(portion, phase.first, maxOf(phase.second, eligibleAt), snoozed)
        }.minWithOrNull(compareBy<ReminderEvent> { it.at }.thenBy { it.portion.deadline })
}
