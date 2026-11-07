package com.nur.quran.ui.components

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel

/** Shared app settings, including real offline-pack and sync actions. */
data class AppSettingsModels(val reader: SurahViewModel, val packs: PackViewModel)
val LocalAppSettingsModels = staticCompositionLocalOf<AppSettingsModels?> { null }

@Composable
internal fun AppSettingsSection(onDismiss: () -> Unit, readerControls: (@Composable () -> Unit)? = null) {
    val models = LocalAppSettingsModels.current
    val reader = models?.reader ?: hiltViewModel<SurahViewModel>()
    val packs = models?.packs ?: hiltViewModel<PackViewModel>()
    SettingsDrawer(
        viewModel = reader, onDismiss = onDismiss,
        tafsirPacks = packs.tafsirPacks.collectAsState().value,
        onDownloadTafsir = packs::downloadTafsirPack, onCancelTafsir = packs::cancelTafsirPack,
        onDeleteTafsir = packs::deleteTafsirPack,
        translationPacks = packs.translationPacks.collectAsState().value,
        onDownloadTranslation = packs::downloadTranslationPack, onCancelTranslation = packs::cancelTranslationPack,
        onDeleteTranslation = packs::deleteTranslationPack,
        wordCachedCount = packs.wordCachedCount.collectAsState().value,
        wordIsDownloading = packs.wordIsDownloading.collectAsState().value,
        wordProgressByTafsir = packs.wordProgressByTafsir.collectAsState().value,
        onDownloadAllWords = packs::downloadAllMissingWordPacks, onCancelAllWords = packs::cancelAllWordPacks,
        completedEvents = packs.completedEvents,
        syncState = packs.syncUiState.collectAsState().value,
        onBackup = packs::backupNow, onRestore = packs::restoreNow,
        readerControls = readerControls
    )
}
