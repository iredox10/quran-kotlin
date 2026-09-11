package com.nur.quran.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.translation.translationNameOf
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.navigation.Screen
import com.nur.quran.ui.viewmodels.AuthViewModel
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import java.util.Calendar

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
    var showGoalPicker by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }

    val authVm: AuthViewModel = hiltViewModel()
    val authState by authVm.authState.collectAsState()

    // Hoisted so the Backup & Restore card and SettingsDrawer share one instance.
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

    // Logged-in identity derived from email (AuthState exposes no name field).
    val signedInEmail = authState.email

    val sessions by homeViewModel.readingSessions.collectAsState()
    val todayTotalSeconds = remember(sessions) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        sessions.filter { it.date == todayStr }.sumOf { it.duration }
    }
    val todayTotalMins = Math.round(todayTotalSeconds / 60.0).toInt()
    val goalPct = if (dailyGoalMins > 0) ((todayTotalMins.toFloat() / dailyGoalMins.toFloat()) * 100f).coerceAtMost(100f).toInt() else 0

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val heroTitle = if (authState.signedIn) {
        signedInEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
            ?: "Quran Student"
    } else greeting
    val heroSubtitle = if (authState.signedIn) signedInEmail ?: "" else "Your personal Quran companion"
    val heroInitials = if (authState.signedIn) {
        signedInEmail?.substringBefore("@")?.filter { it.isLetterOrDigit() }?.take(2)?.uppercase(java.util.Locale.US)?.takeIf { it.isNotBlank() } ?: "?"
    } else null

    val goalOptions = listOf(10, 15, 20, 30, 45, 60)

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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Auth Card
            item {
                AuthCard(
                    signedIn = authState.signedIn,
                    email = authState.email,
                    busy = authState.busy,
                    error = authState.error,
                    message = authState.message,
                    onLogin = authVm::login,
                    onRegister = authVm::register,
                    onLogout = authVm::logout,
                    onForgot = authVm::sendRecovery,
                    onClearMessage = authVm::clearMessage
                )
            }

            // Hero Avatar Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(hGoldLight)
                            .border(2.dp, hGold.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (heroInitials != null) {
                            Text(
                                text = heroInitials,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamilyUi,
                                color = hInk
                            )
                        } else {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                                contentDescription = "Quran Nur Logo",
                                modifier = Modifier.size(58.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "YOUR PROFILE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.2.sp,
                        color = hInkMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = heroTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hInk
                    )
                    Text(
                        text = heroSubtitle,
                        fontSize = 13.sp,
                        color = hInkMuted
                    )
                }
            }

            // Daily Goal Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = hSurface),
                    border = BorderStroke(1.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(hGoldLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Clock,
                                        contentDescription = null,
                                        tint = hGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Daily Reading Goal",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi
                                    )
                                    Text(
                                        text = "${todayTotalMins}m / ${dailyGoalMins}m today",
                                        fontSize = 12.sp,
                                        color = hInkMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showGoalPicker = !showGoalPicker },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(hWhite)
                                    .border(1.dp, hBoneDark, CircleShape)
                            ) {
                                Icon(
                                    imageVector = NurIcons.Settings,
                                    contentDescription = "Set Goal",
                                    tint = hInkMid,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = (goalPct.toFloat() / 100f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = hGold,
                            trackColor = hBorderColor
                        )

                        AnimatedVisibility(visible = showGoalPicker) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Divider(color = hBoneDark, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "SELECT DAILY GOAL (MINUTES)",
                                    fontSize = 10.sp,
                                    fontFamily = fontFamilyMono,
                                    fontWeight = FontWeight.Bold,
                                    color = hInkMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    goalOptions.forEach { optionMins ->
                                        val isSelected = dailyGoalMins == optionMins
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(if (isSelected) hGold else hWhite)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) hGold else hBoneDark,
                                                    CircleShape
                                                )
                                                .clickable {
                                                    dailyGoalMins = optionMins
                                                    prefs.edit().putInt("daily_reading_goal", optionMins).apply()
                                                    showGoalPicker = false
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${optionMins}m",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = fontFamilyMono,
                                                color = if (isSelected) Color.White else hInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Links Navigation Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickLinks = listOf(
                        Triple(Screen.Analytics.route, "Analytics", NurIcons.BookOpen),
                        Triple(Screen.Planner.route, "Planner", NurIcons.CalendarDays),
                        Triple(Screen.Memorize.route, "Memorize", NurIcons.Brain)
                    )
                    items(quickLinks) { (route, label, icon) ->
                        Card(
                            modifier = Modifier.clickable { onNavigateToScreen(route) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = hSurface),
                            border = BorderStroke(1.dp, hBoneDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(hGoldLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = hGold, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                            }
                        }
                    }
                }
            }

            // Reading Experience Section
            item {
                Column {
                    Text(
                        text = "READING EXPERIENCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.2.sp,
                        color = hInkMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column {
                            ProfileSettingRow(
                                icon = NurIcons.Settings,
                                label = "Reading Settings",
                                onClick = { showSettingsDrawer = true }
                            )
                            Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)
                            ProfileSettingRow(
                                icon = NurIcons.Volume2,
                                label = "Reciter Options",
                                subtitle = Reciters.nameOf(reciterId),
                                onClick = { showSettingsDrawer = true }
                            )
                            Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)
                            ProfileSettingRow(
                                icon = NurIcons.BookOpen,
                                label = "Translation Options",
                                subtitle = translationNameOf(translationId),
                                onClick = { showSettingsDrawer = true }
                            )
                            Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)
                            ProfileSettingRow(
                                icon = NurIcons.Download,
                                label = "Downloads",
                                subtitle = "Offline audio manager",
                                onClick = { onNavigateToScreen(Screen.Downloads.route) }
                            )
                        }
                    }
                }
            }

            // App Appearance & Community Section
            item {
                Column {
                    Text(
                        text = "APP & COMMUNITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.2.sp,
                        color = hInkMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column {
                            // Theme toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isDarkThemeGlobal = !isDarkThemeGlobal
                                        isDarkTheme = isDarkThemeGlobal
                                        prefs.edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(hGoldLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) NurIcons.Sun else NurIcons.Moon,
                                            contentDescription = null,
                                            tint = hGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Appearance",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk
                                    )
                                }
                                Text(
                                    text = if (isDarkTheme) "Dark" else "Light",
                                    fontSize = 12.sp,
                                    fontFamily = fontFamilyMono,
                                    fontWeight = FontWeight.Bold,
                                    color = hGold
                                )
                            }

                            Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)

                            // Reset Tours
                            ProfileSettingRow(
                                icon = NurIcons.RotateCcw,
                                label = "Replay All Tours & Tips",
                                subtitle = "${minOf(completedTours.size, 5)}/5 Completed",
                                onClick = {
                                    hifdhPrefs.edit().remove("has_seen_surah_tour").remove("has_seen_swipe_tip").apply()
                                }
                            )

                            Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)

                            // Share App
                            ProfileSettingRow(
                                icon = NurIcons.Share2,
                                label = "Invite Friends & Share App",
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Read & study Quran with Quran Nur 🌙: https://quran-nur.appwrite.network")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                                }
                            )
                        }
                    }
                }
            }

            // Backup & Restore (logged in only)
            if (authState.signedIn) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(hGoldLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = NurIcons.RefreshCw,
                                        contentDescription = null,
                                        tint = hGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Backup & Restore",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi
                                    )
                                    Text(
                                        text = "LAST SYNCED: ${relativeTime(syncUi.lastSync)}",
                                        fontSize = 10.sp,
                                        fontFamily = fontFamilyMono,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = hInkMuted
                                    )
                                }
                            }

                            if (syncUi.syncing || syncUi.error != null || showSyncSuccess) {
                                Spacer(modifier = Modifier.height(12.dp))
                                when {
                                    syncUi.syncing -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = hGold,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (syncOp == "restore") "Restoring data..." else "Saving data...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hGold
                                        )
                                    }
                                    syncUi.error != null -> Text(
                                        text = syncUi.error ?: "Sync failed",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hRed
                                    )
                                    showSyncSuccess -> Text(
                                        text = "✓ Sync complete successfully.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { syncOp = "restore"; showSyncSuccess = false; packVm.restoreNow() },
                                    enabled = !syncUi.syncing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { syncOp = "backup"; showSyncSuccess = false; packVm.backupNow() },
                                    enabled = !syncUi.syncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.RefreshCw,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Danger Zone (logged in only)
            if (authState.signedIn) {
                item {
                    Column {
                        Text(
                            text = "DANGER ZONE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamilyMono,
                            letterSpacing = 1.2.sp,
                            color = hRed.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = hSurface),
                            border = BorderStroke(1.dp, hRed.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = { authVm.logout() })
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(hRed.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = NurIcons.X,
                                            contentDescription = null,
                                            tint = hRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Sign Out",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hRed
                                    )
                                }
                                Icon(
                                    imageVector = NurIcons.ArrowRight,
                                    contentDescription = null,
                                    tint = hRed.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Footer Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QURAN NUR · V1.0.0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.2.sp,
                        color = hInkMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Made with ♥ for the Ummah",
                        fontSize = 12.sp,
                        color = hInkMuted
                    )
                }
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
                    translationPacks = packVm.translationPacks.collectAsState().value, onDownloadTranslation = packVm::downloadTranslationPack, onCancelTranslation = packVm::cancelTranslationPack, onDeleteTranslation = packVm::deleteTranslationPack,
                wordCachedCount = wordCached,
                wordIsDownloading = wordDownloading,
                onDownloadAllWords = packVm::downloadAllMissingWordPacks,
                    onCancelAllWords = packVm::cancelAllWordPacks,
                    syncState = syncState, onBackup = packVm::backupNow, onRestore = packVm::restoreNow,
                wordProgressByTafsir = wordProgress
            )
        }
    }
}

@Composable
private fun AuthCard(
    signedIn: Boolean,
    email: String?,
    busy: Boolean,
    error: String?,
    message: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onLogout: () -> Unit,
    onForgot: (String) -> Unit,
    onClearMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = hSurface),
        border = BorderStroke(1.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(hGoldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.User,
                        contentDescription = null,
                        tint = hGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Account",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi
                    )
                    Text(
                        text = if (signedIn) "Signed in" else "Sign in to sync",
                        fontSize = 12.sp,
                        color = hInkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (signedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (busy) {
                        CircularProgressIndicator(
                            color = hGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Button(
                        onClick = onLogout,
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = hGold)
                    ) {
                        Text("Logout", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                var emailInput by remember { mutableStateOf("") }
                var passwordInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, fontSize = 12.sp, color = hRed)
                }
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, fontSize = 12.sp, color = hGreen)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onLogin(emailInput, passwordInput) },
                        enabled = !busy && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = hGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Login", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onRegister(emailInput, passwordInput) },
                        enabled = !busy && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Register", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (busy) {
                        CircularProgressIndicator(
                            color = hGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onClearMessage()
                        onForgot(emailInput)
                    },
                    enabled = !busy && emailInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forgot password?", fontSize = 12.sp, color = hInkMid)
                }
            }
        }
    }
}

private fun relativeTime(lastSync: Long?): String {
    if (lastSync == null) return "Never"
    val diff = System.currentTimeMillis() - lastSync
    if (diff < 0) return "Just now"
    val mins = diff / 60_000L
    if (mins < 1) return "Just now"
    if (mins < 60) return "${mins}m ago"
    val hours = mins / 60
    if (hours < 24) return "${hours}h ago"
    return "${hours / 24}d ago"
}

@Composable
private fun ProfileSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(hGoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = hInkMuted
                    )
                }
            }
        }
        Icon(
            imageVector = NurIcons.ArrowRight,
            contentDescription = null,
            tint = hInkMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}
