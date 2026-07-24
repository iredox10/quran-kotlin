package com.nur.quran.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.viewmodels.SurahUiState
import com.nur.quran.ui.viewmodels.SurahViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HifdhReaderScreen(
    surahViewModel: SurahViewModel,
    chapterId: Int,
    onBackClick: () -> Unit
) {
    val uiState by surahViewModel.uiState.collectAsState()
    val memorizedAyahs by surahViewModel.memorizedAyahs.collectAsState()
    val isPlaying by surahViewModel.isPlaying.collectAsState()
    val collections by surahViewModel.collections.collectAsState()
    val bookmarkedVerses by surahViewModel.bookmarkedVerses.collectAsState()
    val arabicFontScale by surahViewModel.arabicFontScale.collectAsState()
    val selectedArabicFontName by surahViewModel.selectedArabicFontName.collectAsState(initial = "Scheherazade New")

    val fontFamilyArabic = remember(selectedArabicFontName) {
        getArabicFontFamily(selectedArabicFontName)
    }

    var currentVerseIndex by remember { mutableStateOf(0) }
    var ayahsPerChunk by remember { mutableStateOf(1) } // 1, 3, 5, 10
    var hideMode by remember { mutableStateOf("visible") } // "visible", "blur", "word", "firstletter"
    var showTranslation by remember { mutableStateOf(false) }
    var revealedWords by remember { mutableStateOf(setOf<String>()) }
    var ayahRepeatCount by remember { mutableStateOf(1) }
    var delayBetweenAyahs by remember { mutableStateOf(0) }
    var showAudioSettingsPopover by remember { mutableStateOf(false) }
    var showCollectionsPopover by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var sessionSeconds by remember { mutableStateOf(0) }

    val context = LocalContext.current

    LaunchedEffect(chapterId) {
        surahViewModel.loadChapterDetails(chapterId)
    }

    DisposableEffect(chapterId) {
        surahViewModel.startReadingSession(chapterId, "memorizing")
        onDispose {
            surahViewModel.endReadingSession("memorizing")
        }
    }

    // Session Timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            sessionSeconds++
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // ── 1. Top Thin Progress Bar ──
        when (val state = uiState) {
            is SurahUiState.Success -> {
                val total = state.verses.size
                val sessionPct = if (total > 0) ((currentVerseIndex + ayahsPerChunk).toFloat() / total).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = sessionPct,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = hTeal,
                    trackColor = hBoneDark
                )
            }
            else -> {}
        }

        // ── 2. Web-Identical Top Navbar Header (Matching Header.jsx) ──
        val chapterName = (uiState as? SurahUiState.Success)?.chapter?.nameSimple ?: "Surah $chapterId"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            color = hCream,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Back Button + Brand Logo & Title Breadcrumb
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Back", tint = hInk)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                        contentDescription = "Quran Nur Logo",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quran Nur",
                        fontFamily = fontFamilyUi,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    Text(
                        text = " / ",
                        fontSize = 12.sp,
                        color = hInkMuted
                    )
                    Text(
                        text = "Hifdh Mode: $chapterName",
                        fontFamily = fontFamilyMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = hTeal
                    )
                }

                // Right Actions (Award Icon + Theme Toggle + Settings)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val versesList = (uiState as? SurahUiState.Success)?.verses ?: emptyList()
                    val isSurahFullyMemorized = versesList.isNotEmpty() && versesList.all { memorizedAyahs.contains(it.verseKey) }

                    IconButton(
                        onClick = {
                            versesList.forEach { v -> surahViewModel.toggleMemorizedAyah(v.verseKey) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Award,
                            contentDescription = "Mark Surah Memorized",
                            tint = if (isSurahFullyMemorized) hGreen else hInkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isDarkThemeGlobal = !isDarkThemeGlobal
                            context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkThemeGlobal) NurIcons.Sun else NurIcons.Moon,
                            contentDescription = "Theme",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDrawer = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Settings,
                            contentDescription = "Settings",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        when (val state = uiState) {
            is SurahUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = hGold)
                }
            }
            is SurahUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = hInkMuted)
                }
            }
            is SurahUiState.Success -> {
                val versesList = state.verses
                val chapterObj = state.chapter
                val currentVerses = remember(currentVerseIndex, ayahsPerChunk, versesList) {
                    if (versesList.isEmpty()) emptyList()
                    else versesList.subList(currentVerseIndex, (currentVerseIndex + ayahsPerChunk).coerceAtMost(versesList.size))
                }

                Box(modifier = Modifier.weight(1f)) {
                    // ── 3. Distraction-Free Verses Canvas (Matching Web Memorization.jsx) ──
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = if (currentVerses.size == 1) Arrangement.Center else Arrangement.spacedBy(32.dp)
                    ) {
                        itemsIndexed(currentVerses, key = { _, v -> v.verseKey }) { index, verse ->
                            val verseKey = verse.verseKey
                            val isMemorized = memorizedAyahs.contains(verseKey)
                            val isBookmarked = bookmarkedVerses.contains(verseKey)
                            val rawText = verse.textUthmani ?: verse.textQpcHafs ?: verse.textIndopak ?: ""
                            val cleanedText = rawText.replace(Regex("[\u06df\u06e0\u06ea\u06eb\u06ec\u25cc\u06dd]"), "")

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp)
                                ) {
                                    // Left Action Overlay (Bookmark & CheckCircle)
                                    Column(
                                        modifier = Modifier.align(Alignment.TopStart),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { surahViewModel.toggleBookmark(verseKey, chapterId, chapterObj.nameSimple) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = NurIcons.Bookmark,
                                                contentDescription = "Bookmark",
                                                tint = if (isBookmarked) hTeal else hInkMuted.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { surahViewModel.toggleMemorizedAyah(verseKey) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMemorized) NurIcons.CheckCircle2 else NurIcons.Check,
                                                contentDescription = "Mark Memorized",
                                                tint = if (isMemorized) hGreen else hInkMuted.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Centered Arabic Verse Text
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Verse Key Badge
                                        Surface(shape = RoundedCornerShape(100), color = hGoldSoft) {
                                            Text(
                                                text = verse.verseKey,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hGold,
                                                fontFamily = fontFamilyMono,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        when (hideMode) {
                                            "blur" -> {
                                                var tempRevealed by remember { mutableStateOf(false) }
                                                Text(
                                                    text = cleanedText,
                                                    fontFamily = fontFamilyArabic,
                                                    fontSize = (28 * arabicFontScale).sp,
                                                    color = if (isDarkThemeGlobal) Color(0xFFEFECE4) else Color(0xFF2B3F3C),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 48.sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .blur(if (tempRevealed) 0.dp else 12.dp)
                                                        .clickable { tempRevealed = !tempRevealed }
                                                )
                                            }
                                            "word" -> {
                                                val words = cleanedText.split(" ")
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    words.forEachIndexed { wordIdx, word ->
                                                        val wordKey = "$verseKey-$wordIdx"
                                                        val isWordRevealed = revealedWords.contains(wordKey)

                                                        Box(
                                                            modifier = Modifier
                                                                .padding(4.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isWordRevealed) Color.Transparent else hBoneDark)
                                                                .clickable {
                                                                    revealedWords = if (isWordRevealed) revealedWords - wordKey else revealedWords + wordKey
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (isWordRevealed) word else "▇".repeat(word.length.coerceAtLeast(3)),
                                                                fontFamily = fontFamilyArabic,
                                                                fontSize = (26 * arabicFontScale).sp,
                                                                color = if (isWordRevealed) hInk else hInkMuted
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            "firstletter" -> {
                                                val words = cleanedText.split(" ")
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    words.forEachIndexed { wordIdx, word ->
                                                        val wordKey = "$verseKey-$wordIdx"
                                                        val isWordRevealed = revealedWords.contains(wordKey)
                                                        val firstChar = if (word.isNotEmpty()) word.take(1) + "..." else ""

                                                        Box(
                                                            modifier = Modifier
                                                                .padding(4.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isWordRevealed) Color.Transparent else hGoldSoft)
                                                                .clickable {
                                                                    revealedWords = if (isWordRevealed) revealedWords - wordKey else revealedWords + wordKey
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (isWordRevealed) word else firstChar,
                                                                fontFamily = fontFamilyArabic,
                                                                fontSize = (26 * arabicFontScale).sp,
                                                                color = if (isWordRevealed) hInk else hGold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                Text(
                                                    text = cleanedText,
                                                    fontFamily = fontFamilyArabic,
                                                    fontSize = (28 * arabicFontScale).sp,
                                                    color = if (isDarkThemeGlobal) Color(0xFFEFECE4) else Color(0xFF2B3F3C),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 48.sp,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }

                                // Translation Card underneath verse (if enabled)
                                if (showTranslation && !verse.translation.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = hCream),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = verse.translation,
                                            fontSize = 14.sp,
                                            color = hInkMid,
                                            fontFamily = fontFamilyBody,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(140.dp)) }
                    }

                    // ── 4. Floating Popovers for Collections & Audio Settings ──
                    if (showCollectionsPopover) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                                .width(260.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = hWhite,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Add to Collection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = hInk)
                                    IconButton(onClick = { showCollectionsPopover = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (collections.isEmpty()) {
                                    Text("No collections yet.", fontSize = 11.sp, color = hInkMuted)
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                                        items(collections) { col ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        surahViewModel.addToCollection(col.id.toLong(), "$chapterId:${currentVerseIndex + 1}", chapterId, chapterObj.nameSimple)
                                                        showCollectionsPopover = false
                                                    }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = NurIcons.Bookmark, contentDescription = null, tint = hGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(col.name, fontSize = 12.sp, color = hInk)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newCollectionName,
                                        onValueChange = { newCollectionName = it },
                                        placeholder = { Text("New...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            if (newCollectionName.isNotBlank()) {
                                                surahViewModel.addCollection(newCollectionName.trim())
                                                newCollectionName = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(hTeal)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (showAudioSettingsPopover) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                                .width(240.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = hWhite,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Audio Repeat Options", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = hInk)
                                    IconButton(onClick = { showAudioSettingsPopover = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Ayah Repeat Count", fontSize = 11.sp, color = hInkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(1, 2, 3, 5, 10).forEach { r ->
                                        FilterChip(
                                            selected = ayahRepeatCount == r,
                                            onClick = { ayahRepeatCount = r },
                                            label = { Text("${r}x", fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Reciter Delay Gap", fontSize = 11.sp, color = hInkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0, 1, 2, 3, 5).forEach { d ->
                                        FilterChip(
                                            selected = delayBetweenAyahs == d,
                                            onClick = { delayBetweenAyahs = d },
                                            label = { Text("${d}s", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 5. Status Timer Line & Dock Bar Stack ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status Line (`Surah Al-Mulk · Page 562 · Ayah 67:1 · 02:45`)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            val surahName = chapterObj.nameSimple
                            val pageNum = currentVerses.firstOrNull()?.pageNumber ?: 1
                            val currentVKey = currentVerses.firstOrNull()?.verseKey ?: ""
                            val mins = sessionSeconds / 60
                            val secs = sessionSeconds % 60
                            val timeStr = "%d:%02d".format(mins, secs)

                            Text(text = "Surah $surahName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hTeal, fontFamily = fontFamilyMono)
                            Text(text = "·", fontSize = 11.sp, color = hInkMuted.copy(alpha = 0.5f))
                            Text(text = "Page $pageNum", fontSize = 11.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                            Text(text = "·", fontSize = 11.sp, color = hInkMuted.copy(alpha = 0.5f))
                            Text(text = "Ayah $currentVKey", fontSize = 11.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                            Text(text = "·", fontSize = 11.sp, color = hInkMuted.copy(alpha = 0.5f))
                            Text(text = timeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono)
                        }

                        // Floating Dock Bar (`max-w-[480px]`)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .widthIn(max = 480.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = hWhite,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Prev Chunk Button (<ChevronLeft>)
                                IconButton(
                                    onClick = {
                                        if (currentVerseIndex - ayahsPerChunk >= 0) {
                                            currentVerseIndex -= ayahsPerChunk
                                        } else {
                                            currentVerseIndex = 0
                                        }
                                        revealedWords = emptySet()
                                    },
                                    enabled = currentVerseIndex > 0,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Prev", tint = if (currentVerseIndex > 0) hInkMid else hInkMuted.copy(alpha = 0.3f))
                                }

                                // 2. Hide Mode Cycle Button (<EyeOff> / <Eye>)
                                IconButton(
                                    onClick = {
                                        hideMode = when (hideMode) {
                                            "visible" -> "blur"
                                            "blur" -> "word"
                                            "word" -> "firstletter"
                                            else -> "visible"
                                        }
                                        revealedWords = emptySet()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (hideMode != "visible") hGoldSoft else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = if (hideMode == "visible") NurIcons.Eye else NurIcons.EyeOff,
                                        contentDescription = "Hide Mode",
                                        tint = if (hideMode != "visible") hGold else hInkMid
                                    )
                                }

                                // 3. Translation Toggle Button (<Languages> / <BookOpen>)
                                IconButton(
                                    onClick = { showTranslation = !showTranslation },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (showTranslation) hGoldSoft else Color.Transparent)
                                ) {
                                    Icon(imageVector = NurIcons.BookOpen, contentDescription = "Translation", tint = if (showTranslation) hGold else hInkMid)
                                }

                                // Vertical Divider Line
                                Box(modifier = Modifier.height(22.dp).width(1.dp).background(hBoneDark))

                                // 4. Center Audio Play/Pause Circular Button (54.dp)
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) hGold else hTeal)
                                        .clickable { surahViewModel.playPauseChapter(versesList, chapterId) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Vertical Divider Line
                                Box(modifier = Modifier.height(22.dp).width(1.dp).background(hBoneDark))

                                // 5. Ayah Chunking Selector (1, 3, 5 Ayahs)
                                TextButton(
                                    onClick = {
                                        ayahsPerChunk = when (ayahsPerChunk) {
                                            1 -> 3
                                            3 -> 5
                                            5 -> 1
                                            else -> 1
                                        }
                                    },
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("${ayahsPerChunk}A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hInkMid, fontFamily = fontFamilyMono)
                                }

                                // 6. Add to Collection Button (<Bookmark>)
                                IconButton(onClick = { showCollectionsPopover = !showCollectionsPopover }, modifier = Modifier.size(38.dp)) {
                                    Icon(imageVector = NurIcons.Bookmark, contentDescription = "Add to Collection", tint = hInkMid, modifier = Modifier.size(18.dp))
                                }

                                // 7. Audio Settings Gear (<Settings>)
                                IconButton(onClick = { showAudioSettingsPopover = !showAudioSettingsPopover }, modifier = Modifier.size(38.dp)) {
                                    Icon(imageVector = NurIcons.Settings, contentDescription = "Audio Settings", tint = hInkMid, modifier = Modifier.size(18.dp))
                                }

                                // 8. Next Chunk Button (<ChevronRight>)
                                IconButton(
                                    onClick = {
                                        if (currentVerseIndex + ayahsPerChunk < versesList.size) {
                                            currentVerseIndex += ayahsPerChunk
                                        }
                                        revealedWords = emptySet()
                                    },
                                    enabled = currentVerseIndex + ayahsPerChunk < versesList.size,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(imageVector = NurIcons.ArrowRight, contentDescription = "Next", tint = if (currentVerseIndex + ayahsPerChunk < versesList.size) hInkMid else hInkMuted.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsDrawer) {
            SettingsDrawer(
                viewModel = surahViewModel,
                onDismiss = { showSettingsDrawer = false }
            )
        }
    }
}
