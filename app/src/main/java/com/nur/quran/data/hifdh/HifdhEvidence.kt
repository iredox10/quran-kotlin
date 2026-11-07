package com.nur.quran.data.hifdh

import java.time.Instant
import java.time.ZoneId

/** Immutable evidence; a correction repetition never replaces the original failed review. */
data class HifdhReviewEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val verseKey: String,
    val edition: String = "hafs",
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int,
    val cue: String = RecallCue.NONE.name,
    val task: String = RecallTask.AYAH.name,
    val durationSeconds: Int = 0,
    val assessor: String = "self",
    val outcome: String = "review",
    val note: String = "",
    val errorType: String? = null,
    val wordPosition: Int? = null,
    val relatedVerse: String? = null,
    val recordingId: String? = null,
    val relatedEventId: String? = null
) {
    val independentPass get() = rating in 2..4 && cue == RecallCue.NONE.name &&
        task != RecallTask.CORRECTION.name && outcome == "review"
    val isValid get() = runCatching { id.length in 8..100 && Regex("\\d{1,3}:\\d{1,3}").matches(verseKey) &&
        edition == "hafs" && timestamp > 0 && rating in 1..4 && note.length <= 2000 &&
        durationSeconds in 0..86_400 && assessor in setOf("self", "listener", "teacher") &&
        cue in RecallCue.entries.map { it.name } && task in RecallTask.entries.map { it.name } &&
        outcome in setOf("review", "mistake", "resolved", "confusion")
    }.getOrDefault(false)
}

enum class HifdhMastery { IMPORTED, LEARNING, RECALLED, RETAINED, REPAIR }
data class HifdhEvidence(
    val learnedAt: Long? = null, val lastIndependentPass: Long? = null,
    val independentDays: Int = 0, val lastFailure: Long? = null,
    val teacherCheckedAt: Long? = null, val mastery: HifdhMastery = HifdhMastery.IMPORTED
)

/** No invented acquisition dates for legacy checkboxes or latest-only scheduler records. */
fun evidenceFor(events: List<HifdhReviewEvent>, zone: ZoneId = ZoneId.systemDefault()): HifdhEvidence {
    val ordered = events.filter { it.isValid }.distinctBy { it.id }.sortedBy { it.timestamp }
    val passes = ordered.filter { it.independentPass }
    val lastPass = passes.lastOrNull()?.timestamp
    val failure = ordered.lastOrNull { it.outcome == "review" && it.rating == 1 && it.task != RecallTask.CORRECTION.name }?.timestamp
    val days = passes.map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }.distinct().size
    val teacher = ordered.lastOrNull { it.assessor == "teacher" && it.independentPass }?.timestamp
    val state = when {
        failure != null && (lastPass == null || failure >= lastPass) -> HifdhMastery.REPAIR
        days >= 2 && passes.last().timestamp - passes.first().timestamp >= 86_400_000 -> HifdhMastery.RETAINED
        lastPass != null -> HifdhMastery.RECALLED
        ordered.any { it.outcome == "review" } -> HifdhMastery.LEARNING
        else -> HifdhMastery.IMPORTED
    }
    return HifdhEvidence(passes.firstOrNull()?.timestamp, lastPass, days, failure, teacher, state)
}

/** Unknown-age imports stay older revision; looking at a surah does not change acquisition. */
fun classifyEvidenceQueue(evidence: HifdhEvidence, card: FsrsCard?, now: Long, recentDays: Int = 7): HifdhQueue {
    if (evidence.mastery == HifdhMastery.REPAIR || card?.state == FsrsState.RELEARNING) return HifdhQueue.SABQI
    val learned = evidence.learnedAt ?: return if (card?.state in setOf(FsrsState.NEW, FsrsState.LEARNING)) HifdhQueue.SABAQ else HifdhQueue.MANZIL
    val age = (now - learned).coerceAtLeast(0)
    return when {
        age < 86_400_000 -> HifdhQueue.SABAQ
        age < recentDays.coerceIn(1, 60) * 86_400_000L -> HifdhQueue.SABQI
        else -> HifdhQueue.MANZIL
    }
}
