package com.nur.quran.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val chapters: List<ChapterEntity>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

/** One column of the "This Week" heatmap, mirroring the web app's weekData entries. */
data class WeekDay(val label: String, val mins: Int, val isToday: Boolean)

/** Aggregated reading stats shown on Home, computed exactly like the web app. */
data class HomeStats(
    val streak: Int = 0,
    val todayMinutes: Int = 0,
    val totalHours: String = "0.0",
    val week: List<WeekDay> = emptyList(),
    val weekMax: Int = 1
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QuranRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _browseMode = MutableStateFlow("surah") // "surah", "page", "juz", "hizb"
    val browseMode: StateFlow<String> = _browseMode.asStateFlow()

    val recentlyRead: StateFlow<List<RecentlyReadEntity>> = repository.getRecentlyReadFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestBookmark: StateFlow<BookmarkEntity?> = repository.getLatestBookmarkFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stats: StateFlow<HomeStats> = repository.getReadingSessionsFlow()
        .map { sessions -> computeStats(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats())

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _isOnline.value = true }
        override fun onLost(network: Network) { _isOnline.value = isCurrentlyOnline() }
        override fun onUnavailable() { _isOnline.value = false }
    }

    init {
        _isOnline.value = isCurrentlyOnline()
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        loadChapters()
    }

    override fun onCleared() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }

    private fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadChapters() {
        viewModelScope.launch {
            repository.getChaptersFlow()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown Error")
                }
                .collect { list ->
                    if (list.isEmpty()) {
                        refreshChapters()
                    } else {
                        _uiState.value = HomeUiState.Success(list)
                    }
                }
        }
    }

    fun refreshChapters() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                repository.refreshChapters()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to refresh chapters")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBrowseMode(mode: String) {
        _browseMode.value = mode
    }

    // ─── Stats computation (ported from the web app's Home.jsx) ─────────

    private fun computeStats(sessions: List<ReadingSessionEntity>): HomeStats {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(Date())

        val todayMinutes = Math.round(
            sessions.filter { it.date == today }.sumOf { it.duration } / 60.0f
        ).toInt()

        val totalHours = String.format(Locale.US, "%.1f", sessions.sumOf { it.duration } / 3600.0)

        // Streak: consecutive days with at least one session, counting back from today
        // (or yesterday if today has none yet)
        val uniqueDays = sessions.map { it.date }.toSet()
        val streak = computeStreak(uniqueDays, fmt)

        // Weekly heatmap: last 7 days, oldest first
        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val week = (6 downTo 0).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -i)
            val ds = fmt.format(cal.time)
            val dayMins = Math.round(
                sessions.filter { it.date == ds }.sumOf { it.duration } / 60.0f
            ).toInt()
            WeekDay(
                label = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1],
                mins = dayMins,
                isToday = i == 0
            )
        }
        val weekMax = maxOf(week.maxOfOrNull { it.mins } ?: 0, 1)

        return HomeStats(
            streak = streak,
            todayMinutes = todayMinutes,
            totalHours = totalHours,
            week = week,
            weekMax = weekMax
        )
    }

    private fun computeStreak(uniqueDays: Set<String>, fmt: SimpleDateFormat): Int {
        if (uniqueDays.isEmpty()) return 0
        val newest = uniqueDays.sortedDescending().first()
        val cal = Calendar.getInstance()
        val todayStr = fmt.format(cal.time)
        if (newest != todayStr) {
            cal.add(Calendar.DATE, -1)
            if (newest != fmt.format(cal.time)) return 0
        }
        var count = 0
        for (i in 0 until 365) {
            val check = Calendar.getInstance()
            check.add(Calendar.DATE, -i)
            val ds = fmt.format(check.time)
            if (ds in uniqueDays) count++
            else if (i > 0) break
        }
        return count
    }
}
