package com.nur.quran.data.planner

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nur.quran.data.HIZB_STARTS
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.PAGE_GROUPS
import com.nur.quran.data.db.entities.ChapterEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class PlannerUnit(val label: String, val plural: String, val max: Int)

val PLANNER_UNITS = mapOf(
    "page" to PlannerUnit("Page", "Pages", 604),
    "juz" to PlannerUnit("Juz", "Ajza", 30),
    "hizb" to PlannerUnit("Hizb", "Ahzab", 60),
    "surah" to PlannerUnit("Surah", "Surahs", 114)
)

data class PlanTemplate(
    val id: String,
    val title: String,
    val durationDays: Int,
    val unitType: String,
    val startUnit: Int,
    val endUnit: Int,
    val description: String,
    val tags: List<String> = emptyList()
)

val PLAN_TEMPLATES = listOf(
    PlanTemplate("ramadan-last-10", "Ramadan Last 10", 10, "page", 542, 604, "Complete the last 3 Ajza in the last 10 days", listOf("Ramadan")),
    PlanTemplate("juz-amma", "Juz Amma Focus", 15, "page", 582, 604, "Take 15 days to master the 30th Juz", listOf("Juz 30")),
    PlanTemplate("al-kahf", "Surah Al-Kahf", 1, "surah", 18, 18, "The recommended Friday reading — a single-day focus", listOf("Friday")),
    PlanTemplate("tafsir-deep-dive", "Tafsir Deep Dive", 114, "surah", 1, 114, "One Surah per day — a 114-day deep dive through the Quran", listOf("Study")),
    PlanTemplate("daily-juz", "Daily Juz", 30, "juz", 1, 30, "One Juz per day — complete the Quran in a month", listOf("Juz")),
    PlanTemplate("quick-revision", "Quick Revision", 10, "juz", 1, 30, "Full Quran in 10 days for intense revision", listOf("Review"))
)

data class PlannerItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val route: String,
    val rangeValue: String, // String to handle single numbers or ranges like "1-5"
    val pageStart: Int,
    val pageEnd: Int
)

data class PlannerAssignment(
    val dayNumber: Int,
    var date: String, // YYYY-MM-DD
    val unitType: String,
    val title: String,
    val subtitle: String,
    val startUnit: Int,
    val endUnit: Int,
    var primaryRoute: String,
    val pageStart: Int,
    val pageEnd: Int,
    var items: List<PlannerItem>
)

data class ReadingPlan(
    val id: String,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
    val unitType: String,
    val title: String,
    val durationDays: Int,
    val startDate: String, // YYYY-MM-DD
    val startUnit: Int,
    val endUnit: Int,
    val isCustomRange: Boolean = false,
    val assignmentProgress: Map<Int, Int> = emptyMap(), // dayNumber -> completed item count
    val assignmentReadPages: Map<Int, List<Int>> = emptyMap(), // dayNumber -> read page list
    val assignmentCompletedItems: Map<Int, List<String>> = emptyMap(), // dayNumber -> rangeValue list
    val assignmentCompletedAt: Map<Int, String> = emptyMap(), // dayNumber -> date
    val completedDays: List<Int> = emptyList(),
    val assignments: List<PlannerAssignment>,
    val excludeDays: List<Int> = emptyList(), // Day of week to skip: 0=Sun, 1=Mon, ..., 6=Sat
    val assignmentReflections: Map<Int, String> = emptyMap(), // dayNumber -> reflection text
    val lastReadPage: Int? = null,
    val lastReadVerseKey: String? = null
)

/** Web: plannerBookmarks[planId] entry — a verse highlighted from the reader. */
data class PlannerBookmark(
    val verseKey: String,
    val surahName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Web: groupContiguousPages entry — one contiguous page range { pStart, pEnd }. */
data class ContiguousPageGroup(
    val pStart: Int,
    val pEnd: Int
)

data class AssignmentProgress(
    val completedCount: Int,
    val totalCount: Int,
    val readPagesCount: Int,
    val totalPagesCount: Int,
    val remainingCount: Int,
    val completionRatio: Float,
    val isComplete: Boolean,
    val completedAt: String?,
    val completedRangeValues: List<String>,
    val nextItem: PlannerItem?
)

data class PlannerOverview(
    val completedCount: Int,
    val remainingCount: Int,
    val currentDayNumber: Int,
    val overdueDays: Int,
    val isUpcoming: Boolean,
    val isFinishedWindow: Boolean,
    val completionRatio: Float,
    val firstIncomplete: PlannerAssignment?
)

data class PlannerSuccessMetrics(
    val completedCount: Int,
    val onTimeCount: Int,
    val lateCount: Int,
    val dueCount: Int,
    val dueCompletedCount: Int,
    val successRate: Int,
    val consistencyStreak: Int
)

object PlannerEngine {

    /**
     * Gson allocates Kotlin data classes via Unsafe, so JSON keys absent from
     * legacy cached plans land as null in non-null fields (Kotlin defaults are
     * skipped). Any later copy()/access then throws — e.g. opening Planner
     * auto-rebalances and crashed on missing assignmentReflections.
     * Backfill every defaulted collection/scalar before parsing.
     */
    private val PLAN_MAP_KEYS = setOf(
        "assignmentProgress", "assignmentReadPages", "assignmentCompletedItems",
        "assignmentCompletedAt", "assignmentReflections"
    )
    private val PLAN_LIST_KEYS = setOf("completedDays", "excludeDays")

    private fun backfillPlan(obj: JsonObject): JsonObject {
        for (key in PLAN_MAP_KEYS) {
            if (!obj.has(key) || obj.get(key).isJsonNull) obj.add(key, JsonObject())
        }
        for (key in PLAN_LIST_KEYS) {
            if (!obj.has(key) || obj.get(key).isJsonNull) {
                obj.add(key, com.google.gson.JsonArray())
            }
        }
        if (!obj.has("isCustomRange") || obj.get("isCustomRange").isJsonNull) {
            obj.addProperty("isCustomRange", false)
        }
        if (!obj.has("createdAt") || obj.get("createdAt").isJsonNull) {
            val start = obj.get("startDate")?.takeIf { it.isJsonPrimitive }?.asString
            obj.addProperty("createdAt", (start ?: "1970-01-01") + "T00:00:00Z")
        }
        return obj
    }

    /** Parse one cached plan, tolerating legacy JSON missing newer keys. */
    fun parseReadingPlan(json: String, gson: Gson): ReadingPlan? {
        return try {
            val el = JsonParser.parseString(json)
            if (!el.isJsonObject) return null
            gson.fromJson(backfillPlan(el.asJsonObject), ReadingPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Parse a cached plan list, dropping entries that still fail. */
    fun parseReadingPlans(json: String, gson: Gson): List<ReadingPlan> {
        return try {
            val el = JsonParser.parseString(json)
            if (!el.isJsonArray) return emptyList()
            el.asJsonArray.mapNotNull { item ->
                try {
                    if (!item.isJsonObject) null
                    else gson.fromJson(backfillPlan(item.asJsonObject), ReadingPlan::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatPlannerDate(date: Date = Date()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    fun parsePlannerDate(dateStr: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun addDays(dateStr: String, days: Int): String {
        val cal = Calendar.getInstance().apply {
            time = parsePlannerDate(dateStr)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return formatPlannerDate(cal.time)
    }

    fun diffDays(startDateStr: String, endDateStr: String): Int {
        val start = parsePlannerDate(startDateStr).time
        val end = parsePlannerDate(endDateStr).time
        return floor((end - start).toDouble() / 86400000.0).toInt()
    }

    /**
     * Web: groupContiguousPages (planner.js:306-324, tested) — dedupe, sort,
     * and fold a page list into contiguous { pStart, pEnd } groups.
     * Pure function, unit-testable.
     */
    fun groupContiguousPages(pages: List<Int>): List<ContiguousPageGroup> {
        if (pages.isEmpty()) return emptyList()
        val sorted = pages.toSet().sorted()
        val groups = mutableListOf<ContiguousPageGroup>()
        var pStart = sorted[0]
        var pEnd = sorted[0]
        for (i in 1 until sorted.size) {
            if (sorted[i] == pEnd + 1) {
                pEnd = sorted[i]
            } else {
                groups.add(ContiguousPageGroup(pStart, pEnd))
                pStart = sorted[i]
                pEnd = sorted[i]
            }
        }
        groups.add(ContiguousPageGroup(pStart, pEnd))
        return groups
    }

    fun getReadingDate(startDateStr: String, index: Int, excludeDays: List<Int> = emptyList()): String {
        val cal = Calendar.getInstance().apply {
            time = parsePlannerDate(startDateStr)
        }

        // Adjust day of week format: Calendar.SUNDAY=1 -> 0, MONDAY=2 -> 1, ..., SATURDAY=7 -> 6
        fun getDayIdx(c: Calendar): Int = c.get(Calendar.DAY_OF_WEEK) - 1

        while (excludeDays.contains(getDayIdx(cal))) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        var daysFound = 0
        while (daysFound < index) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            if (!excludeDays.contains(getDayIdx(cal))) {
                daysFound++
            }
        }

        return formatPlannerDate(cal.time)
    }

    fun buildUnitSource(unitType: String, chapters: List<ChapterEntity>): List<PlannerItem> {
        return when (unitType) {
            "page" -> PAGE_GROUPS.map { item ->
                PlannerItem(
                    id = item.id,
                    title = "Page ${item.pageNumber}",
                    subtitle = "Mushaf page view",
                    route = "/page/${item.pageNumber}",
                    rangeValue = item.pageNumber.toString(),
                    pageStart = item.pageNumber,
                    pageEnd = item.pageNumber
                )
            }
            "juz" -> JUZ_STARTS.mapIndexed { index, item ->
                val nextItem = JUZ_STARTS.getOrNull(index + 1)
                val pageStart = item.pageNumber
                val pageEnd = if (nextItem != null) nextItem.pageNumber - 1 else 604
                PlannerItem(
                    id = item.id,
                    title = "Juz ${item.id}",
                    subtitle = "Starts at ${item.verseKey}",
                    route = "/page/${item.pageNumber}",
                    rangeValue = item.id.toString(),
                    pageStart = pageStart,
                    pageEnd = pageEnd
                )
            }
            "hizb" -> HIZB_STARTS.mapIndexed { index, item ->
                val nextItem = HIZB_STARTS.getOrNull(index + 1)
                val pageStart = item.pageNumber
                val pageEnd = if (nextItem != null) nextItem.pageNumber - 1 else 604
                PlannerItem(
                    id = item.id,
                    title = "Hizb ${item.id}",
                    subtitle = "Starts at ${item.verseKey}",
                    route = "/page/${item.pageNumber}",
                    rangeValue = item.id.toString(),
                    pageStart = pageStart,
                    pageEnd = pageEnd
                )
            }
            else -> chapters.map { chapter ->
                val pStart = chapter.pagesStart
                val pEnd = chapter.pagesEnd
                PlannerItem(
                    id = chapter.id,
                    title = chapter.nameSimple,
                    subtitle = chapter.translatedName,
                    route = "/surah/${chapter.id}",
                    rangeValue = chapter.id.toString(),
                    pageStart = pStart,
                    pageEnd = pEnd
                )
            }
        }
    }

    private fun splitIntoChunks(items: List<PlannerItem>, chunkCount: Int): List<List<PlannerItem>> {
        val chunks = mutableListOf<List<PlannerItem>>()
        var cursor = 0
        val baseSize = items.size / chunkCount
        val remainder = items.size % chunkCount

        for (index in 0 until chunkCount) {
            val chunkSize = baseSize + (if (index < remainder) 1 else 0)
            if (chunkSize <= 0) break
            chunks.add(items.subList(cursor, cursor + chunkSize))
            cursor += chunkSize
        }
        return chunks
    }

    private fun buildAssignmentTitle(unitType: String, items: List<PlannerItem>): String {
        val first = items.first()
        val last = items.last()

        if (unitType == "surah") {
            if (items.size == 1) return first.title
            return "Surah ${first.id}-${last.id}"
        }

        val unit = PLANNER_UNITS[unitType] ?: PlannerUnit("Page", "Pages", 604)
        if (first.rangeValue == last.rangeValue) {
            return "${unit.label} ${first.rangeValue}"
        }
        return "${unit.plural} ${first.rangeValue}-${last.rangeValue}"
    }

    private fun buildAssignmentSubtitle(unitType: String, items: List<PlannerItem>): String {
        val first = items.first()
        val last = items.last()

        if (unitType == "surah") {
            if (items.size == 1) return "${first.subtitle} · ${first.title}"
            return "${first.title} to ${last.title}"
        }

        if (items.size == 1) return first.subtitle
        return "${first.subtitle} · ${last.subtitle}"
    }

    private fun buildPlanTitle(unitType: String, items: List<PlannerItem>, customTitle: String?): String {
        if (!customTitle.isNullOrBlank()) return customTitle.trim()

        val first = items.first()
        val last = items.last()
        val unit = PLANNER_UNITS[unitType] ?: PlannerUnit("Page", "Pages", 604)

        if (first.rangeValue == last.rangeValue) {
            return "${unit.label} ${first.rangeValue} Plan"
        }
        return "${unit.plural} ${first.rangeValue}-${last.rangeValue} Plan"
    }

    fun buildReadingPlanner(
        unitType: String,
        durationDays: Int,
        startDate: String,
        startUnit: Int,
        endUnit: Int,
        customTitle: String? = null,
        excludeDays: List<Int> = emptyList(),
        chapters: List<ChapterEntity>
    ): ReadingPlan {
        val sourceItems = buildUnitSource(unitType, chapters)
        val unitMeta = PLANNER_UNITS[unitType] ?: throw IllegalArgumentException("Unsupported unit")

        val normStart = max(1, min(startUnit, unitMeta.max))
        val normEnd = max(normStart, min(endUnit, unitMeta.max))
        val scopedItems = sourceItems.filter {
            val v = it.rangeValue.toIntOrNull() ?: 1
            v in normStart..normEnd
        }

        if (scopedItems.isEmpty()) throw IllegalArgumentException("Planner scope is empty")

        val safeDuration = max(1, min(durationDays, scopedItems.size))
        val chunks = splitIntoChunks(scopedItems, safeDuration)
        val planId = "plan-${System.currentTimeMillis()}"

        val assignments = chunks.mapIndexed { index, items ->
            val pStart = items.map { it.pageStart }.minOrNull() ?: 1
            val pEnd = items.map { it.pageEnd }.maxOrNull() ?: 1
            PlannerAssignment(
                dayNumber = index + 1,
                date = getReadingDate(startDate, index, excludeDays),
                unitType = unitType,
                title = buildAssignmentTitle(unitType, items),
                subtitle = buildAssignmentSubtitle(unitType, items),
                startUnit = items.first().rangeValue.toIntOrNull() ?: 1,
                endUnit = items.last().rangeValue.toIntOrNull() ?: 1,
                primaryRoute = items.first().route,
                pageStart = pStart,
                pageEnd = pEnd,
                items = items
            )
        }

        return ReadingPlan(
            id = planId,
            unitType = unitType,
            title = buildPlanTitle(unitType, scopedItems, customTitle),
            durationDays = assignments.size,
            startDate = startDate,
            startUnit = normStart,
            endUnit = normEnd,
            isCustomRange = normStart != 1 || normEnd != unitMeta.max || !customTitle.isNullOrBlank(),
            assignments = assignments,
            excludeDays = excludeDays
        )
    }

    fun getAssignmentProgress(plan: ReadingPlan, assignment: PlannerAssignment): AssignmentProgress {
        val totalCount = assignment.items.size
        val explicitCompleted = plan.assignmentCompletedItems[assignment.dayNumber]
        val rawCompletedCount = plan.assignmentProgress[assignment.dayNumber] ?: 0

        val initialCompletedRangeValues = if (explicitCompleted != null) {
            assignment.items.filter { explicitCompleted.contains(it.rangeValue) }.map { it.rangeValue }
        } else {
            assignment.items.take(max(0, min(rawCompletedCount, totalCount))).map { it.rangeValue }
        }

        val completedRangeValuesSet = initialCompletedRangeValues.toMutableSet()
        val explicitReadPages = plan.assignmentReadPages[assignment.dayNumber] ?: emptyList()

        var totalPagesCount = 0
        val allReadPages = explicitReadPages.toMutableSet()

        assignment.items.forEach { item ->
            // Web parity (planner.js:670-671): `item.pageStart || 1`,
            // `item.pageEnd || pStart` — 0 counts as missing.
            val pStart = item.pageStart.takeIf { it != 0 } ?: 1
            val pEnd = item.pageEnd.takeIf { it != 0 } ?: pStart
            totalPagesCount += (pEnd - pStart + 1)

            var allItemPagesRead = true
            for (p in pStart..pEnd) {
                if (!explicitReadPages.contains(p)) {
                    allItemPagesRead = false
                    break
                }
            }

            if (allItemPagesRead) {
                completedRangeValuesSet.add(item.rangeValue)
            }

            if (completedRangeValuesSet.contains(item.rangeValue)) {
                for (p in pStart..pEnd) {
                    allReadPages.add(p)
                }
            }
        }

        val completedRangeValues = completedRangeValuesSet.toList()
        val completedCount = completedRangeValues.size
        val nextItem = assignment.items.find { !completedRangeValues.contains(it.rangeValue) } ?: assignment.items.lastOrNull()

        return AssignmentProgress(
            completedCount = completedCount,
            totalCount = totalCount,
            readPagesCount = allReadPages.size,
            totalPagesCount = totalPagesCount,
            remainingCount = max(totalCount - completedCount, 0),
            completionRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
            isComplete = totalCount > 0 && completedCount >= totalCount,
            completedAt = plan.assignmentCompletedAt[assignment.dayNumber],
            completedRangeValues = completedRangeValues,
            nextItem = nextItem
        )
    }

    fun getPlannerOverview(plan: ReadingPlan?, today: String = formatPlannerDate()): PlannerOverview? {
        if (plan == null) return null
        // Web parity (planner.js:621): empty plan has no last assignment —
        // web yields undefined instead of throwing on .last().
        if (plan.assignments.isEmpty()) return PlannerOverview(
            completedCount = 0,
            remainingCount = 0,
            currentDayNumber = 0,
            overdueDays = 0,
            isUpcoming = diffDays(plan.startDate, today) < 0,
            isFinishedWindow = diffDays(plan.startDate, today) >= plan.durationDays,
            completionRatio = 0f,
            firstIncomplete = null
        )

        val elapsedDays = diffDays(plan.startDate, today)
        val currentDayNumber = min(max(elapsedDays + 1, 1), plan.durationDays)
        val completedCount = plan.assignments.count { getAssignmentProgress(plan, it).isComplete }
        val remainingCount = max(plan.durationDays - completedCount, 0)
        val firstIncomplete = plan.assignments.find { !getAssignmentProgress(plan, it).isComplete } ?: plan.assignments.last()
        val overdueDays = plan.assignments.count { it.date < today && !getAssignmentProgress(plan, it).isComplete }

        var totalPages = 0
        var readPages = 0
        plan.assignments.forEach { a ->
            val prog = getAssignmentProgress(plan, a)
            // Web parity (planner.js:609): `prog?.totalPagesCount || 1`.
            totalPages += prog.totalPagesCount.takeIf { it != 0 } ?: 1
            readPages += prog.readPagesCount
        }

        return PlannerOverview(
            completedCount = completedCount,
            remainingCount = remainingCount,
            currentDayNumber = currentDayNumber,
            overdueDays = overdueDays,
            isUpcoming = elapsedDays < 0,
            isFinishedWindow = elapsedDays >= plan.durationDays,
            completionRatio = if (totalPages > 0) readPages.toFloat() / totalPages else 0f,
            firstIncomplete = firstIncomplete
        )
    }

    fun getPlannerSuccessMetrics(plan: ReadingPlan?, today: String = formatPlannerDate()): PlannerSuccessMetrics? {
        if (plan == null) return null

        val completedAssignments = plan.assignments.filter { getAssignmentProgress(plan, it).isComplete }
        val onTimeCount = completedAssignments.count { assignment ->
            val completedAt = plan.assignmentCompletedAt[assignment.dayNumber]
            completedAt != null && completedAt <= assignment.date
        }
        val lateCount = max(completedAssignments.size - onTimeCount, 0)
        val dueAssignments = plan.assignments.filter { it.date <= today }
        val dueCompletedCount = dueAssignments.count { getAssignmentProgress(plan, it).isComplete }
        val successRate = if (dueAssignments.isNotEmpty()) Math.round((onTimeCount.toFloat() / dueAssignments.size) * 100) else 0

        var consistencyStreak = 0
        for (assignment in dueAssignments) {
            val completedAt = plan.assignmentCompletedAt[assignment.dayNumber]
            if (completedAt != null && completedAt <= assignment.date) {
                consistencyStreak++
                continue
            }
            break
        }

        return PlannerSuccessMetrics(
            completedCount = completedAssignments.size,
            onTimeCount = onTimeCount,
            lateCount = lateCount,
            dueCount = dueAssignments.size,
            dueCompletedCount = dueCompletedCount,
            successRate = successRate,
            consistencyStreak = consistencyStreak
        )
    }

    // Web parity: planner.js splitAssignmentReadPages — per-item page split into
    // flat read/unread page lists. String-vs-Number safe: both sides normalized
    // via toString() (web chunk items use numeric rangeValue).
    fun splitAssignmentReadPages(
        plan: ReadingPlan,
        assignment: PlannerAssignment
    ): Pair<List<Int>, List<Int>> {
        val prog = getAssignmentProgress(plan, assignment)
        val explicitReadPages = plan.assignmentReadPages[assignment.dayNumber] ?: emptyList()
        val completedSet = prog.completedRangeValues.map { it.toString() }.toSet()
        val readPages = mutableListOf<Int>()
        val unreadPages = mutableListOf<Int>()

        assignment.items.forEach { item ->
            // Web parity (planner.js:339-340): item bounds only, no
            // assignment.pageStart fallback; 0 counts as missing.
            val pStart = item.pageStart.takeIf { it != 0 } ?: 1
            val pEnd = item.pageEnd.takeIf { it != 0 } ?: pStart
            for (p in pStart..pEnd) {
                if (explicitReadPages.contains(p) || completedSet.contains(item.rangeValue.toString())) {
                    readPages.add(p)
                } else {
                    unreadPages.add(p)
                }
            }
        }

        return readPages to unreadPages
    }

    private fun dayOfWeekIdx(dateStr: String): Int {
        val cal = Calendar.getInstance().apply { time = parsePlannerDate(dateStr) }
        return cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday=0 .. Saturday=6
    }

    fun rebalancePlanner(
        planner: ReadingPlan,
        strategy: String, // "spread", "extend", "custom_pace"
        customDurationDays: Int? = null
    ): ReadingPlan {
        val today = formatPlannerDate()
        val excludeDays = planner.excludeDays

        // Web parity: split each incomplete assignment into read/unread pages.
        // Completed days carry over untouched; partially-read days keep only the
        // read portions (grouped contiguous) as preserved assignments; unread
        // pages pool item-by-item (fixes surah/juz over-pooling from expanding
        // assignment.pageStart..pageEnd which spans whole chapters).
        val preservedAssignments = mutableListOf<PlannerAssignment>()
        val unreadPagePool = mutableListOf<Int>()

        planner.assignments.forEach { a ->
            val prog = getAssignmentProgress(planner, a)
            if (prog.isComplete) {
                preservedAssignments.add(a)
                return@forEach
            }

            val (readPages, unreadPages) = splitAssignmentReadPages(planner, a)

            if (readPages.isNotEmpty() && unreadPages.isNotEmpty()) {
                val readGroups = groupContiguousPages(readPages)
                val preservedItems = readGroups.map { grp ->
                    PlannerItem(
                        id = "${grp.pStart}-${grp.pEnd}".hashCode(),
                        title = "Pages ${grp.pStart}-${grp.pEnd}",
                        subtitle = "${grp.pEnd - grp.pStart + 1} pages",
                        route = "/planner/read/${a.dayNumber}",
                        rangeValue = "${grp.pStart}-${grp.pEnd}",
                        pageStart = grp.pStart,
                        pageEnd = grp.pEnd
                    )
                }
                val aStart = readGroups.first().pStart
                val aEnd = readGroups.last().pEnd
                preservedAssignments.add(
                    a.copy(
                        unitType = "page",
                        title = "Pages $aStart-$aEnd",
                        subtitle = "${readPages.size} pages (Read)",
                        startUnit = aStart,
                        endUnit = aEnd,
                        pageStart = aStart,
                        pageEnd = aEnd,
                        items = preservedItems
                    )
                )
                unreadPagePool.addAll(unreadPages)
            } else if (readPages.isNotEmpty()) {
                preservedAssignments.add(a)
            } else {
                unreadPagePool.addAll(unreadPages)
            }
        }

        if (unreadPagePool.isEmpty()) return planner

        // Web parity guard: a custom total shorter than already-completed days
        // is impossible — refuse rather than dump the pool into one day.
        if (strategy == "custom_pace" && customDurationDays != null &&
            customDurationDays < preservedAssignments.size + 1
        ) {
            return planner
        }

        fun chunkToAssignment(chunk: List<Int>): PlannerAssignment {
            val pStart = chunk.first()
            val pEnd = chunk.last()
            return PlannerAssignment(
                dayNumber = 0,
                date = "",
                unitType = "page",
                title = "Pages $pStart-$pEnd",
                subtitle = "${chunk.size} pages",
                startUnit = pStart,
                endUnit = pEnd,
                primaryRoute = "",
                pageStart = pStart,
                pageEnd = pEnd,
                items = chunk.map { p ->
                    PlannerItem(
                        id = p,
                        title = "Page $p",
                        subtitle = "1 page",
                        route = "",
                        rangeValue = p.toString(),
                        pageStart = p,
                        pageEnd = p
                    )
                }
            )
        }

        val newChunkAssignments = mutableListOf<PlannerAssignment>()

        if (strategy == "extend" || strategy == "custom_pace") {
            val chunkSize = if (strategy == "custom_pace" && customDurationDays != null) {
                val remainingNewDays = customDurationDays - preservedAssignments.size
                ceil(unreadPagePool.size.toDouble() / remainingNewDays).toInt()
            } else {
                val totalDayPages = planner.assignments.sumOf { a ->
                    a.items.sumOf { (it.pageEnd - it.pageStart) + 1 }
                }
                max(ceil(totalDayPages.toDouble() / planner.assignments.size).toInt(), 1)
            }
            var i = 0
            while (i < unreadPagePool.size) {
                val chunk = unreadPagePool.subList(i, min(i + chunkSize, unreadPagePool.size))
                newChunkAssignments.add(chunkToAssignment(chunk.toList()))
                i += chunkSize
            }
        } else if (strategy == "spread") {
            if (planner.assignments.isEmpty()) return planner
            val originalEndDate = planner.assignments.last().date
            if (today > originalEndDate) {
                return rebalancePlanner(planner, "extend")
            }
            var remainingDays = 0
            var curr = today
            while (curr <= originalEndDate) {
                if (!excludeDays.contains(dayOfWeekIdx(curr))) remainingDays++
                curr = addDays(curr, 1)
            }
            if (remainingDays <= 0) remainingDays = 1
            val pagesPerDay = ceil(unreadPagePool.size.toDouble() / remainingDays).toInt()
            for (idx in 0 until remainingDays) {
                // Web parity: JS slice() clamps out-of-range to [] (then break);
                // Kotlin subList() throws when fromIndex > size, which happens
                // once remaining days outnumber unread pages.
                val from = idx * pagesPerDay
                if (from >= unreadPagePool.size) break
                val chunk = unreadPagePool.subList(
                    from, min((idx + 1) * pagesPerDay, unreadPagePool.size)
                )
                if (chunk.isEmpty()) break
                newChunkAssignments.add(chunkToAssignment(chunk.toList()))
            }
        } else {
            return planner
        }

        val mergedRaw = preservedAssignments + newChunkAssignments
        var lastPreservedDate: String? = null
        preservedAssignments.forEach { a ->
            if (lastPreservedDate == null || a.date > lastPreservedDate!!) {
                lastPreservedDate = a.date
            }
        }

        var hasPartialToday = false
        planner.assignments.forEach { a ->
            val prog = getAssignmentProgress(planner, a)
            if (prog.isComplete) return@forEach
            val (readPages, unreadPages) = splitAssignmentReadPages(planner, a)
            if (readPages.isNotEmpty() && unreadPages.isNotEmpty() && a.date == today) {
                hasPartialToday = true
            }
        }

        var newStartDate = today
        if (strategy == "extend" || strategy == "custom_pace") {
            if (lastPreservedDate != null) {
                newStartDate = addDays(lastPreservedDate!!, 1)
                if (hasPartialToday) newStartDate = today
                if (newStartDate < today) newStartDate = today
            }
        }

        val newAssignmentProgress = mutableMapOf<Int, Int>()
        val newAssignmentReadPages = mutableMapOf<Int, List<Int>>()
        val newAssignmentCompletedItems = mutableMapOf<Int, List<String>>()
        val newAssignmentCompletedAt = mutableMapOf<Int, String>()
        val newCompletedDays = mutableListOf<Int>()

        var nextDayNumber = 1
        var newDayIndex = 0
        val preservedCount = preservedAssignments.size

        val finalAssignments = mergedRaw.mapIndexed { index, a ->
            val dn = nextDayNumber
            val oldDayNumber = a.dayNumber.takeIf { it > 0 }
            val isPreserved = index < preservedCount
            var mapped = a.copy(
                dayNumber = dn,
                primaryRoute = "/planner/read/$dn",
                items = a.items.map { it.copy(route = "/planner/read/$dn") }
            )
            if (isPreserved && oldDayNumber != null) {
                planner.assignmentProgress[oldDayNumber]?.let { newAssignmentProgress[dn] = it }
                planner.assignmentReadPages[oldDayNumber]?.let { newAssignmentReadPages[dn] = it }
                planner.assignmentCompletedItems[oldDayNumber]?.let {
                    newAssignmentCompletedItems[dn] = it
                }
                planner.assignmentCompletedAt[oldDayNumber]?.let { newAssignmentCompletedAt[dn] = it }
                if (planner.completedDays.contains(oldDayNumber)) {
                    newCompletedDays.add(dn)
                }
                if (!planner.completedDays.contains(oldDayNumber)) {
                    newAssignmentProgress[dn] = mapped.items.size
                    newAssignmentCompletedItems[dn] = mapped.items.map { it.rangeValue }
                    newAssignmentCompletedAt[dn] = today
                    newCompletedDays.add(dn)
                }
            } else {
                mapped = mapped.copy(date = getReadingDate(newStartDate, newDayIndex, excludeDays))
                newDayIndex++
            }
            nextDayNumber++
            mapped
        }

        return planner.copy(
            durationDays = finalAssignments.size,
            assignments = finalAssignments,
            assignmentProgress = newAssignmentProgress,
            assignmentReadPages = newAssignmentReadPages,
            assignmentCompletedItems = newAssignmentCompletedItems,
            assignmentCompletedAt = newAssignmentCompletedAt,
            completedDays = newCompletedDays
        )
    }

    // Web parity (planner.js:625-643): completion comes from progress
    // (not completedDays); overdue+any-read-pages (incl. item-derived) is
    // 'partial'; date==today is always 'today' — web has no partial branch
    // for today and checks overdue before today.
    fun getAssignmentStatus(plan: ReadingPlan, assignment: PlannerAssignment, today: String = formatPlannerDate()): String {
        val progress = getAssignmentProgress(plan, assignment)
        if (progress.isComplete) return "completed"
        if (assignment.date < today) {
            return if (progress.readPagesCount > 0) "partial" else "overdue"
        }
        if (assignment.date == today) return "today"
        return "upcoming"
    }

    // ── Resume Page Number ──────────────────────────────────────────────
    // Web parity (planner.js getAssignmentResumePageNumber): first unread page
    // in range, accounting for item-level completedRangeValues; falls back to
    // the next item's page bounds, then the assignment start.
    fun getAssignmentResumePageNumber(plan: ReadingPlan, assignment: PlannerAssignment): Int {
        val prog = getAssignmentProgress(plan, assignment)
        if (prog.isComplete) return assignment.pageStart
        val explicitReadPages = plan.assignmentReadPages[assignment.dayNumber] ?: emptyList()
        // Web parity (planner.js:758-759): `pageStart || 1`, `pageEnd || start`.
        val start = assignment.pageStart.takeIf { it != 0 } ?: 1
        val end = assignment.pageEnd.takeIf { it != 0 } ?: start
        for (p in start..end) {
            if (explicitReadPages.contains(p)) continue
            var coveredByItem = false
            for (item in assignment.items) {
                if (prog.completedRangeValues.contains(item.rangeValue)) {
                    if (p in item.pageStart..item.pageEnd) {
                        coveredByItem = true
                        break
                    }
                }
            }
            if (!coveredByItem) return p
        }
        val nextItem = prog.nextItem
        if (nextItem != null) {
            val bounds = lookupItemPageBounds(nextItem, assignment.unitType)
            if (bounds.first > 0) return bounds.first
        }
        return assignment.pageStart
    }

    private fun lookupItemPageBounds(item: PlannerItem, unitType: String): Pair<Int, Int> {
        if (item.pageStart > 0 && item.pageEnd > 0) return item.pageStart to item.pageEnd
        return 0 to 0
    }

    // ── Weekly Summary ──────────────────────────────────────────────────
    fun getWeeklySummary(plan: ReadingPlan): List<WeeklySummary> {
        val weeks = mutableListOf<WeeklySummary>()
        val duration = plan.durationDays
        var weekNum = 1
        var i = 0
        while (i < duration) {
            val weekAssignments = plan.assignments.slice(i until min(i + 7, plan.assignments.size))
            if (weekAssignments.isEmpty()) break
            var totalUnits = 0
            var completedUnits = 0
            weekAssignments.forEach { a ->
                val prog = getAssignmentProgress(plan, a)
                // Web parity (planner.js:841): `prog?.totalPagesCount || 1`.
                totalUnits += prog.totalPagesCount.takeIf { it != 0 } ?: 1
                completedUnits += prog.readPagesCount
            }
            weeks.add(
                WeeklySummary(
                    label = "Week $weekNum",
                    completedUnits = completedUnits,
                    totalUnits = if (totalUnits > 0) totalUnits else 1
                )
            )
            weekNum++
            i += 7
        }
        return weeks
    }

    // ── Planner Analytics ───────────────────────────────────────────────
    // Web parity (planner.js getPlannerAnalytics): catchUpDaysCount = lateCount,
    // avgUnitsPerDay = totalReadPages / activeDays (assignments with readPages > 0).
    fun getPlannerAnalytics(plan: ReadingPlan, today: String = formatPlannerDate()): PlannerAnalytics {
        val metrics = getPlannerSuccessMetrics(plan, today)

        var totalReadPages = 0
        var activeDaysCount = 0
        plan.assignments.forEach { a ->
            val readPages = getAssignmentProgress(plan, a).readPagesCount
            if (readPages > 0) activeDaysCount++
            totalReadPages += readPages
        }
        val avgUnitsPerDay = if (activeDaysCount > 0) totalReadPages.toFloat() / activeDaysCount else 0f

        val elapsed = max(1, diffDays(plan.startDate, today))

        return PlannerAnalytics(
            onTimeRate = metrics?.successRate ?: 0,
            catchUpDays = metrics?.lateCount ?: 0,
            avgPagesPerDay = avgUnitsPerDay,
            totalReadPages = totalReadPages,
            elapsedDays = elapsed
        )
    }

    // Web parity: planner.js getDifficultyIndicators returns { level, pages }
    // per dayNumber — callers need the page count, not just the label.
    data class DifficultyIndicator(val level: String, val pages: Int)

    // ── Difficulty Indicators ───────────────────────────────────────────
    fun getDifficultyIndicators(assignments: List<PlannerAssignment>): Map<Int, DifficultyIndicator> {
        if (assignments.isEmpty()) return emptyMap()
        val pageCounts = assignments.map { a ->
            var pages = 0
            a.items.forEach { item ->
                if (item.pageStart > 0 && item.pageEnd > 0) {
                    pages += (item.pageEnd - item.pageStart + 1)
                }
            }
            a.dayNumber to pages
        }
        val totalPages = pageCounts.sumOf { it.second }
        val avgPages = totalPages.toFloat() / pageCounts.size
        val result = mutableMapOf<Int, DifficultyIndicator>()
        pageCounts.forEach { (dayNumber, pages) ->
            val level = when {
                pages > avgPages * 1.3f -> "heavy"
                pages < avgPages * 0.7f -> "light"
                else -> "moderate"
            }
            result[dayNumber] = DifficultyIndicator(level, pages)
        }
        return result
    }

    // ── Build Revision Planner ──────────────────────────────────────────
    fun buildRevisionPlanner(
        completedPlan: ReadingPlan,
        chapters: List<ChapterEntity>
    ): ReadingPlan {
        val halfDuration = max(1, (completedPlan.durationDays + 1) / 2)
        return buildReadingPlanner(
            unitType = completedPlan.unitType,
            durationDays = halfDuration,
            startDate = formatPlannerDate(),
            startUnit = completedPlan.startUnit,
            endUnit = completedPlan.endUnit,
            customTitle = "${completedPlan.title} (Revision)",
            excludeDays = completedPlan.excludeDays,
            chapters = chapters
        )
    }

    // ── Format Date Label ───────────────────────────────────────────────
    fun formatPlannerDateLabel(dateStr: String): String {
        return try {
            val date = parsePlannerDate(dateStr)
            SimpleDateFormat("EEE, MMM d", Locale.US).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}

// ── Additional Data Classes ─────────────────────────────────────────────
// Web parity (planner.js getWeeklySummary): pages-based { label, completedUnits
// (read pages), totalUnits (total pages) } per 7-day block.
data class WeeklySummary(
    val label: String,
    val completedUnits: Int,
    val totalUnits: Int
)

data class PlannerAnalytics(
    val onTimeRate: Int,
    val catchUpDays: Int,
    val avgPagesPerDay: Float,
    val totalReadPages: Int,
    val elapsedDays: Int
)

// ── Prayer Integration ──────────────────────────────────────────────────
val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

data class PrayerTimings(
    val date: String,
    val timings: Map<String, String> // e.g. "Fajr" -> "05:30"
)

data class PrayerSlot(
    val name: String,
    val time: String?,        // Formatted label e.g. "After 5:30 AM"
    val count: Int,           // Total items in this slot
    val doneInSlot: Int,      // Items completed in this slot
    val slotStart: Int,       // Start index into items list
    val slotEnd: Int,         // End index into items list
    val status: String        // "completed", "current", "upcoming", "empty", "locked"
)

fun buildPrayerSlots(
    plan: ReadingPlan,
    todayAssignment: PlannerAssignment?,
    prayerTimings: PrayerTimings?,
    readPreference: String = "after",         // "before", "after", "split"
    activePrayers: List<String> = PRAYER_NAMES
): List<PrayerSlot> {
    val sortedPrayers = activePrayers.sortedBy { PRAYER_NAMES.indexOf(it) }

    if (todayAssignment == null) {
        return sortedPrayers.map { name ->
            PrayerSlot(name = name, time = null, count = 0, doneInSlot = 0, slotStart = 0, slotEnd = 0, status = "upcoming")
        }
    }

    val items = todayAssignment.items
    val total = items.size
    val numSlots = sortedPrayers.size.coerceAtLeast(1)
    val completedRangeValues = plan.assignmentCompletedItems[todayAssignment.dayNumber] ?: emptyList()

    return sortedPrayers.mapIndexed { i, name ->
        val slotStart = ceil((i.toDouble() / numSlots) * total).toInt()
        val slotEnd = ceil(((i + 1).toDouble() / numSlots) * total).toInt()
        val slotItems = items.subList(slotStart.coerceAtMost(total), slotEnd.coerceAtMost(total))
        val count = slotItems.size.coerceAtLeast(0)
        val doneInSlot = slotItems.count { completedRangeValues.contains(it.rangeValue) }

        val isComplete = count > 0 && doneInSlot >= count
        // Web parity: slots unlock sequentially — a slot with pending items is
        // locked until every earlier slot is complete.
        val earlierIncomplete = (0 until i).any { prevIdx ->
            val pStart = ceil((prevIdx.toDouble() / numSlots) * total).toInt()
            val pEnd = ceil(((prevIdx + 1).toDouble() / numSlots) * total).toInt()
            val pItems = items.subList(pStart.coerceAtMost(total), pEnd.coerceAtMost(total))
            pItems.isNotEmpty() && pItems.any { !completedRangeValues.contains(it.rangeValue) }
        }
        val isCurrent = count > 0 && !isComplete && !earlierIncomplete
        // Web order: empty -> completed -> locked -> current -> upcoming.
        val status = when {
            count == 0 -> "empty"
            isComplete -> "completed"
            earlierIncomplete -> "locked"
            isCurrent -> "current"
            else -> "upcoming"
        }

        // Format time label
        var timeLabel: String? = null
        val rawTime = prayerTimings?.timings?.get(name)
        if (rawTime != null) {
            try {
                val parts = rawTime.split(":")
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().take(2) // Strip timezone like " (CEST)"
                val ampm = if (h >= 12) "PM" else "AM"
                val h12 = if (h % 12 == 0) 12 else h % 12
                val timeStr = "$h12:$m $ampm"
                timeLabel = when (readPreference) {
                    "before" -> "Before $timeStr"
                    "after" -> "After $timeStr"
                    else -> "Around $timeStr"
                }
            } catch (_: Exception) { }
        }

        PrayerSlot(
            name = name,
            time = timeLabel,
            count = count,
            doneInSlot = doneInSlot,
            slotStart = slotStart,
            slotEnd = slotEnd,
            status = status
        )
    }
}

