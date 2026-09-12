package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: QuranRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.getAllBookmarksFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val collectionsWithItems: StateFlow<List<QuranRepository.CollectionWithItems>> = repository.getCollectionsWithItemsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteBookmark(verseKey: String) {
        viewModelScope.launch {
            repository.deleteBookmark(verseKey)
        }
    }

    fun toggleBookmark(verseKey: String, chapterId: Int = 1, surahName: String = "") {
        viewModelScope.launch {
            val isBookmarked = bookmarks.value.any { it.verseKey == verseKey }
            if (isBookmarked) {
                repository.deleteBookmark(verseKey)
            } else {
                repository.addBookmark(
                    BookmarkEntity(
                        id = verseKey.hashCode(),
                        verseKey = verseKey,
                        chapterId = chapterId,
                        surahName = surahName,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun addCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCollection(name.trim())
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
        }
    }

    fun removeFromCollection(collectionId: Long, verseKey: String) {
        viewModelScope.launch {
            repository.removeFromCollection(collectionId, verseKey)
        }
    }
}
