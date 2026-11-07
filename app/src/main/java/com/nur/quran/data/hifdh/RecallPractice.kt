package com.nur.quran.data.hifdh

/** Cue level and task identity must survive separately from an FSRS rating. */
enum class RecallTask { AYAH, TRANSITION, PASSAGE, CORRECTION }
enum class RecallCue { NONE, PREVIOUS_AYAH, ANSWER_PROMPT }
data class RecallAssessment(
    val requestedRating: Int,
    val cue: RecallCue = RecallCue.NONE,
    val task: RecallTask = RecallTask.AYAH,
    val durationSeconds: Int = 0
) {
    val schedulerRating: Int get() = if (cue == RecallCue.ANSWER_PROMPT) FsrsRating.AGAIN
        else requestedRating.coerceIn(FsrsRating.AGAIN, FsrsRating.EASY)
    val independent: Boolean get() = schedulerRating != FsrsRating.AGAIN && cue == RecallCue.NONE && task != RecallTask.CORRECTION
}

/** A bounded correction pass, with intervening items when available. Never silently counts as mastery. */
class RecallPractice(keys: List<String>) {
    private val pending = ArrayDeque(keys.distinct())
    private val attempts = mutableMapOf<String, Int>()
    private val corrections = mutableSetOf<String>()
    val unresolved = linkedSetOf<String>()
    var current: String? = pending.removeFirstOrNull(); private set
    var reviewed: Int = 0; private set
    val isCorrection get() = current in corrections
    val finished get() = current == null
    fun grade(rating: Int) {
        val key = current ?: return
        reviewed++
        val count = attempts.getOrDefault(key, 0) + 1
        attempts[key] = count
        if (rating == FsrsRating.AGAIN) {
            unresolved.add(key)
            if (count < 2) { pending.addLast(key); corrections.add(key) }
        } else unresolved.remove(key)
    }
    fun next() { current = pending.removeFirstOrNull() }
}
