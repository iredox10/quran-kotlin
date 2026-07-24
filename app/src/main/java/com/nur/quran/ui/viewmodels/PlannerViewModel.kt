package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: QuranRepository
) : ViewModel() {

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

    init {
        loadData()
        fetchPrayerTimings()
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
        }
    }

    fun switchActivePlan(planId: String) {
        val plan = _allPlans.value.find { it.id == planId }
        if (plan != null) {
            viewModelScope.launch {
                _activePlannerId.value = planId
                _activePlan.value = plan
                repository.saveActivePlannerId(planId)
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
            }
        }
    }

    fun deleteActivePlan() {
        val currentId = _activePlannerId.value ?: return
        deletePlan(currentId)
    }

    fun archiveActivePlan() {
        val current = _activePlan.value ?: return
        viewModelScope.launch {
            val updatedArchives = _archivedPlans.value + current
            _archivedPlans.value = updatedArchives
            repository.saveArchivedPlans(updatedArchives)
            
            deletePlan(current.id)
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

        val updatedPlan = current.copy(
            assignmentReadPages = updatedReadPagesMap,
            completedDays = updatedCompletedDays,
            assignmentCompletedAt = updatedCompletedAtMap,
            lastReadPage = pageNumber
        )

        setActivePlan(updatedPlan)
    }

    fun toggleAssignmentCompleted(dayNumber: Int) {
        val current = _activePlan.value ?: return
        val isCompleted = current.completedDays.contains(dayNumber)

        val updatedCompletedDays = if (isCompleted) {
            current.completedDays - dayNumber
        } else {
            current.completedDays + dayNumber
        }

        val updatedCompletedAtMap = current.assignmentCompletedAt.toMutableMap()
        if (!isCompleted) {
            updatedCompletedAtMap[dayNumber] = PlannerEngine.formatPlannerDate()
        } else {
            updatedCompletedAtMap.remove(dayNumber)
        }

        val updatedPlan = current.copy(
            completedDays = updatedCompletedDays,
            assignmentCompletedAt = updatedCompletedAtMap
        )

        setActivePlan(updatedPlan)
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
}
