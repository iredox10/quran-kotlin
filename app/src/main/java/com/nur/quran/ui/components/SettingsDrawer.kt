package com.nur.quran.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nur.quran.data.audio.NetworkPolicy
import com.nur.quran.data.translation.TRANSLATION_EDITIONS
import com.nur.quran.data.translation.translationNameOf
import com.nur.quran.ui.components.audio.DEFAULT_TAFSIR_PACKS
import com.nur.quran.ui.components.audio.TranslationPickerRow
import com.nur.quran.ui.components.audio.ReciterLibraryPanel
import com.nur.quran.ui.components.audio.PackDownloadRow
import com.nur.quran.ui.components.audio.PackUiState
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.viewmodels.SurahViewModel

private val MUSHAF_PRESETS = com.nur.quran.data.mushaf.Mushaf.ALL.map {
    it.id to "${it.name} (${if (it.id == "indopak") "16 lines" else "15 lines"})"
}

private val RECITERS_LIST = listOf(
    7 to "Mishari Rashid al-`Afasy",
    1 to "AbdulBaset AbdulSamad",
    2 to "Abdur-Rahman as-Sudais",
    3 to "Abu Bakr al-Shatri",
    4 to "Hani ar-Rifai",
    5 to "Mahmoud Khalil Al-Husary",
    6 to "Al-Minshawi"
)

private val TAFSIRS_LIST = listOf(
    169 to "Ibn Kathir (Abridged)",
    168 to "Ma'arif al-Qur'an",
    817 to "Tazkirul Quran",
    16 to "Tafsir al-Muyassar"
)

private data class FontOption(val id: String, val name: String, val fontFamily: FontFamily)

private val ARABIC_FONT_OPTIONS = listOf(
    FontOption("kfgqpc-hafs", "KFGQPC Hafs", fontKfgqpcHafs),
    FontOption("uthman-taha-naskh", "Uthman Taha Naskh", fontUthmanTahaNaskh),
    FontOption("amiri-quran", "Amiri Quran", fontAmiri),
    FontOption("noto-naskh-arabic", "Noto Naskh Arabic", fontNoto),
    FontOption("scheherazade-new", "Scheherazade New", fontScheherazade)
)

@Composable
fun SettingsDrawer(
    viewModel: SurahViewModel,
    onDismiss: () -> Unit,
    onLinkTree: ((Uri) -> Unit)? = null,
    linkedSummary: String? = null,
    tafsirPacks: List<PackUiState> = emptyList(),
    onDownloadTafsir: (Int) -> Unit = {},
    onCancelTafsir: (Int) -> Unit = {},
    onDeleteTafsir: (Int) -> Unit = {},
    translationPacks: List<PackUiState> = emptyList(),
    onDownloadTranslation: (Int) -> Unit = {},
    onCancelTranslation: (Int) -> Unit = {},
    onDeleteTranslation: (Int) -> Unit = {},
    wordCachedCount: Int = 0,
    wordTotal: Int = 114,
    wordIsDownloading: Boolean = false,
    onDownloadAllWords: () -> Unit = {},
    onCancelAllWords: () -> Unit = {},
    wordProgressByTafsir: Map<Int, Pair<Int, Int>> = emptyMap(),
    wifiOnly: Boolean = true,
    onWifiOnlyChange: (Boolean) -> Unit = {},
    syncState: SyncUiState? = null,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val arabicFontScale by viewModel.arabicFontScale.collectAsState()
    val translationFontScale by viewModel.translationFontScale.collectAsState()
    val selectedArabicFontName by viewModel.selectedArabicFontName.collectAsState()
    val activeTranslationId by viewModel.currentTranslationId.collectAsState()
    val currentReciterId by viewModel.currentReciterId.collectAsState()
    val wordTapBehavior by viewModel.wordTapBehavior.collectAsState()
    val mushafPreset by viewModel.mushafPreset.collectAsState()
    val currentMushaf = remember(mushafPreset) {
        com.nur.quran.data.mushaf.Mushaf.fromId(mushafPreset)
    }
    val isTajweedEffective by viewModel.isTajweedEffective.collectAsState()

    var activeTab by remember { mutableStateOf("general") }
    var activeSubView by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // WiFi-only policy is owned here via NetworkPolicy so every host gets
    // live state without extra wiring; hosts are still notified via callback.
    val policyContext = LocalContext.current
    var wifiPolicy by remember { mutableStateOf(NetworkPolicy.isWifiOnly(policyContext)) }

    // Link-in-place folder picker (GreenTech/quran_android layout).
    // Registered unconditionally so composition stays stable; the Link
    // button itself only renders when [onLinkTree] is provided.
    val linkTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onLinkTree?.invoke(uri)
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Backdrop blur overlay matching web app
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onDismiss() }
                )
            }

            // Right-side sliding drawer panel matching web app (`aside className="fixed inset-y-0 right-0 max-w-[380px]"`)
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth },
                exit = slideOutHorizontally(animationSpec = tween(250)) { fullWidth -> fullWidth },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.85f),
                    color = hSurface,
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header matching web app SettingsDrawer header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(hSurface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (activeSubView != null) {
                                    IconButton(
                                        onClick = { activeSubView = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = NurIcons.ArrowLeft,
                                            contentDescription = "Back",
                                            tint = hInk
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column {
                                    Text(
                                        text = when (activeSubView) {
                                            "mushaf" -> "Choose Mushaf"
                                            "translation" -> "Choose Translation"
                                            "reciters" -> "Reciters"
                                            "font" -> "Choose Arabic Font"
                                            "tafsir" -> "Choose Tafsir"
                                            else -> "Settings"
                                        },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi
                                    )
                                    if (activeSubView == null) {
                                        Text(
                                            text = "Customize your reading experience",
                                            fontSize = 11.sp,
                                            color = hInkMuted,
                                            fontFamily = fontFamilyBody
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 18.sp,
                                    color = hInkMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = hBoneDark)

                        if (activeSubView == null) {
                            // 3-Segmented Tab Bar (General / Reading / Data) matching web app
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(hCream)
                                    .border(1.dp, hBoneDark, RoundedCornerShape(12.dp))
                            ) {
                                listOf("general" to "General", "reading" to "Reading", "data" to "Data").forEach { (tabKey, label) ->
                                    val isSelected = activeTab == tabKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) hGold else Color.Transparent)
                                            .clickable { activeTab = tabKey },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else hInkMid
                                        )
                                    }
                                }
                            }

                            // Drawer Body Content Scrollable Area
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                when (activeTab) {
                                    "general" -> {
                                        // Appearance Section
                                        Text(
                                            text = "APPEARANCE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(hCream)
                                                .border(1.dp, hBoneDark, RoundedCornerShape(12.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (!isDarkThemeGlobal) hGold else Color.Transparent)
                                                    .clickable { isDarkThemeGlobal = false },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = NurIcons.Sun, contentDescription = null, tint = if (!isDarkThemeGlobal) Color.White else hInkMid, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Light", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isDarkThemeGlobal) Color.White else hInkMid)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isDarkThemeGlobal) hGold else Color.Transparent)
                                                    .clickable { isDarkThemeGlobal = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = NurIcons.Moon, contentDescription = null, tint = if (isDarkThemeGlobal) Color.White else hInkMid, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Dark", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkThemeGlobal) Color.White else hInkMid)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Essentials Section
                                        Text(
                                            text = "ESSENTIALS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            Column {
                                                SettingsRowItem(
                                                    label = "Mushaf",
                                                    value = MUSHAF_PRESETS.find { it.first == mushafPreset }?.second ?: "Uthmani",
                                                    onClick = { activeSubView = "mushaf" }
                                                )
                                                Divider(color = hBoneDark)
                                                SettingsRowItem(
                                                    label = "Translation",
                                                    value = translationNameOf(activeTranslationId),
                                                    onClick = { activeSubView = "translation" }
                                                )
                                                Divider(color = hBoneDark)
                                                SettingsRowItem(
                                                    label = "Reciter",
                                                    value = RECITERS_LIST.find { it.first == currentReciterId }?.second ?: "Mishari Rashid al-Afasy",
                                                    onClick = { activeSubView = "reciters" }
                                                )
                                            }
                                        }
                                    }

                                    "reading" -> {
                                        // Text Preferences
                                        Text(
                                            text = "TEXT PREFERENCES",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            Column {
                                                SettingsRowItem(
                                                    label = "Arabic Font",
                                                    value = selectedArabicFontName,
                                                    onClick = { activeSubView = "font" }
                                                )
                                                Divider(color = hBoneDark)
                                                SettingsRowItem(
                                                    label = "Tafsir",
                                                    value = "Ibn Kathir",
                                                    onClick = { activeSubView = "tafsir" }
                                                )
                                                Divider(color = hBoneDark)
                                                SettingsToggleItem(
                                                    label = if (currentMushaf.supportsTajweedToggle) "Tajweed"
                                                        else "Tajweed (Not available for IndoPak)",
                                                    checked = isTajweedEffective,
                                                    enabled = currentMushaf.supportsTajweedToggle,
                                                    onToggle = { viewModel.toggleTajweed() }
                                                )
                                                Divider(color = hBoneDark)

                                                // Word Hover/Tap Action Selector matching web app
                                                Column(modifier = Modifier.padding(14.dp)) {                                                    Text(
                                                        text = "Word Hover Action",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = hInk
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(hWhite)
                                                            .border(1.dp, hBoneDark, RoundedCornerShape(8.dp))
                                                    ) {
                                                        listOf("none" to "None", "translation" to "Translation", "tajweed" to "Tajweed").forEach { (actionKey, label) ->
                                                            val isSelected = wordTapBehavior == actionKey
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(32.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isSelected) hGold else Color.Transparent)
                                                                    .clickable { viewModel.setWordTapBehavior(actionKey) },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = label,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSelected) Color.White else hInkMid
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Divider(color = hBoneDark)
                                                // Word-by-word translations for offline tap (moved from Data tab).
                                                WordTranslationsRow(
                                                    cachedCount = wordCachedCount,
                                                    totalCount = wordTotal,
                                                    isDownloading = wordIsDownloading,
                                                    onDownloadClick = onDownloadAllWords,
                                                    onCancelClick = onCancelAllWords
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Sizes Section matching web app sliders
                                        Text(
                                            text = "SIZES",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Arabic Size", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = hInk)
                                                    Text("${(arabicFontScale * 100).toInt()}%", fontSize = 12.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                                                }
                                                Slider(
                                                    value = arabicFontScale,
                                                    onValueChange = { viewModel.updateArabicFontScale(it - arabicFontScale) },
                                                    valueRange = 0.7f..2.0f,
                                                    colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Translation Size", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = hInk)
                                                    Text("${(translationFontScale * 100).toInt()}%", fontSize = 12.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                                                }
                                                Slider(
                                                    value = translationFontScale,
                                                    onValueChange = { viewModel.updateTranslationFontScale(it - translationFontScale) },
                                                    valueRange = 0.7f..2.0f,
                                                    colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold)
                                                )
                                            }
                                        }
                                    }

                                    "data" -> {
                                        Text(
                                            text = "OFFLINE DATA",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(imageVector = NurIcons.Bookmark, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Offline Library", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = hInk)
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(100),
                                                        color = hGreen.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Offline First",
                                                            color = hGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "All Quran texts, translations, and audio files are stored locally for fast offline access.",
                                                    fontSize = 12.sp,
                                                    color = hInkMid,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Download policy: auto-cache/packs wait for WiFi.
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            SettingsToggleItem(
                                                label = "WiFi-only downloads",
                                                subtitle = "Auto-cache and packs wait for unmetered WiFi; streaming unaffected",
                                                checked = wifiPolicy,
                                                onToggle = {
                                                    val next = !wifiPolicy
                                                    NetworkPolicy.setWifiOnly(policyContext, next)
                                                    wifiPolicy = next
                                                    onWifiOnlyChange(next)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Linked Audio Section (link-in-place, no re-download)
                                        Text(
                                            text = "LINKED AUDIO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(imageVector = NurIcons.Volume2, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Linked Audio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = hInk)
                                                    }
                                                    if (linkedSummary != null) {
                                                        Surface(
                                                            shape = RoundedCornerShape(100),
                                                            color = hGreen.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = linkedSummary,
                                                                color = hGreen,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Link GreenTech/quran_android folders (001..114.mp3 + timings.db) — no re-download.",
                                                    fontSize = 12.sp,
                                                    color = hInkMid,
                                                    lineHeight = 18.sp
                                                )
                                                if (onLinkTree != null) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Button(
                                                        onClick = { linkTreeLauncher.launch(null) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = hGold)
                                                    ) {
                                                        Text(
                                                            text = "Link folder",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Backup & Sync Section (cloud backup, sign-in gated)
                                        Text(
                                            text = "BACKUP & SYNC",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                        if (syncState == null) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = hCream),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
                                            ) {
                                                Text(
                                                    text = "Sign in from Profile to enable cloud backup.",
                                                    fontSize = 12.sp,
                                                    color = hInkMuted,
                                                    lineHeight = 18.sp,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }
                                        } else {
                                            SyncStatusCard(syncState, onBackup, onRestore, onLoginClick)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        } else {
                            // Sub-View Selection List matching web app pickers
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                when (activeSubView) {
                                    "mushaf" -> {
                                        MUSHAF_PRESETS.forEach { (presetKey, title) ->
                                            PickerItemRow(
                                                title = title,
                                                selected = mushafPreset == presetKey,
                                                onSelect = {
                                                    viewModel.setMushafPreset(presetKey)
                                                    activeSubView = null
                                                }
                                            )
                                        }
                                    }
                                    "translation" -> {
                                        var translationQuery by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = translationQuery,
                                            onValueChange = { translationQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = {
                                                Text(
                                                    text = "Search translations…",
                                                    color = hInkMuted,
                                                    fontFamily = fontFamilyBody,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        val translationFilter = translationQuery.trim().lowercase()
                                        val filteredEditions = if (translationFilter.isEmpty()) {
                                            TRANSLATION_EDITIONS
                                        } else {
                                            TRANSLATION_EDITIONS.filter {
                                                it.name.lowercase().contains(translationFilter) ||
                                                    it.language.lowercase().contains(translationFilter)
                                            }
                                        }
                                        val packByTranslationId = translationPacks.associateBy { it.id }
                                        filteredEditions.groupBy { it.language }.toSortedMap().forEach { (language, editions) ->
                                            Text(
                                                text = language.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hInkMuted,
                                                letterSpacing = 1.sp,
                                                fontFamily = fontFamilyMono,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                            )
                                            editions.sortedBy { it.name }.forEach { edition ->
                                                TranslationPickerRow(
                                                    title = edition.name,
                                                    subtitle = edition.language,
                                                    selected = activeTranslationId == edition.id,
                                                    pack = packByTranslationId[edition.id],
                                                    onSelect = {
                                                        viewModel.setTranslationId(edition.id)
                                                        activeSubView = null
                                                    },
                                                    onDownload = { onDownloadTranslation(edition.id) },
                                                    onCancel = { onCancelTranslation(edition.id) },
                                                    onDelete = { onDeleteTranslation(edition.id) }
                                                )
                                            }
                                        }
                                    }
                                    "reciters" -> {
                                        ReciterLibraryPanel(
                                            selectedReciterId = currentReciterId,
                                            onSelectReciter = {
                                                viewModel.setReciterId(it)
                                                activeSubView = null
                                            }
                                        )
                                    }
                                    "font" -> {
                                        ARABIC_FONT_OPTIONS.forEach { font ->
                                            val isSelected = selectedArabicFontName == font.name
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) hGoldSoft else Color.Transparent)
                                                    .clickable {
                                                        viewModel.setSelectedArabicFontName(font.name)
                                                        activeSubView = null
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = font.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = if (isSelected) hGold else hInk
                                                        )
                                                        Text(
                                                            text = if (currentMushaf.supportedFontIds.contains(
                                                                    com.nur.quran.data.mushaf.fontNameToId(font.name)
                                                                )
                                                            ) "Compatible with ${currentMushaf.name}"
                                                            else "Not recommended for ${currentMushaf.name}",
                                                            fontSize = 11.sp,
                                                            color = hInkMuted
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(imageVector = NurIcons.Check, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                // Sample Arabic text rendered in the option's font family (matching web app sampleStyle)
                                                Text(
                                                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                                    fontSize = 20.sp,
                                                    fontFamily = font.fontFamily,
                                                    color = hGold,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                            Divider(color = hBoneDark)
                                        }
                                    }
                                    "tafsir" -> {
                                        val packById = tafsirPacks.associateBy { it.id }
                                        TAFSIRS_LIST.forEach { (tId, title) ->
                                            TafsirPickerRow(
                                                title = title,
                                                selected = tId == 169,
                                                pack = packById[tId],
                                                onSelect = {
                                                    viewModel.setTafsirId(tId)
                                                    activeSubView = null
                                                },
                                                onDownload = { onDownloadTafsir(tId) },
                                                onCancel = { onCancelTafsir(tId) },
                                                onDelete = { onDeleteTafsir(tId) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRowItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = hInk)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 12.sp, color = hInkMuted)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = NurIcons.ChevronDown, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) hInk else hInkMuted
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = hInkMuted,
                    lineHeight = 15.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = hGold)
        )
    }
}

@Composable
private fun PickerItemRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) hGoldSoft else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) hGold else hInk
        )
        if (selected) {
            Icon(imageVector = NurIcons.Check, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
        }
    }
    Divider(color = hBoneDark)
}

/**
 * Word-by-word translations row (Reading → Text Preferences). Same icon
 * language as [TafsirPickerRow]: download arrow when idle, determinate
 * spinner + cancel while fetching, green check once all 114 chapters are
 * offline for tap-to-translate.
 */
@Composable
private fun WordTranslationsRow(
    cachedCount: Int,
    totalCount: Int,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isComplete = totalCount > 0 && cachedCount >= totalCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Word Translations", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = hInk)
            Text(
                text = if (isComplete) "Downloaded • $cachedCount/$totalCount chapters"
                else "$cachedCount/$totalCount chapters for offline tap",
                fontSize = 11.sp,
                color = hInkMuted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                isDownloading -> {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = hGold)
                    IconButton(onClick = onCancelClick, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = NurIcons.X, contentDescription = "Cancel download", tint = hInkMuted, modifier = Modifier.size(16.dp))
                    }
                }
                isComplete -> {
                    Icon(imageVector = NurIcons.CheckCircle2, contentDescription = "Downloaded for offline", tint = hGreen, modifier = Modifier.size(18.dp))
                }
                else -> {
                    IconButton(onClick = onDownloadClick, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = NurIcons.Download, contentDescription = "Download for offline", tint = hGold, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
    if (isDownloading && totalCount > 0) {
        LinearProgressIndicator(
            progress = (cachedCount.toFloat() / totalCount).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = hGold,
            trackColor = hBoneDark
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Tafsir picker row with inline offline-download controls (web: Reading →
 * Tafsir list). Icon states follow platform convention: download arrow when
 * idle, determinate spinner + cancel while fetching, green check + delete
 * once fully offline (pack includes word-by-word translations).
 */
@Composable
private fun TafsirPickerRow(
    title: String,
    selected: Boolean,
    pack: PackUiState?,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) hGoldSoft else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) hGold else hInk,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(imageVector = NurIcons.Check, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                val downloaded = pack?.downloaded ?: 0
                val total = pack?.total ?: 114
                val isDownloaded = pack != null && total > 0 && downloaded >= total
                when {
                    pack?.isDownloading == true -> {
                        if (total > 0) {
                            Text(
                                text = "$downloaded/$total",
                                fontSize = 11.sp,
                                color = hInkMuted,
                                fontFamily = fontFamilyMono
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = hGold
                        )
                        IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = NurIcons.X, contentDescription = "Cancel download", tint = hInkMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    isDownloaded -> {
                        Icon(imageVector = NurIcons.CheckCircle2, contentDescription = "Downloaded for offline", tint = hGreen, modifier = Modifier.size(18.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = NurIcons.X, contentDescription = "Delete download", tint = hInkMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    else -> {
                        IconButton(onClick = onDownload, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = NurIcons.Download, contentDescription = "Download for offline", tint = hGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        if (pack?.isDownloading == true && (pack.total) > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (pack.downloaded.toFloat() / pack.total).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = hGold,
                trackColor = hBoneDark
            )
        }
    }
    Divider(color = hBoneDark)
}
