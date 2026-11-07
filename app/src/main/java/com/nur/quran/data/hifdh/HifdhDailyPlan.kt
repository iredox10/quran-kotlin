package com.nur.quran.data.hifdh

import kotlin.math.max

data class HifdhDailyPlan(
    val newKeys: List<String>, val recentKeys: List<String>, val olderKeys: List<String>,
    val remainingReviewCount: Int, val budgetMinutes: Int, val recovery: Boolean,
    val edition: String = "hafs"
) { val reviewKeys get() = (olderKeys + recentKeys).distinct() }

/** Budget in estimated recitation time, protect old material; no ayah-count quota pretending equal lengths. */
fun buildDailyHifdhPlan(
    inventory: List<String>, memorized: Set<String>, history: Map<String, HifdhHistoryEntry>,
    events: List<HifdhReviewEvent>, now: Long, budgetMinutes: Int = 20,
    wordCounts: Map<String, Int> = emptyMap(), newLimit: Int = 3, recentDays: Int = 7
): HifdhDailyPlan {
    val byVerse = events.groupBy { it.verseKey }
    val evidence = byVerse.mapValues { evidenceFor(it.value) }
    val minutes = budgetMinutes.coerceIn(5, 120)
    val repairKeys = unresolvedHifdhMistakes(events).flatMap { listOfNotNull(it.verseKey, it.relatedVerse) }.toSet()
    val allReview = (memorized + repairKeys).sortedWith(compareBy<String> {
        val e = evidence[it]
        when {
            it in repairKeys || e?.mastery == HifdhMastery.REPAIR -> 0
            history[it]?.card?.due?.let { d -> d < now } == true -> 1
            else -> 2
        }
    }.thenBy { evidence[it]?.lastIndependentPass ?: history[it]?.lastReviewed ?: 0L }
        .thenBy { it.substringBefore(":").toIntOrNull() ?: 0 }.thenBy { it.substringAfter(":").toIntOrNull() ?: 0 })
    fun cost(key: String): Int {
        val observed = byVerse[key].orEmpty().filter { it.outcome == "review" && it.durationSeconds in 3..600 && it.task != "CORRECTION" }
            .take(5).map { it.durationSeconds }
        return if (observed.isEmpty()) max(12, (wordCounts[key] ?: 20) * 2) else observed.average().toInt().coerceIn(12, 600)
    }
    val due = allReview.filter { it in repairKeys || isHifdhDue(history[it]?.card, now) }
    val lastPractice = events.filter { it.outcome in setOf("review", "practice", "listening") }.maxOfOrNull { it.timestamp }
    val recovery = (lastPractice != null && now - lastPractice > 3 * 86_400_000L) || due.sumOf(::cost) > minutes * 60
    var remaining = minutes * 60
    val older = mutableListOf<String>()
    val recent = mutableListOf<String>()
    // Always offer at least the oldest untouched portion, even if FSRS did not mark it due.
    val rotation = allReview.minByOrNull { evidence[it]?.lastIndependentPass ?: history[it]?.lastReviewed ?: 0L }
    val candidates = (listOfNotNull(rotation) + due).distinct()
    for (key in candidates) {
        val seconds = cost(key)
        if (seconds > remaining) continue
        val bucket = if (key in repairKeys) HifdhQueue.SABQI else classifyEvidenceQueue(evidence[key] ?: HifdhEvidence(), history[key]?.card, now, recentDays)
        if (bucket == HifdhQueue.MANZIL) older.add(key) else recent.add(key)
        remaining -= seconds
    }
    val new = mutableListOf<String>()
    if (!recovery) for (key in inventory.filterNot { it in memorized || it in repairKeys }.take(newLimit.coerceIn(0, 20))) {
        val seconds = cost(key) * 4 // Acquisition takes longer than a familiar recitation.
        if (seconds > remaining) break
        new.add(key); remaining -= seconds
    }
    return HifdhDailyPlan(new, recent, older, (due - older.toSet() - recent.toSet()).size, minutes, recovery)
}
