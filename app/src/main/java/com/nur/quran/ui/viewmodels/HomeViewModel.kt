package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val chapters: List<ChapterEntity>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _browseMode = MutableStateFlow("surah") // "surah", "page", "juz", "hizb"
    val browseMode: StateFlow<String> = _browseMode.asStateFlow()

    init {
        loadChapters()
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
}
