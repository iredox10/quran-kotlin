package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nur.quran.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShareVerseViewModel @Inject constructor(private val repository: QuranRepository) : ViewModel() {
    suspend fun translation(edition: Int, verseKey: String): String? = repository.getShareTranslation(edition, verseKey)
}
