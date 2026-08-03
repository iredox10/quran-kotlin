package com.nur.quran.data.hifdh

/**
 * Kotlin port of ts-fsrs 5.4.1 (FSRS-6.0) with the exact defaults the web app
 * uses (`fsrs({})` in quran-app/src/store/useAppStore.js):
 * request_retention=0.9, maximum_interval=36500, enable_fuzz=false,
 * enable_short_term=true, learning_steps=["1m","10m"], relearning_steps=["10m"].
 *
 * Cards use epoch millis for dates (Gson-friendly) and integer state/rating
 * constants matching the ts-fsrs enums.
 */
object FsrsRating {
    const val AGAIN = 1
    const val HARD = 2
    const val GOOD = 3
    const val EASY = 4
}

object FsrsState {
    const val NEW = 0
    const val LEARNING = 1
    const val REVIEW = 2
    const val RELEARNING = 3
}

/** Serializable memory state — mirrors ts-fsrs `Card` (dates as epoch millis). */
data class FsrsCard(
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Double,
    val scheduledDays: Int,
    val reps: Int,
    val lapses: Int,
    val learningSteps: Int,
    val state: Int,
    val lastReview: Long? = null
) {
    fun isDue(nowMs: Long = System.currentTimeMillis()): Boolean = due <= nowMs
}

/** Entry stored per verse in hifdhHistory — mirrors the web `{ card, lastReviewed, strength }`. */
data class HifdhHistoryEntry(
    val card: FsrsCard? = null,
    val lastReviewed: Long = 0L,
    val strength: String = "medium"
)

/** Mirrors the web `addHifdhGoal({ targetType, targetId, targetDate, ... })`. */
data class HifdhGoal(
    val id: String,
    val targetType: String = "surah",
    val targetId: Int,
    val targetDate: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    val daysLeft: Long
        get() = kotlin.math.max(0, (targetDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000))
}

private const val S_MIN = 1e-3
private const val S_MAX = 36500.0

private fun clamp(v: Double, min: Double, max: Double) = v.coerceIn(min, max)

private fun round8(v: Double) = Math.round(v * 1e8) / 1e8

/** ts-fsrs FSRS-6 default parameters (default_w). */
val FSRS6_DEFAULT_W = doubleArrayOf(
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 1e-3,
    1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
    1.8729, 0.5425, 0.0912, 0.0658, 0.1542
)

/**
 * Port of ts-fsrs `FSRSAlgorithm` + `BasicScheduler` (enable_short_term=true),
 * exposing `createEmptyCard` / `next(card, now, rating)` like the web store.
 */
class FsrsScheduler(
    private val w: DoubleArray = FSRS6_DEFAULT_W,
    private val requestRetention: Double = 0.9,
    private val maximumInterval: Int = 36500
) {

    private val learningSteps = intArrayOf(1, 10) // "1m", "10m"
    private val relearningSteps = intArrayOf(10)  // "10m"

    /** I(r,s) = (r^(1/DECAY) - 1) / FACTOR — the "1.0" with default params. */
    val intervalModifier: Double = calculateIntervalModifier(requestRetention)

    fun createEmptyCard(nowMs: Long = System.currentTimeMillis()) = FsrsCard(
        due = nowMs,
        stability = 0.0,
        difficulty = 0.0,
        elapsedDays = 0.0,
        scheduledDays = 0,
        reps = 0,
        lapses = 0,
        learningSteps = 0,
        state = FsrsState.NEW,
        lastReview = null
    )

    /** R(t,S) = (1 + FACTOR * t / S)^DECAY */
    fun forgettingCurve(elapsedDays: Double, stability: Double): Double {
        val (decay, factor) = computeDecayFactor(w)
        return round8(Math.pow(1 + factor * elapsedDays / stability, decay))
    }

    /**
     * Web: `f.next(card, now, ratingValue)` → `recordLog.card`.
     * Runs the full BasicScheduler flow for the given rating.
     */
    fun next(card: FsrsCard, nowMs: Long, rating: Int): FsrsCard {
        val elapsedDays: Double =
            if (card.state != FsrsState.NEW && card.lastReview != null) {
                (utcDay(nowMs) - utcDay(card.lastReview)).toDouble()
            } else 0.0
        val current = card.copy(
            lastReview = nowMs,
            elapsedDays = elapsedDays,
            reps = card.reps + 1
        )
        return when (card.state) {
            FsrsState.NEW -> newState(current, nowMs, rating, elapsedDays)
            FsrsState.LEARNING, FsrsState.RELEARNING ->
                learningState(current, nowMs, rating, elapsedDays, card.state)
            else -> reviewState(current, nowMs, rating, elapsedDays)
        }
    }

    // ── Scheduler states ────────────────────────────────────────────────

    private fun newState(current: FsrsCard, nowMs: Long, grade: Int, elapsedDays: Double): FsrsCard {
        val next = nextDs(current, 0.0, grade)
        return applyLearningSteps(next, nowMs, grade, FsrsState.LEARNING, elapsedDays)
    }

    private fun learningState(
        current: FsrsCard,
        nowMs: Long,
        grade: Int,
        elapsedDays: Double,
        lastState: Int
    ): FsrsCard {
        val next = nextDs(current, elapsedDays, grade)
        return applyLearningSteps(next, nowMs, grade, lastState, elapsedDays)
    }

    private fun reviewState(current: FsrsCard, nowMs: Long, grade: Int, elapsedDays: Double): FsrsCard {
        val r = forgettingCurve(elapsedDays, current.stability)
        val nextAgain = nextDs(current, elapsedDays, FsrsRating.AGAIN, r)
        val nextHard = nextDs(current, elapsedDays, FsrsRating.HARD, r)
        val nextGood = nextDs(current, elapsedDays, FsrsRating.GOOD, r)
        val nextEasy = nextDs(current, elapsedDays, FsrsRating.EASY, r)

        val hardInterval = Math.min(nextInterval(nextHard.stability), nextInterval(nextGood.stability))
        val goodInterval = Math.max(nextInterval(nextGood.stability), hardInterval + 1)
        val easyInterval = Math.max(nextInterval(nextEasy.stability), goodInterval + 1)

        val hard = nextHard.copy(
            state = FsrsState.REVIEW, learningSteps = 0,
            scheduledDays = hardInterval, due = nowMs + hardInterval * DAY_MS
        )
        val good = nextGood.copy(
            state = FsrsState.REVIEW, learningSteps = 0,
            scheduledDays = goodInterval, due = nowMs + goodInterval * DAY_MS
        )
        val easy = nextEasy.copy(
            state = FsrsState.REVIEW, learningSteps = 0,
            scheduledDays = easyInterval, due = nowMs + easyInterval * DAY_MS
        )
        val again = applyLearningSteps(
            nextAgain.copy(lapses = nextAgain.lapses + 1),
            nowMs, FsrsRating.AGAIN, FsrsState.RELEARNING, elapsedDays
        )

        return when (grade) {
            FsrsRating.AGAIN -> again
            FsrsRating.HARD -> hard
            FsrsRating.GOOD -> good
            else -> easy
        }
    }

    /** next_ds(t, g, r) — recompute difficulty/stability, keep the rest of the card. */
    private fun nextDs(current: FsrsCard, t: Double, g: Int, r: Double? = null): FsrsCard {
        val (nd, ns) = nextState(current.difficulty, current.stability, t, g, r)
        return current.copy(difficulty = nd, stability = ns)
    }

    /** Learning/relearning steps for the given card state and current step index. */
    private fun learningStepsInfo(state: Int, curStep: Int): Map<Int, Pair<Int, Int>> {
        val steps = if (state == FsrsState.RELEARNING || state == FsrsState.REVIEW) relearningSteps else learningSteps
        if (steps.isEmpty() || curStep >= steps.size) return emptyMap()
        val first = steps[0]
        val result = mutableMapOf<Int, Pair<Int, Int>>()
        if (state == FsrsState.REVIEW) {
            // Relearning step for a failed Review card (only Again is scheduled).
            result[FsrsRating.AGAIN] = steps[kotlin.math.max(0, curStep)] to 0
            return result
        }
        result[FsrsRating.AGAIN] = first to 0
        result[FsrsRating.HARD] =
            (if (steps.size == 1) Math.round(first * 1.5).toInt() else Math.round((first + steps[1]) / 2.0).toInt()) to curStep
        val nextInfo = steps.getOrNull(curStep + 1)
        if (nextInfo != null) result[FsrsRating.GOOD] = nextInfo to curStep + 1
        return result
    }

    private fun applyLearningSteps(
        current: FsrsCard,
        nowMs: Long,
        grade: Int,
        toState: Int,
        elapsedDays: Double
    ): FsrsCard {
        val info = learningStepsInfo(current.state, current.learningSteps)
        val scheduledMinutes = kotlin.math.max(0, info[grade]?.first ?: 0)
        val nextSteps = kotlin.math.max(0, info[grade]?.second ?: 0)
        return if (scheduledMinutes > 0 && scheduledMinutes < 1440) {
            current.copy(
                learningSteps = nextSteps,
                scheduledDays = 0,
                state = toState,
                due = nowMs + scheduledMinutes * MINUTE_MS
            )
        } else if (scheduledMinutes >= 1440) {
            current.copy(
                state = FsrsState.REVIEW,
                learningSteps = nextSteps,
                due = nowMs + scheduledMinutes * MINUTE_MS,
                scheduledDays = scheduledMinutes / 1440
            )
        } else {
            val interval = nextInterval(current.stability, elapsedDays)
            current.copy(
                state = FsrsState.REVIEW,
                learningSteps = 0,
                scheduledDays = interval,
                due = nowMs + interval * DAY_MS
            )
        }
    }

    // ── Algorithm (FSRS-6) ──────────────────────────────────────────────

    /** S0(G) = max(w[G-1], 0.1) */
    fun initStability(g: Int): Double = Math.max(w[g - 1], 0.1)

    /** D0(G) = w[4] - e^((G-1)*w[5]) + 1 */
    fun initDifficulty(g: Int): Double = round8(w[4] - Math.exp((g - 1) * w[5]) + 1)

    /** I = min(max(1, round(S * intervalModifier)), maximumInterval); fuzz disabled. */
    fun nextInterval(s: Double, elapsedDays: Double = 0.0): Int {
        return Math.min(Math.max(1, Math.round(s * intervalModifier).toInt()), maximumInterval)
    }

    private fun linearDamping(deltaD: Double, oldD: Double): Double = round8(deltaD * (10 - oldD) / 9)

    private fun meanReversion(init: Double, current: Double): Double =
        round8(w[7] * init + (1 - w[7]) * current)

    /** next_D = mean_reversion(D0(Easy), D + linear_damping(-w[6]*(G-3), D)), clamped [1,10] */
    fun nextDifficulty(d: Double, g: Int): Double {
        val deltaD = -w[6] * (g - 3)
        val nextD = d + linearDamping(deltaD, d)
        return clamp(meanReversion(initDifficulty(FsrsRating.EASY), nextD), 1.0, 10.0)
    }

    /** S'r(D,S,R,G) — stability after successful recall. */
    fun nextRecallStability(d: Double, s: Double, r: Double, g: Int): Double {
        val hardPenalty = if (g == FsrsRating.HARD) w[15] else 1.0
        val easyBound = if (g == FsrsRating.EASY) w[16] else 1.0
        return round8(clamp(
            s * (1 + Math.exp(w[8]) * (11 - d) * Math.pow(s, -w[9]) *
                (Math.exp((1 - r) * w[10]) - 1) * hardPenalty * easyBound),
            S_MIN, S_MAX
        ))
    }

    /** S'f(D,S,R) — stability after forgetting. */
    fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        return round8(clamp(
            w[11] * Math.pow(d, -w[12]) * (Math.pow(s + 1, w[13]) - 1) * Math.exp((1 - r) * w[14]),
            S_MIN, S_MAX
        ))
    }

    /** S's(S,G) — short-term stability (learning steps, enable_short_term=true). */
    fun nextShortTermStability(s: Double, g: Int): Double {
        val sinc = Math.pow(s, -w[19]) * Math.exp(w[17] * (g - 3 + w[18]))
        val maskedSinc = if (g >= FsrsRating.HARD) Math.max(sinc, 1.0) else sinc
        return round8(clamp(s * maskedSinc, S_MIN, S_MAX))
    }

    /** next_state({d,s}, t, g, r) — returns (newDifficulty, newStability). */
    private fun nextState(d: Double, s: Double, t: Double, g: Int, r: Double? = null): Pair<Double, Double> {
        if (d == 0.0 && s == 0.0) {
            return clamp(initDifficulty(g), 1.0, 10.0) to initStability(g)
        }
        val rr = r ?: forgettingCurve(t, s)
        val newS: Double = if (t == 0.0) {
            nextShortTermStability(s, g)
        } else if (g == FsrsRating.AGAIN) {
            val sAfterFail = nextForgetStability(d, s, rr)
            val nextSMin = s / Math.exp(w[17] * w[18])
            clamp(round8(nextSMin), S_MIN, sAfterFail)
        } else {
            nextRecallStability(d, s, rr, g)
        }
        return nextDifficulty(d, g) to newS
    }

    private fun calculateIntervalModifier(requestRetention: Double): Double {
        val (decay, factor) = computeDecayFactor(w)
        return round8((Math.pow(requestRetention, 1.0 / decay) - 1) / factor)
    }

    private fun computeDecayFactor(w: DoubleArray): Pair<Double, Double> {
        val decay = -w[20]
        val factor = round8(Math.exp(Math.pow(decay, -1.0) * Math.log(0.9)) - 1)
        return decay to factor
    }

    /** Days since epoch (UTC calendar days) — matches JS Date.UTC normalization. */
    private fun utcDay(ms: Long): Long = ms / DAY_MS

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val MINUTE_MS = 60_000L
    }
}
