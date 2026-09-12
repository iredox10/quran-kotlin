package com.nur.quran.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.translation.translationNameOf
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.components.profile.*
import com.nur.quran.ui.navigation.Screen
import com.nur.quran.ui.viewmodels.AuthViewModel
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel

/**
 * ProfileScreen matching web app Profile.jsx:
 * 1. Top navigation title: "Profile"
 * 2. Premium Hero Avatar (Avatar circle + ambient glow halo + category tag + greeting/user name + subtitle)
 * 3. Sleek Goal Card (Clock icon, daily goal text, settings gear, gradient progress bar, expandable 10-60m pills)
 * 4. Quick Links Row (Bookmarks, Analytics, Planner, Memorize, Sauka)
 * 5. Reading Experience Group Card (Reading Settings, Reciter, Translation)
 * 6. Onboarding & Tours Group Card (Replay All Tours)
 * 7. Community Group Card (Invite Friends & Share App)
 * 8. App & Storage Group Card (Appearance Light/Dark toggle, Offline Library -> Downloads)
 * 9. Cloud Sync Card (Sign In / Register / Forgot Password form or Backup & Restore card)
 * 10. Danger Zone (Sign Out button in red, visible only when logged in)
 * 11. Footer ("QURAN NUR · V1.0.0", "Made with ♥ for the Ummah")
 * 12. Settings Drawer modal (when showSettingsDrawer is true)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    homeViewModel: HomeViewModel,
    surahViewModel: SurahViewModel,
    plannerViewModel: PlannerViewModel,
    onNavigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("Settings", Context.MODE_PRIVATE) }
    val hifdhPrefs = remember { context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE) }

    var isDarkTheme by remember { mutableStateOf(isDarkThemeGlobal) }
    var dailyGoalMins by remember { mutableIntStateOf(prefs.getInt("daily_reading_goal", 20)) }
    var showSettingsDrawer by remember { mutableStateOf(false) }

    val authVm: AuthViewModel = hiltViewModel()
    val authState by authVm.authState.collectAsState()

    // Hoisted so Cloud Sync and SettingsDrawer share the same packVm instance
    val packVm: PackViewModel = hiltViewModel()
    val syncUi by packVm.syncUiState.collectAsState()

    val reciterId by surahViewModel.currentReciterId.collectAsState()
    val translationId by surahViewModel.currentTranslationId.collectAsState()
    val completedTours by homeViewModel.completedTours.collectAsState()

    var syncOp by remember { mutableStateOf<String?>(null) }
    var showSyncSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(syncUi.syncing) {
        if (!syncUi.syncing && syncOp != null) {
            showSyncSuccess = syncUi.error == null
            syncOp = null
        }
    }
    LaunchedEffect(showSyncSuccess) {
        if (showSyncSuccess) {
            kotlinx.coroutines.delay(3000)
            showSyncSuccess = false
        }
    }

    val sessions by homeViewModel.readingSessions.collectAsState()
    val todayTotalSeconds = remember(sessions) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        sessions.filter { it.date == todayStr }.sumOf { it.duration }
    }

    val reciterName = remember(reciterId) {
        Reciters.nameOf(reciterId)
    }
    val translationName = remember(translationId) {
        translationNameOf(translationId)
    }

    val greeting = remember {
        ProfileUtils.getGreeting()
    }

    val syncStatusMessage = if (syncUi.syncing) {
        if (syncOp == "restore") "Restoring data..." else "Saving data..."
    } else null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = hWhite)
            )
        },
        containerColor = hWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Premium Hero
            item {
                ProfileHero(
                    signedIn = authState.signedIn,
                    email = authState.email,
                    displayName = authState.email?.substringBefore("@")?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString()
                    },
                    greeting = greeting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Sleek Daily Goal Card
            item {
                ProfileGoalCard(
                    todayTotalSeconds = todayTotalSeconds,
                    dailyGoalMins = dailyGoalMins,
                    onGoalSelected = { mins ->
                        dailyGoalMins = mins
                        prefs.edit().putInt("daily_reading_goal", mins).apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Quick Links Row
            item {
                ProfileQuickLinks(
                    onNavigateToRoute = onNavigateToScreen,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Reading Experience
            item {
                ReadingExperienceGroup(
                    reciterName = reciterName,
                    translationName = translationName,
                    onOpenSettings = { showSettingsDrawer = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Onboarding & Tours
            item {
                OnboardingGroup(
                    completedToursCount = completedTours.size,
                    onReplayTours = {
                        hifdhPrefs.edit()
                            .remove("has_seen_surah_tour")
                            .remove("has_seen_swipe_tip")
                            .apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Community
            item {
                CommunityGroup(
                    onShareApp = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Read & study Quran with Quran Nur 🌙: https://quran-nur.appwrite.network"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 7. App & Storage
            item {
                AppStorageGroup(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        val newDark = !isDarkThemeGlobal
                        isDarkThemeGlobal = newDark
                        isDarkTheme = newDark
                        prefs.edit().putBoolean("is_dark_theme", newDark).apply()
                    },
                    onNavigateToDownloads = {
                        onNavigateToScreen(Screen.Downloads.route)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 8. Cloud Sync
            item {
                ProfileCloudSyncCard(
                    signedIn = authState.signedIn,
                    busy = authState.busy,
                    error = authState.error,
                    message = authState.message,
                    lastSyncAt = syncUi.lastSync,
                    isSyncing = syncUi.syncing,
                    syncStatusMessage = syncStatusMessage,
                    isSyncSuccess = showSyncSuccess,
                    isSyncError = syncUi.error != null,
                    onLogin = authVm::login,
                    onRegister = authVm::register,
                    onForgot = authVm::sendRecovery,
                    onBackup = {
                        syncOp = "backup"
                        showSyncSuccess = false
                        packVm.backupNow()
                    },
                    onRestore = {
                        syncOp = "restore"
                        showSyncSuccess = false
                        packVm.restoreNow()
                    },
                    onClearMessage = authVm::clearMessage,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 9. Danger Zone (signed in only)
            if (authState.signedIn) {
                item {
                    ProfileDangerZone(
                        onLogout = authVm::logout,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 10. Footer
            item {
                ProfileFooter(modifier = Modifier.fillMaxWidth())
            }
        }

        if (showSettingsDrawer) {
            val tafsirPackRows by packVm.tafsirPacks.collectAsState()
            val wordCached by packVm.wordCachedCount.collectAsState()
            val wordDownloading by packVm.wordIsDownloading.collectAsState()
            val wordProgress by packVm.wordProgressByTafsir.collectAsState()
            val syncState by packVm.syncUiState.collectAsState()
            SettingsDrawer(
                viewModel = surahViewModel,
                onDismiss = { showSettingsDrawer = false },
                tafsirPacks = tafsirPackRows,
                onDownloadTafsir = packVm::downloadTafsirPack,
                onCancelTafsir = packVm::cancelTafsirPack,
                onDeleteTafsir = packVm::deleteTafsirPack,
                translationPacks = packVm.translationPacks.collectAsState().value,
                onDownloadTranslation = packVm::downloadTranslationPack,
                onCancelTranslation = packVm::cancelTranslationPack,
                onDeleteTranslation = packVm::deleteTranslationPack,
                wordCachedCount = wordCached,
                wordIsDownloading = wordDownloading,
                onDownloadAllWords = packVm::downloadAllMissingWordPacks,
                onCancelAllWords = packVm::cancelAllWordPacks,
                completedEvents = packVm.completedEvents,
                syncState = syncState,
                onBackup = packVm::backupNow,
                onRestore = packVm::restoreNow,
                wordProgressByTafsir = wordProgress
            )
        }
    }
}
