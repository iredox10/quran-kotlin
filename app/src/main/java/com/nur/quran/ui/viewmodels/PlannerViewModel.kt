package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.planner.*
import com.nur.quran.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val repository: QuranRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val plannerPrefs by lazy {
        appContext.getSharedPreferences("PlannerSettings", android.content.Context.MODE_PRIVATE)
    }

    private val _activePlan = MutableStateFlow<ReadingPlan?>(null)
    val activePlan: StateFlow<ReadingPlan?> = _activePlan.asStateFlow()

    private val _allPlans = MutableStateFlow<List<ReadingPlan>>(emptyList())
    val allPlans: StateFlow<List<ReadingPlan>> = _allPlans.asStateFlow()

    private val _activePlannerId = MutableStateFlow<String?>(null)
    val activePlannerId: StateFlow<String?> = _activePlannerId.asStateFlow()

    private val _archivedPlans = MutableStateFlow<List<ReadingPlan>>(emptyList())
    val archivedPlans: StateFlow<List<ReadingPlan>> = _archivedPlans.asStateFlow()

    private val _prayerTimings = MutableStateFlow<PrayerTimings?>(null)
    val prayerTimings: StateFlow<PrayerTimings?> = _prayerTimings.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    // ── Per-plan highlights (web: plannerBookmarks) ─────────────────────
    private val _plannerBookmarks = MutableStateFlow<List<PlannerBookmark>>(emptyList())
    val plannerBookmarks: StateFlow<List<PlannerBookmark>> = _plannerBookmarks.asStateFlow()

    // ── Per-plan reading timers, day -> total seconds (web: plannerSessionTimers)
    // Active-plan view — derived from [_sessionTotalsByPlan]; collectors keep working unchanged.
    private val _sessionTotals = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val sessionTotals: StateFlow<Map<Int, Long>> = _sessionTotals.asStateFlow()

    // ── PlanId-keyed extras (web parity: plannerReflections / plannerBookmarks / plannerSessionTimers)
    // Web holds these as { [plannerId]: ... } maps for ALL plans at once. The
    // active-plan flows above stay as derived views so every existing function
    // signature and every screen collector keeps working unchanged.
    private val _plannerReflectionsByPlan = MutableStateFlow<Map<String, Map<Int, String>>>(emptyMap())
    val plannerReflectionsByPlan: StateFlow<Map<String, Map<Int, String>>> = _plannerReflectionsByPlan.asStateFlow()

    private val _plannerBookmarksByPlan = MutableStateFlow<Map<String, List<PlannerBookmark>>>(emptyMap())
    val plannerBookmarksByPlan: StateFlow<Map<String, List<PlannerBookmark>>> = _plannerBookmarksByPlan.asStateFlow()

    private val _sessionTotalsByPlan = MutableStateFlow<Map<String, Map<Int, Long>>>(emptyMap())
    val sessionTotalsByPlan: StateFlow<Map<String, Map<Int, Long>>> = _sessionTotalsByPlan.asStateFlow()

    // Session-only timer start stamps, keyed by plan like web startedAt (never persisted).
    private val _timerStartedAtByPlan = MutableStateFlow<Map<String, Map<Int, Long?>>>(emptyMap())

    // ── Planner settings (same SharedPreferences file the UI already uses)
    private val _useDeviceLocation = MutableStateFlow(false)
    val useDeviceLocation: StateFlow<Boolean> = _useDeviceLocation.asStateFlow()

    private val _activePrayers = MutableStateFlow<List<String>>(PRAYER_NAMES)
    val activePrayers: StateFlow<List<String>> = _activePrayers.asStateFlow()

    // ── Prayer reading preference (web: prayerSettings.readPreference, default 'after')
    private val _readPreference = MutableStateFlow("after")
    val readPreference: StateFlow<String> = _readPreference.asStateFlow()

    // ── Intention prompt (web: intentionPromptEnabled, default true)
    private val _useIntentionPrompt = MutableStateFlow(true)
    val useIntentionPrompt: StateFlow<Boolean> = _useIntentionPrompt.asStateFlow()

    private var extrasPlanId: String? = null

    init {
        _useDeviceLocation.value = plannerPrefs.getBoolean("use_device_location", false)
        _activePrayers.value = plannerPrefs.getStringSet("active_prayers", null)
            ?.filter { PRAYER_NAMES.contains(it) }
            ?.sortedBy { PRAYER_NAMES.indexOf(it) }
            ?.takeIf { it.isNotEmpty() } ?: PRAYER_NAMES
        _readPreference.value = plannerPrefs.getString("reading_preference", "after")
            ?.takeIf { it == "before" || it == "after" || it == "split" } ?: "after"
        // Same key the screens already read/write ("show_intention_prompt") so both stay in sync.
        _useIntentionPrompt.value = plannerPrefs.getBoolean("show_intention_prompt", true)
        loadData()
        if (_useDeviceLocation.value) {
            refreshPrayerTimingsWithDeviceLocation()
        } else {
            fetchPrayerTimings()
        }
    }

    /** Push the keyed-map entries for [planId] into the derived active-plan views. */
    private fun refreshActiveExtrasViews(planId: String?) {
        _plannerBookmarks.value = _plannerBookmarksByPlan.value[planId] ?: emptyList()
        _sessionTotals.value = _sessionTotalsByPlan.value[planId] ?: emptyMap()
        _timerStartedAt.value = _timerStartedAtByPlan.value[planId] ?: emptyMap()
    }

    /**
     * Load per-plan extras when the active plan changes (web: plannerBookmarks /
     * plannerSessionTimers are planId-keyed maps held for ALL plans).
     * Entries already in the maps are reused; missing ones load from the
     * repository once, then the active-plan views are re-derived.
     */
    private fun loadPlanExtras(planId: String?) {
        if (planId == extrasPlanId) {
            refreshActiveExtrasViews(planId)
            return
        }
        extrasPlanId = planId
        if (planId == null) {
            refreshActiveExtrasViews(null)
            return
        }
        // Fast path: maps already hold this plan (preloaded or written this session).
        // Unlike before, timer start stamps for OTHER plans are preserved — web
        // keeps startedAt per planId, so peeking at another plan must not kill a
        // running timer.
        val haveBookmarks = _plannerBookmarksByPlan.value.containsKey(planId)
        val haveTotals = _sessionTotalsByPlan.value.containsKey(planId)
        refreshActiveExtrasViews(planId)
        if (haveBookmarks && haveTotals) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (!haveBookmarks) {
                try {
                    val loaded = repository.getPlannerBookmarks(planId)
                    _plannerBookmarksByPlan.value = _plannerBookmarksByPlan.value + (planId to loaded)
                    if (planId == _activePlannerId.value) _plannerBookmarks.value = loaded
                } catch (_: Exception) {}
            }
            if (!haveTotals) {
                try {
                    val loaded = repository.getPlannerSessionTotals(planId)
                    _sessionTotalsByPlan.value = _sessionTotalsByPlan.value + (planId to loaded)
                    if (planId == _activePlannerId.value) _sessionTotals.value = loaded
                } catch (_: Exception) {}
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _chapters.value = repository.getChaptersFlow().firstOrNull() ?: emptyList()
            
            var all = repository.getAllPlans()
            var actId = repository.getActivePlannerId()

            if (all.isEmpty()) {
                val oldActive = repository.getActivePlan()
                val oldArchived = repository.getArchivedPlans()
                
                val migratedPlans = mutableListOf<ReadingPlan>()
                if (oldActive != null) migratedPlans.add(oldActive)
                
                if (migratedPlans.isNotEmpty()) {
                    all = migratedPlans
                    actId = oldActive?.id
                    repository.saveAllPlans(all)
                    repository.saveActivePlannerId(actId)
                    repository.saveActivePlan(null)
                }
            }
            
            _allPlans.value = all
            _activePlannerId.value = actId
            _activePlan.value = all.find { it.id == actId }
            _archivedPlans.value = repository.getArchivedPlans()
            loadPlanExtras(actId)

            repository.getAllBookmarksFlow().collect { bmList ->
                _bookmarks.value = bmList
            }
        }
    }

    fun setActivePlan(plan: ReadingPlan) {
        viewModelScope.launch {
            val updatedAll = _allPlans.value.toMutableList()
            val index = updatedAll.indexOfFirst { it.id == plan.id }
            if (index != -1) {
                updatedAll[index] = plan
            } else {
                updatedAll.add(plan)
            }
            _allPlans.value = updatedAll
            _activePlannerId.value = plan.id
            _activePlan.value = plan
            repository.saveAllPlans(updatedAll)
            repository.saveActivePlannerId(plan.id)
            loadPlanExtras(plan.id)
        }
    }

    fun switchActivePlan(planId: String) {
        val plan = _allPlans.value.find { it.id == planId }
        if (plan != null) {
            viewModelScope.launch {
                _activePlannerId.value = planId
                _activePlan.value = plan
                repository.saveActivePlannerId(planId)
                loadPlanExtras(planId)
            }
        }
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            val updatedAll = _allPlans.value.filter { it.id != planId }
            _allPlans.value = updatedAll
            repository.saveAllPlans(updatedAll)
            
            if (_activePlannerId.value == planId) {
                val nextActive = updatedAll.firstOrNull()
                _activePlannerId.value = nextActive?.id
                _activePlan.value = nextActive
                repository.saveActivePlannerId(nextActive?.id)
                loadPlanExtras(nextActive?.id)
            }
        }
    }

    fun deleteActivePlan() {
        val currentId = _activePlannerId.value ?: return
        deletePlan(currentId)
    }

    fun archiveActivePlan() {
        val current = _activePlan.value ?: return
        archivePlanner(current.id)
    }

    /**
     * Web parity: archivePlanner(planId) archives an arbitrary plan by id
     * (web useAppStore.js:792 — targetId = planId || activePlannerId).
     * archiveActivePlan() keeps its signature and delegates here.
     */
    fun archivePlanner(planId: String? = null) {
        val targetId = planId ?: _activePlannerId.value ?: return
        val target = _allPlans.value.find { it.id == targetId } ?: _activePlan.value?.takeIf { it.id == targetId } ?: return
        viewModelScope.launch {
            val updatedArchives = _archivedPlans.value + target
            _archivedPlans.value = updatedArchives
            repository.saveArchivedPlans(updatedArchives)

            deletePlan(targetId)
        }
    }

    fun markPageRead(dayNumber: Int, pageNumber: Int) {
        val current = _activePlan.value ?: return
        val currentReadPages = current.assignmentReadPages[dayNumber] ?: emptyList()
        if (currentReadPages.contains(pageNumber)) return

        val updatedReadPagesMap = current.assignmentReadPages.toMutableMap()
        updatedReadPagesMap[dayNumber] = currentReadPages + pageNumber

        // Check if all pages for this assignment are read
        val assignment = current.assignments.find { it.dayNumber == dayNumber }
        var isNowComplete = false
        if (assignment != null) {
            val totalPages = (assignment.pageEnd - assignment.pageStart + 1)
            if ((currentReadPages.size + 1) >= totalPages) {
                isNowComplete = true
            }
        }

        val updatedCompletedDays = if (isNowComplete && !current.completedDays.contains(dayNumber)) {
            current.completedDays + dayNumber
        } else current.completedDays

        val updatedCompletedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (isNowComplete && !updatedCompletedAtMap.containsKey(dayNumber)) {
            updatedCompletedAtMap[dayNumber] = PlannerEngine.formatPlannerDate()
        }

        // Keep the explicit completed-items map in sync so prayer-slot progress
        // (doneInSlot) agrees with page-derived completion.
        val updatedCompletedItemsMap = current.assignmentCompletedItems.toMutableMap()
        val updatedProgressMap = current.assignmentProgress.toMutableMap()
        if (isNowComplete && assignment != null) {
            updatedCompletedItemsMap[dayNumber] = assignment.items.map { it.rangeValue }
            updatedProgressMap[dayNumber] = assignment.items.size
        }

        val updatedPlan = current.copy(
            assignmentReadPages = updatedReadPagesMap,
            completedDays = updatedCompletedDays,
            assignmentCompletedAt = updatedCompletedAtMap,
            assignmentCompletedItems = updatedCompletedItemsMap,
            assignmentProgress = updatedProgressMap,
            lastReadPage = pageNumber
        )

        setActivePlan(updatedPlan)
    }

    /**
     * Web: setPlannerAssignmentProgress — clamp count, take first N items as
     * completed, stamp/clear completion date, recompute completedDays.
     * Used by prayer-slot mark/undo on the dashboard.
     */
    fun setPlannerAssignmentProgress(dayNumber: Int, completedCount: Int) {
        val current = _activePlan.value ?: return
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return
        val total = assignment.items.size
        val safe = completedCount.coerceIn(0, total)
        val completedItems = assignment.items.take(safe).map { it.rangeValue }

        val completedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (safe >= total && total > 0) {
            if (!completedAtMap.containsKey(dayNumber)) {
                completedAtMap[dayNumber] = PlannerEngine.formatPlannerDate()
            }
        } else {
            completedAtMap.remove(dayNumber)
        }

        val completedItemsMap = current.assignmentCompletedItems.toMutableMap()
        completedItemsMap[dayNumber] = completedItems
        val progressMap = current.assignmentProgress.toMutableMap()
        progressMap[dayNumber] = safe

        val completedDays = current.assignments
            .filter { (completedItemsMap[it.dayNumber]?.size ?: 0) >= it.items.size && it.items.isNotEmpty() }
            .map { it.dayNumber }
            .sorted()

        setActivePlan(
            current.copy(
                assignmentProgress = progressMap,
                assignmentCompletedItems = completedItemsMap,
                assignmentCompletedAt = completedAtMap,
                completedDays = completedDays
            )
        )
    }

    /**
     * Web: markPlannerItemComplete — union a single item rangeValue into the
     * day's completed items (used by the reader's per-item Done button).
     */
    fun markPlannerItemComplete(dayNumber: Int, rangeValue: String) {
        val current = _activePlan.value ?: return
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return
        if (assignment.items.none { it.rangeValue == rangeValue }) return
        val existing = current.assignmentCompletedItems[dayNumber] ?: emptyList()
        if (existing.contains(rangeValue)) return
        val next = (existing + rangeValue).filter { v -> assignment.items.any { it.rangeValue == v } }

        val completedItemsMap = current.assignmentCompletedItems.toMutableMap()
        completedItemsMap[dayNumber] = next
        val progressMap = current.assignmentProgress.toMutableMap()
        progressMap[dayNumber] = next.size
        val completedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (next.size >= assignment.items.size) {
            if (!completedAtMap.containsKey(dayNumber)) {
                completedAtMap[dayNumber] = PlannerEngine.formatPlannerDate()
            }
        }
        val completedDays = current.assignments
            .filter { (completedItemsMap[it.dayNumber]?.size ?: 0) >= it.items.size && it.items.isNotEmpty() }
            .map { it.dayNumber }
            .sorted()

        setActivePlan(
            current.copy(
                assignmentProgress = progressMap,
                assignmentCompletedItems = completedItemsMap,
                assignmentCompletedAt = completedAtMap,
                completedDays = completedDays
            )
        )
    }

    fun toggleAssignmentCompleted(dayNumber: Int) {
        val current = _activePlan.value ?: return
        val assignment = current.assignments.find { it.dayNumber == dayNumber } ?: return
        val isCompleted = current.completedDays.contains(dayNumber)
        // Route through the progress maps so completedDays never disagrees
        // with the underlying completed items.
        setPlannerAssignmentProgress(dayNumber, if (isCompleted) 0 else assignment.items.size)
    }

    fun setLastReadPosition(pageNumber: Int, verseKey: String? = null) {
        val current = _activePlan.value ?: return
        val updatedPlan = current.copy(
            lastReadPage = pageNumber,
            lastReadVerseKey = verseKey
        )
        setActivePlan(updatedPlan)
    }

    fun rebalancePlan(strategy: String, customDurationDays: Int? = null) {
        val current = _activePlan.value ?: return
        val rebalanced = PlannerEngine.rebalancePlanner(current, strategy, customDurationDays)
        setActivePlan(rebalanced)
    }

    /**
     * Web: adjustActivePlannerPace — thin wrapper over the custom_pace rebalance.
     * Web store :559 → adjustPlannerPace → rebalancePlanner('custom_pace', n).
     */
    fun adjustActivePlannerPace(newDurationDays: Int) {
        rebalancePlan("custom_pace", newDurationDays)
    }

    /** Creation guard: true when a plan with this title already exists (case-insensitive). */
    fun planTitleExists(title: String): Boolean {
        val needle = title.trim()
        if (needle.isEmpty()) return false
        return (_allPlans.value + _archivedPlans.value).any {
            it.title.trim().equals(needle, ignoreCase = true)
        }
    }

    fun restoreArchivedPlan(planId: String) {
        val target = _archivedPlans.value.find { it.id == planId } ?: return
        val updatedArchives = _archivedPlans.value.filter { it.id != planId }

        viewModelScope.launch {
            _archivedPlans.value = updatedArchives
            repository.saveArchivedPlans(updatedArchives)
            
            setActivePlan(target)
        }
    }

    fun deleteArchivedPlan(planId: String) {
        val updatedArchives = _archivedPlans.value.filter { it.id != planId }
        viewModelScope.launch {
            _archivedPlans.value = updatedArchives
            repository.saveArchivedPlans(updatedArchives)
        }
    }

    fun saveReflection(dayNumber: Int, note: String) {
        val current = _activePlan.value ?: return
        val updatedMap = current.assignmentReflections.toMutableMap()
        if (note.isNotBlank()) {
            updatedMap[dayNumber] = note
        } else {
            updatedMap.remove(dayNumber)
        }
        val updatedPlan = current.copy(assignmentReflections = updatedMap)
        setActivePlan(updatedPlan)
    }

    fun buildRevisionPlan() {
        val current = _activePlan.value ?: return
        val chaptersVal = _chapters.value
        viewModelScope.launch {
            // Archive current plan first
            val updatedArchives = _archivedPlans.value + current
            _archivedPlans.value = updatedArchives
            repository.saveArchivedPlans(updatedArchives)
            
            // Note: old buildRevisionPlan logic overwrote active plan. We should probably do similar but just create new one
            val revision = PlannerEngine.buildRevisionPlanner(current, chaptersVal)
            
            // We should delete current from all plans because we archived it
            val updatedAll = _allPlans.value.filter { it.id != current.id }
            
            val finalAll = updatedAll + revision
            _activePlannerId.value = revision.id
            _activePlan.value = revision
            repository.saveAllPlans(finalAll)
            repository.saveActivePlannerId(revision.id)
        }
    }

    fun fetchPrayerTimings(lat: Double = 21.4225, lng: Double = 39.8262) { // Default Mecca coordinates
        viewModelScope.launch {
            val todayStr = PlannerEngine.formatPlannerDate()
            try {
                // Fetch from Aladhan API
                val url = "https://api.aladhan.com/v1/timings?latitude=$lat&longitude=$lng&method=2"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                if (connection.responseCode == 200) {
                    val stream = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(stream)
                    if (json.optInt("code") == 200) {
                        val dataObj = json.getJSONObject("data")
                        val timingsObj = dataObj.getJSONObject("timings")
                        val timingsMap = mapOf(
                            "Fajr" to timingsObj.getString("Fajr"),
                            "Dhuhr" to timingsObj.getString("Dhuhr"),
                            "Asr" to timingsObj.getString("Asr"),
                            "Maghrib" to timingsObj.getString("Maghrib"),
                            "Isha" to timingsObj.getString("Isha")
                        )
                        _prayerTimings.value = PrayerTimings(date = todayStr, timings = timingsMap)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Fallback default times
            _prayerTimings.value = PrayerTimings(
                date = todayStr,
                timings = mapOf(
                    "Fajr" to "05:00",
                    "Dhuhr" to "12:30",
                    "Asr" to "15:45",
                    "Maghrib" to "18:15",
                    "Isha" to "19:45"
                )
            )
        }
    }

    // ── Per-plan highlights (web: plannerBookmarks[plannerId]) ────────────
    // Writes go through the planId-keyed map; the active-plan view is re-derived
    // so existing collectors see the same values as before.
    fun togglePlannerBookmark(verseKey: String, surahName: String) {
        val planId = _activePlannerId.value ?: return
        togglePlannerBookmarkForPlan(planId, verseKey, surahName)
    }

    /**
     * Web parity: addPlannerBookmark(plannerId, verseKey, surahName, note) —
     * planId-keyed, deduped by verseKey, updates the note when it already exists.
     */
    fun addPlannerBookmark(planId: String, verseKey: String, surahName: String, note: String = "") {
        val current = (_plannerBookmarksByPlan.value[planId] ?: emptyList()).toMutableList()
        val existingIdx = current.indexOfFirst { it.verseKey == verseKey }
        if (existingIdx < 0) {
            current.add(PlannerBookmark(verseKey = verseKey, surahName = surahName, note = note))
        } else if (note.isNotBlank()) {
            current[existingIdx] = current[existingIdx].copy(note = note)
        } else {
            return
        }
        persistPlannerBookmarks(planId, current)
    }

    /** Web parity: toggle a highlight for an explicit plan (active-plan overload delegates here). */
    fun togglePlannerBookmarkForPlan(planId: String, verseKey: String, surahName: String) {
        val current = (_plannerBookmarksByPlan.value[planId] ?: emptyList()).toMutableList()
        val existing = current.indexOfFirst { it.verseKey == verseKey }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(PlannerBookmark(verseKey = verseKey, surahName = surahName))
        }
        persistPlannerBookmarks(planId, current)
    }

    fun removePlannerBookmark(verseKey: String) {
        val planId = _activePlannerId.value ?: return
        removePlannerBookmarkForPlan(planId, verseKey)
    }

    /** Web parity: removePlannerBookmark(plannerId, verseKey). */
    fun removePlannerBookmarkForPlan(planId: String, verseKey: String) {
        val current = (_plannerBookmarksByPlan.value[planId] ?: emptyList()).filterNot { it.verseKey == verseKey }
        persistPlannerBookmarks(planId, current)
    }

    private fun persistPlannerBookmarks(planId: String, bookmarks: List<PlannerBookmark>) {
        _plannerBookmarksByPlan.value = _plannerBookmarksByPlan.value + (planId to bookmarks)
        if (planId == _activePlannerId.value) _plannerBookmarks.value = bookmarks
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { repository.savePlannerBookmarks(planId, bookmarks) } catch (_: Exception) {}
        }
    }

    // ── Reading timers (web: startPlannerTimer/stopPlannerTimer) ────────
    // Web keeps plannerSessionTimers[planId][day] = { totalSeconds, startedAt }.
    // Totals persist in the repository (key "planner_sessions_$planId"); startedAt
    // is session-only in-memory state. Both are keyed by planId so switching plans
    // no longer wipes a running timer.
    // Active-plan view — derived from [_timerStartedAtByPlan].
    private val _timerStartedAt = MutableStateFlow<Map<Int, Long?>>(emptyMap())
    val timerStartedAt: StateFlow<Map<Int, Long?>> = _timerStartedAt.asStateFlow()

    /**
     * Web: startPlannerTimer — ensure a { totalSeconds: 0 } entry exists, then
     * stamp startedAt. Mirrors the web guard `if (!timers[planId][day])`.
     */
    fun startPlannerTimer(planId: String? = null, dayNumber: Int) {
        val resolvedPlanId = planId ?: _activePlannerId.value ?: return
        val updated = (_sessionTotalsByPlan.value[resolvedPlanId] ?: emptyMap()).toMutableMap()
        if (!updated.containsKey(dayNumber)) {
            updated[dayNumber] = 0L
        }
        _sessionTotalsByPlan.value = _sessionTotalsByPlan.value + (resolvedPlanId to updated)
        val started = (_timerStartedAtByPlan.value[resolvedPlanId] ?: emptyMap()).toMutableMap()
        started[dayNumber] = System.currentTimeMillis()
        _timerStartedAtByPlan.value = _timerStartedAtByPlan.value + (resolvedPlanId to started)
        if (resolvedPlanId == _activePlannerId.value) {
            _sessionTotals.value = updated
            _timerStartedAt.value = started
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { repository.savePlannerSessionTotals(resolvedPlanId, updated) } catch (_: Exception) {}
        }
    }

    /** Persist additional seconds for a day (called periodically + on pause/exit). */
    fun stopPlannerTimer(dayNumber: Int, additionalSeconds: Long) {
        val planId = _activePlannerId.value ?: return
        stopPlannerTimerForPlan(planId, dayNumber, additionalSeconds)
    }

    /** Web parity: stopPlannerTimer(plannerId, dayNumber, additionalSeconds). */
    fun stopPlannerTimerForPlan(planId: String, dayNumber: Int, additionalSeconds: Long) {
        val started = (_timerStartedAtByPlan.value[planId] ?: emptyMap()).toMutableMap()
        if (started.remove(dayNumber) != null) {
            _timerStartedAtByPlan.value = _timerStartedAtByPlan.value + (planId to started)
            if (planId == _activePlannerId.value) _timerStartedAt.value = started
        }
        if (additionalSeconds <= 0) return
        val updated = (_sessionTotalsByPlan.value[planId] ?: emptyMap()).toMutableMap()
        updated[dayNumber] = (updated[dayNumber] ?: 0L) + additionalSeconds
        _sessionTotalsByPlan.value = _sessionTotalsByPlan.value + (planId to updated)
        if (planId == _activePlannerId.value) _sessionTotals.value = updated
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { repository.savePlannerSessionTotals(planId, updated) } catch (_: Exception) {}
        }
    }

    /**
     * Bridge planner reading time into reading_sessions (Home stats/streak).
     * Reads the sessionTotals[dayNumber] checkpoint (seconds), logs it as a
     * "reading" session, then resets that day's total to 0 so repeated calls
     * can't double-log; the live timer keeps accumulating fresh time via
     * stopPlannerTimer deltas on top of the reset total.
     *
     * Safe re stopPlannerTimer bookkeeping: it only ever ADDS deltas
     * (`totals[day] + additionalSeconds`), never overwrites, so zeroing here
     * can't corrupt in-flight timing — at most the screen's unflushed
     * remainder (timerTick - savedTick) lands on top of 0 on the next flush.
     * No-op when the total is 0 or there's no active plan. Never throws.
     */
    fun logPlannerDayToSessions(dayNumber: Int, chapterId: Int? = null) {
        val planId = _activePlannerId.value ?: return
        val total = (_sessionTotalsByPlan.value[planId] ?: emptyMap())[dayNumber] ?: 0L
        if (total <= 0) return
        // Checkpoint synchronously so a repeated call can't double-log.
        val reset = (_sessionTotalsByPlan.value[planId] ?: emptyMap()).toMutableMap()
        reset[dayNumber] = 0L
        _sessionTotalsByPlan.value = _sessionTotalsByPlan.value + (planId to reset)
        _sessionTotals.value = reset
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.logReadingSession(total, "reading", chapterId)
                repository.savePlannerSessionTotals(planId, reset)
            } catch (_: Exception) { /* planner time stays invisible rather than crash */ }
        }
    }

    // ── Prayer settings ─────────────────────────────────────────────────
    fun setActivePrayers(prayers: List<String>) {
        val cleaned = prayers.filter { PRAYER_NAMES.contains(it) }
            .sortedBy { PRAYER_NAMES.indexOf(it) }
            .takeIf { it.isNotEmpty() } ?: PRAYER_NAMES
        _activePrayers.value = cleaned
        plannerPrefs.edit().putStringSet("active_prayers", cleaned.toSet()).apply()
    }

    /** Web parity: prayerSettings.readPreference ('before' | 'after' | 'split', default 'after'). */
    fun setReadPreference(preference: String) {
        val cleaned = if (preference == "before" || preference == "after" || preference == "split") {
            preference
        } else {
            "after"
        }
        _readPreference.value = cleaned
        plannerPrefs.edit().putString("reading_preference", cleaned).apply()
    }

    /** Web parity: intentionPromptEnabled (default true). */
    fun setUseIntentionPrompt(enabled: Boolean) {
        _useIntentionPrompt.value = enabled
        plannerPrefs.edit().putBoolean("show_intention_prompt", enabled).apply()
    }

    fun setUseDeviceLocation(enabled: Boolean) {
        _useDeviceLocation.value = enabled
        plannerPrefs.edit().putBoolean("use_device_location", enabled).apply()
        if (enabled) {
            refreshPrayerTimingsWithDeviceLocation()
        } else {
            fetchPrayerTimings()
        }
    }

    /** Web parity: Aladhan timings for the device location (Mecca fallback). */
    fun refreshPrayerTimingsWithDeviceLocation() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val loc = getDeviceLocation()
            if (loc != null) {
                fetchPrayerTimings(loc.first, loc.second)
            } else {
                fetchPrayerTimings()
            }
        }
    }

    private fun getDeviceLocation(): Pair<Double, Double>? {
        return try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return null
            val manager = appContext.getSystemService(android.content.Context.LOCATION_SERVICE)
                as? android.location.LocationManager ?: return null
            val providers = manager.getProviders(true)
            // Last-known first: instant, no GPS wait.
            for (name in listOf(
                android.location.LocationManager.NETWORK_PROVIDER,
                android.location.LocationManager.GPS_PROVIDER
            )) {
                if (!providers.contains(name)) continue
                try {
                    @Suppress("MissingPermission")
                    val last = manager.getLastKnownLocation(name)
                    if (last != null) return last.latitude to last.longitude
                } catch (_: Exception) {}
            }
            // Single fresh fix with a bounded wait (IO thread: blocking is fine).
            val latch = java.util.concurrent.CountDownLatch(1)
            var fresh: Pair<Double, Double>? = null
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    fresh = location.latitude to location.longitude
                    latch.countDown()
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }
            try {
                for (name in listOf(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    android.location.LocationManager.GPS_PROVIDER
                )) {
                    if (!providers.contains(name)) continue
                    try {
                        @Suppress("MissingPermission")
                        manager.requestSingleUpdate(name, listener, null)
                        break
                    } catch (_: Exception) {}
                }
                latch.await(8, java.util.concurrent.TimeUnit.SECONDS)
            } finally {
                try { manager.removeUpdates(listener) } catch (_: Exception) {}
            }
            fresh
        } catch (_: Exception) {
            null
        }
    }
}
