package com.nur.quran.ui.screens


import androidx.compose.ui.res.stringResource
import com.nur.quran.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.profile.ProfileUtils
import com.nur.quran.ui.viewmodels.HomeViewModel
import java.util.Locale

/**
 * Recently Read page — web /recent parity (RecentSurahs.jsx).
 * Reading history with search, per-row resume/remove, clear-all, and the
 * reading-history stats card. Resume then opens the surah reader at the
 * exact verse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSurahsScreen(
    homeViewModel: HomeViewModel,
    onOpenSurah: (Int, String?) -> Unit,
    onBack: () -> Unit,
) {
    val items by homeViewModel.recentlyReadHistory.collectAsState()

    var query by remember { mutableStateOf("") }
    // Web parity: /recent opens with the focusable search hidden until items exist; filter anytime.
    var showClearDialog by remember { mutableStateOf(false) }

    val uniqueSurahs = remember(items) { items.map { it.chapterId }.toSet().size }
    val lastOpenedAt = items.firstOrNull()?.timestamp ?: 0L
    val trimmed = query.trim()

    val filtered = remember(items, query) {
        val q = query.trim().lowercase(Locale.US)
        if (q.isEmpty()) items
        else items.filter { item ->
            val name = (item.chapterName ?: "Surah ${item.chapterId}").lowercase(Locale.US)
            name.contains(q) ||
                q == item.chapterId.toString() ||
                q == "surah ${item.chapterId}" ||
                q == "surah${item.chapterId}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ui_recently_read_6f71b4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk,
                        fontFamily = fontFamilyUi
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = NurIcons.ArrowLeft, contentDescription = stringResource(R.string.ui_back_b52b36), tint = hInk)
                    }
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
            // Header (web "Reading History" eyebrow + h1 + subtitle)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.ui_reading_history_ac0968),
                        fontFamily = fontFamilyMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = hInkMuted
                    )
                    Text(
                        text = stringResource(R.string.ui_jump_back_into_any_surah_resume_at_the_exact_ver_b3f914),
                        fontSize = 13.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Stats + clear-all card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = hCream,
                    border = BorderStroke(1.5.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HistoryStatCell(
                                modifier = Modifier.weight(1f),
                                value = "$uniqueSurahs",
                                label = if (uniqueSurahs == 1) "SURAH" else "SURAHS"
                            )
                            HistoryStatCell(
                                modifier = Modifier.weight(1f),
                                value = if (lastOpenedAt > 0L) ProfileUtils.timeAgo(lastOpenedAt) else "—",
                                label = stringResource(R.string.ui_last_opened_ad2dec)
                            )
                        }
                        if (items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = CircleShape,
                                color = hWhite,
                                border = BorderStroke(1.5.dp, hBoneDark),
                                modifier = Modifier.clickable { showClearDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Trash2,
                                        contentDescription = null,
                                        tint = hInkMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.ui_clear_all_00c6d5),
                                        fontFamily = fontFamilyMono,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = hInkMuted
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        FilterField(query = query, onQueryChange = { query = it })
                    }
                }
            }

            // History list card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = hCream,
                    border = BorderStroke(1.5.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = NurIcons.Clock, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(13.dp))
                                Text(
                                    text = stringResource(R.string.ui_history_f6dc58),
                                    fontFamily = fontFamilyMono,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.5.sp,
                                    color = hInkMuted
                                )
                            }
                            Text(
                                text = stringResource(R.string.ui_1_s_2_s_78c505, filtered.size, if (filtered.size == 1) "entry" else "entries"),
                                fontFamily = fontFamilyMono,
                                fontSize = 10.sp,
                                color = hInkMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when {
                            items.isEmpty() -> HistoryEmptyState(
                                title = stringResource(R.string.ui_nothing_read_yet_6c71ab),
                                body = "Surahs you open will appear here so you can pick up right where you left off."
                            )
                            filtered.isEmpty() && trimmed.isNotEmpty() -> HistoryEmptyState(
                                title = "No results for \u201c$trimmed\u201d",
                                body = "Try a different surah name or number."
                            )
                            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filtered.forEach { item ->
                                    RecentSurahRow(
                                        item = item,
                                        onOpen = { onOpenSurah(item.chapterId, item.verseKey) },
                                        onRemove = { homeViewModel.removeRecentlyRead(item.chapterId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.ui_clear_reading_history_a5873f)) },
            text = { Text(stringResource(R.string.ui_your_reading_history_on_this_device_will_be_clea_d774e6)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        homeViewModel.clearRecentlyRead()
                    }
                ) { Text(stringResource(R.string.ui_clear_719ea3)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.ui_cancel_77dfd2)) }
            }
        )
    }
}

// ── Pieces (shared with ActivityHistoryScreen) ─────────────────────────

@Composable
internal fun HistoryStatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = hWhite,
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                fontFamily = fontFamilyUi
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontFamily = fontFamilyMono,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = hInkMuted
            )
        }
    }
}

@Composable
internal fun FilterField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        placeholder = {
            Text(stringResource(R.string.ui_filter_by_surah_name_a3458d), fontSize = 13.sp, color = hInkMuted)
        },
        leadingIcon = {
            Icon(imageVector = NurIcons.Search, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(16.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = NurIcons.X,
                    contentDescription = stringResource(R.string.ui_clear_filter_b667d6),
                    tint = hInkMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                )
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun HistoryEmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = NurIcons.BookOpen, contentDescription = null, tint = hTeal.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
        Text(
            text = body,
            fontSize = 13.sp,
            color = hInkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** One reading-history row: id badge, name, resume label + age, remove button (web RecentRow). */
@Composable
private fun RecentSurahRow(
    item: RecentlyReadEntity,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val resumeLabel = item.verseKey
        ?.takeIf { it.isNotBlank() }
        ?.split(":")?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
        ?.let { "Resume at verse ${it.trimStart('0')}" }
        ?: "From the beginning"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(hWhite)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(hTealSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.chapterId.toString(),
                fontWeight = FontWeight.Bold,
                color = hTeal,
                fontSize = 13.sp,
                fontFamily = fontFamilyMono
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.chapterName.ifBlank { "Surah ${item.chapterId}" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                fontFamily = fontFamilyUi,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.ui_1_s_2_s_c1aacd, resumeLabel, ProfileUtils.timeAgo(item.timestamp)),
                fontSize = 10.sp,
                color = hInkMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = NurIcons.X,
            contentDescription = stringResource(R.string.ui_remove_1_s_from_reading_history_4f75b4, item.chapterName),
            tint = hInkMuted,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(2.dp)
        )
    }
}
