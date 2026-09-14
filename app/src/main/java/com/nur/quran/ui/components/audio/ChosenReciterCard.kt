package com.nur.quran.ui.components.audio

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.audio.Reciter
import com.nur.quran.data.audio.Reciters
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontAmiri
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite
import com.nur.quran.ui.screens.isDarkThemeGlobal

/**
 * Arabic calligraphy abbreviations for canonical reciters.
 * Provides custom authentic Arabic abbreviations (e.g. "عبد" for AbdulBaset,
 * "مش" for Mishari, "سد" for Sudais, "حصر" for Husary) with intelligent fallback.
 */
object ReciterArabicInitials {
    fun getInitials(reciterId: Int, name: String = ""): String {
        return when (reciterId) {
            1 -> "عبد"       // AbdulBaset AbdulSamad
            2 -> "سد"       // Abdur-Rahman as-Sudais
            3 -> "شاط"      // Abu Bakr al-Shatri
            4 -> "رفاع"     // Hani ar-Rifai
            5, 10, 13 -> "حصر" // Mahmoud Khalil Al-Husary
            6, 15 -> "منش"   // Mohamed Siddiq Al-Minshawi
            7, 9 -> "مش"     // Mishari Rashid al-Afasy
            11, 14, 18, 19 -> "معيق" // Maher Al-Muaiqly
            12 -> "أبكر"    // Idris Abkar
            16 -> "شريم"    // Saud ash-Shuraym
            17 -> "طبل"     // Mohamed al-Tablawi
            20 -> "دسر"     // Yasser Ad-Dussary
            else -> {
                val lower = name.lowercase()
                when {
                    lower.contains("baset") || lower.contains("basit") -> "عبد"
                    lower.contains("sudais") -> "سد"
                    lower.contains("shatri") -> "شاط"
                    lower.contains("rifai") -> "رفاع"
                    lower.contains("husary") -> "حصر"
                    lower.contains("minshawi") -> "منش"
                    lower.contains("afasy") || lower.contains("mishari") -> "مش"
                    lower.contains("muaiqly") || lower.contains("muaigly") -> "معيق"
                    lower.contains("abkar") -> "أبكر"
                    lower.contains("shuraym") -> "شريم"
                    lower.contains("tablawi") -> "طبل"
                    lower.contains("dussary") || lower.contains("dosari") -> "دسر"
                    else -> name.take(2).uppercase()
                }
            }
        }
    }
}

/**
 * ChosenReciterCard:
 * Redesigned top card for the Audio Settings modal matching the user mockup:
 * - 18.dp rounded container, surface background, subtle warm border.
 * - Top header row: "CHOSEN RECITER" (uppercase, 11.sp, bold) + "Hafs an Asim" golden pill badge.
 * - Details row:
 *   - 48.dp rounded square avatar with Arabic calligraphy abbreviation and verified checkmark badge.
 *   - Reciter title, style subtitle ("Murattal • Studio Quality"), and interactive dropdown chevron.
 *   - Circular 42.dp preview play button with play/pause/loading spinner states.
 * - Integrated Reciter Picker DropdownMenu (and optional modal sheet) grouped by Murattal, Mujawwad, Muallim.
 */
@Composable
fun ChosenReciterCard(
    selectedReciterId: Int,
    onReciterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    riwayah: String = "Hafs an Asim",
    previewPlayingId: Int? = null,
    previewLoadingId: Int? = null,
    onTogglePreview: ((Int) -> Unit)? = null,
    useModalSheetPicker: Boolean = false,
) {
    val selectedReciter = remember(selectedReciterId) {
        Reciters.byId(selectedReciterId) ?: Reciters.byId(Reciters.DEFAULT_ID) ?: Reciters.ALL.first()
    }

    val selectedPreviewUrl = remember(selectedReciter.id) {
        runCatching { Reciters.buildAudioUrl(selectedReciter.id, "1:1") }.getOrNull()
    }

    // Internal preview player if caller doesn't hoist it
    var internalPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var internalPlayingId by remember { mutableIntStateOf(-1) }
    var internalLoadingId by remember { mutableIntStateOf(-1) }

    fun stopInternalPreview() {
        runCatching {
            internalPlayer?.setOnPreparedListener(null)
            internalPlayer?.setOnCompletionListener(null)
            internalPlayer?.setOnErrorListener(null)
            runCatching { internalPlayer?.stop() }
            internalPlayer?.release()
        }
        internalPlayer = null
        internalPlayingId = -1
        internalLoadingId = -1
    }

    fun toggleInternalPreview(targetId: Int) {
        if (internalPlayingId == targetId || internalLoadingId == targetId) {
            stopInternalPreview()
            return
        }
        val url = runCatching { Reciters.buildAudioUrl(targetId, "1:1") }.getOrNull() ?: return
        stopInternalPreview()
        internalLoadingId = targetId
        runCatching {
            val player = MediaPlayer()
            internalPlayer = player
            player.setDataSource(url)
            player.setOnPreparedListener {
                if (internalLoadingId == targetId) {
                    internalLoadingId = -1
                    internalPlayingId = targetId
                    runCatching { it.start() }
                } else {
                    runCatching { it.release() }
                }
            }
            player.setOnCompletionListener { stopInternalPreview() }
            player.setOnErrorListener { _, _, _ ->
                stopInternalPreview()
                true
            }
            player.prepareAsync()
        }.onFailure { stopInternalPreview() }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopInternalPreview()
        }
    }

    val effectivePreviewPlayingId = previewPlayingId ?: internalPlayingId
    val effectivePreviewLoadingId = previewLoadingId ?: internalLoadingId
    val effectivePlaying = effectivePreviewPlayingId == selectedReciter.id
    val effectiveLoading = effectivePreviewLoadingId == selectedReciter.id

    var reciterPickerOpen by remember { mutableStateOf(false) }

    // Palette tokens
    val containerBg = if (isDarkThemeGlobal) hSurface else Color.White
    val borderColor = if (isDarkThemeGlobal) Color(0xFF3E3B34) else Color(0xFFECE5D8)
    val badgeBg = if (isDarkThemeGlobal) Color(0xFF332B1B) else Color(0xFFFAF5E8)
    val badgeBorder = if (isDarkThemeGlobal) Color(0xFF4A3E26) else Color(0xFFF1E6CD)
    val badgeText = if (isDarkThemeGlobal) Color(0xFFDFB96C) else Color(0xFF9E7420)
    val avatarBg = if (isDarkThemeGlobal) Color(0xFF2A2824) else Color(0xFFF9F5EE)
    val avatarBorder = if (isDarkThemeGlobal) Color(0xFF444038) else Color(0xFFDFD5C2)
    val avatarText = if (isDarkThemeGlobal) Color(0xFFDFB96C) else Color(0xFF8C6616)
    val playBtnBg = if (isDarkThemeGlobal) Color(0xFF2E2B25) else Color(0xFFF9F5EE)
    val playBtnBorder = if (isDarkThemeGlobal) Color(0xFF454037) else Color(0xFFDFD5C2)
    val playBtnTint = if (isDarkThemeGlobal) Color(0xFFDFB96C) else Color(0xFF9E7420)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top row: "CHOSEN RECITER" & Riwayah Pill Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHOSEN RECITER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyUi
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(badgeBg)
                        .border(1.dp, badgeBorder, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = riwayah,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeText,
                        fontFamily = fontFamilyUi
                    )
                }
            }

            // ── Reciter details row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Avatar (48.dp rounded square + verified checkmark badge)
                ReciterAvatar(
                    reciterId = selectedReciter.id,
                    reciterName = selectedReciter.name,
                    avatarBg = avatarBg,
                    avatarBorder = avatarBorder,
                    avatarText = avatarText,
                    onClick = { reciterPickerOpen = true }
                )

                // Middle: Reciter Name, Subtitle, Chevron
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { reciterPickerOpen = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = selectedReciter.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedReciter.style} • Studio Quality",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = hInkMuted,
                            fontFamily = fontFamilyBody,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = NurIcons.ChevronDown,
                        contentDescription = "Select reciter",
                        tint = hInkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Right: Circular 42.dp preview play button
                ReciterPreviewCircleButton(
                    isPlaying = effectivePlaying,
                    isLoading = effectiveLoading,
                    playBtnBg = playBtnBg,
                    playBtnBorder = playBtnBorder,
                    playBtnTint = playBtnTint,
                    hasPreview = selectedPreviewUrl != null,
                    onClick = {
                        if (onTogglePreview != null) {
                            onTogglePreview(selectedReciter.id)
                        } else {
                            toggleInternalPreview(selectedReciter.id)
                        }
                    }
                )
            }
        }

        // Anchored Reciter Picker DropdownMenu
        if (!useModalSheetPicker) {
            ReciterPickerDropdown(
                expanded = reciterPickerOpen,
                onDismiss = { reciterPickerOpen = false },
                selectedReciterId = selectedReciter.id,
                onSelect = { id ->
                    onReciterSelected(id)
                    reciterPickerOpen = false
                },
                onPreview = { id ->
                    if (onTogglePreview != null) {
                        onTogglePreview(id)
                    } else {
                        toggleInternalPreview(id)
                    }
                },
                previewPlayingId = effectivePreviewPlayingId,
                previewLoadingId = effectivePreviewLoadingId,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }

    // Modal sheet picker (alternative if useModalSheetPicker is enabled)
    if (useModalSheetPicker && reciterPickerOpen) {
        ReciterPickerModalSheet(
            selectedReciterId = selectedReciter.id,
            onDismiss = { reciterPickerOpen = false },
            onSelectReciter = { id ->
                onReciterSelected(id)
                reciterPickerOpen = false
            },
            onPreviewReciter = { id ->
                if (onTogglePreview != null) {
                    onTogglePreview(id)
                } else {
                    toggleInternalPreview(id)
                }
            },
            previewPlayingId = effectivePreviewPlayingId,
            previewLoadingId = effectivePreviewLoadingId
        )
    }
}

/**
 * Reciter Avatar with authentic Arabic calligraphy initials and verified badge.
 */
@Composable
fun ReciterAvatar(
    reciterId: Int,
    reciterName: String,
    avatarBg: Color,
    avatarBorder: Color,
    avatarText: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showVerifiedBadge: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size + 4.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // 48.dp rounded square with 14.dp corner shape
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(avatarBg)
                .border(1.dp, avatarBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ReciterArabicInitials.getInitials(reciterId, reciterName),
                color = avatarText,
                fontSize = (size.value * 0.40f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontAmiri,
                textAlign = TextAlign.Center
            )
        }

        if (showVerifiedBadge) {
            // Verified checkmark badge at bottom-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(hGold)
                    .border(
                        width = 1.5.dp,
                        color = if (isDarkThemeGlobal) hSurface else Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.Check,
                    contentDescription = "Verified reciter",
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
    }
}

/**
 * Circular 42.dp preview play button with play, pause, and loading spinner states.
 */
@Composable
fun ReciterPreviewCircleButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playBtnBg: Color,
    playBtnBorder: Color,
    playBtnTint: Color,
    hasPreview: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    if (!hasPreview) {
        Spacer(modifier = modifier.size(size))
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(playBtnBg)
            .border(1.dp, playBtnBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = hGold
                )
            }
            isPlaying -> {
                Icon(
                    imageVector = NurIcons.PauseFilled,
                    contentDescription = "Pause preview",
                    tint = playBtnTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = NurIcons.PlayFilled,
                    contentDescription = "Play preview",
                    tint = playBtnTint,
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = 1.dp) // Optical centering for play triangle
                )
            }
        }
    }
}

/**
 * Reciter Picker DropdownMenu:
 * Lists all available reciters grouped by recitation style (Murattal, Mujawwad, Muallim).
 * Each row includes avatar with Arabic initials, reciter name, style, selected state, and audio sample preview.
 */
@Composable
fun ReciterPickerDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedReciterId: Int,
    onSelect: (Int) -> Unit,
    onPreview: (Int) -> Unit,
    previewPlayingId: Int,
    previewLoadingId: Int,
    modifier: Modifier = Modifier
) {
    val menuBg = if (isDarkThemeGlobal) hSurface else Color(0xFFFCFAF6)
    val grouped = remember {
        runCatching { Reciters.groupedByStyle() }.getOrDefault(emptyMap())
    }
    val styleOrder = remember {
        listOf(
            Reciters.STYLE_MURATTAL,
            Reciters.STYLE_MUJAWWAD,
            Reciters.STYLE_MUALLIM
        )
    }
    val orderedStyles = remember(grouped) {
        styleOrder.filter { grouped.containsKey(it) } +
            (grouped.keys - styleOrder.toSet()).sorted()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
            .width(320.dp)
            .heightIn(max = 380.dp)
            .background(menuBg)
    ) {
        orderedStyles.forEach { style ->
            Text(
                text = style.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = hInkMuted,
                letterSpacing = 1.sp,
                fontFamily = fontFamilyMono,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            grouped[style].orEmpty().forEach { option ->
                val isSelected = option.id == selectedReciterId
                val hasPreview = runCatching { Reciters.buildAudioUrl(option.id, "1:1") != null }.getOrDefault(false)

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Mini avatar
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkThemeGlobal) Color(0xFF2A2824) else Color(0xFFF5EFE3))
                                    .border(
                                        1.dp,
                                        if (isDarkThemeGlobal) Color(0xFF444038) else Color(0xFFDFD5C2),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ReciterArabicInitials.getInitials(option.id, option.name),
                                    color = if (isDarkThemeGlobal) Color(0xFFDFB96C) else Color(0xFF8C6616),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontAmiri
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) hGold else hInk,
                                    fontFamily = fontFamilyUi,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = option.style,
                                    fontSize = 11.sp,
                                    color = hInkMuted,
                                    fontFamily = fontFamilyMono
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = NurIcons.Check,
                                    contentDescription = "Selected",
                                    tint = hGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (hasPreview) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { onPreview(option.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        previewLoadingId == option.id -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = hGold
                                            )
                                        }
                                        previewPlayingId == option.id -> {
                                            Icon(
                                                imageVector = NurIcons.PauseFilled,
                                                contentDescription = "Pause",
                                                tint = hGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = NurIcons.PlayFilled,
                                                contentDescription = "Preview",
                                                tint = hInkMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onClick = { onSelect(option.id) }
                )
            }
        }
    }
}

/**
 * Optional ModalBottomSheet version for picking reciters with search capability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterPickerModalSheet(
    selectedReciterId: Int,
    onDismiss: () -> Unit,
    onSelectReciter: (Int) -> Unit,
    onPreviewReciter: (Int) -> Unit,
    previewPlayingId: Int,
    previewLoadingId: Int,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) Reciters.ALL
        else runCatching { Reciters.ALL.filter { it.name.lowercase().contains(q) } }.getOrDefault(Reciters.ALL)
    }
    val grouped = remember(filtered) {
        runCatching { filtered.groupBy { it.style } }.getOrDefault(emptyMap())
    }
    val styleOrder = remember {
        listOf(
            Reciters.STYLE_MURATTAL,
            Reciters.STYLE_MUJAWWAD,
            Reciters.STYLE_MUALLIM
        )
    }
    val orderedStyles = remember(grouped) {
        styleOrder.filter { grouped.containsKey(it) } +
            (grouped.keys - styleOrder.toSet()).sorted()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (isDarkThemeGlobal) hSurface else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hBorderColor)
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Reciter",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                fontFamily = fontFamilyUi
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search reciters…",
                        color = hInkMuted,
                        fontFamily = fontFamilyBody,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            orderedStyles.forEach { style ->
                Text(
                    text = style.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInkMuted,
                    letterSpacing = 1.sp,
                    fontFamily = fontFamilyMono,
                    modifier = Modifier.padding(top = 6.dp)
                )

                grouped[style].orEmpty().forEach { option ->
                    val isSelected = option.id == selectedReciterId
                    val hasPreview = runCatching { Reciters.buildAudioUrl(option.id, "1:1") != null }.getOrDefault(false)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    if (isDarkThemeGlobal) Color(0xFF332B1B) else Color(0xFFFAF5E8)
                                } else Color.Transparent
                            )
                            .clickable { onSelectReciter(option.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkThemeGlobal) Color(0xFF2A2824) else Color(0xFFF5EFE3))
                                .border(
                                    1.dp,
                                    if (isDarkThemeGlobal) Color(0xFF444038) else Color(0xFFDFD5C2),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ReciterArabicInitials.getInitials(option.id, option.name),
                                color = if (isDarkThemeGlobal) Color(0xFFDFB96C) else Color(0xFF8C6616),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontAmiri
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) hGold else hInk,
                                fontFamily = fontFamilyUi
                            )
                            Text(
                                text = "${option.style} • Studio Quality",
                                fontSize = 12.sp,
                                color = hInkMuted,
                                fontFamily = fontFamilyBody
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = NurIcons.Check,
                                contentDescription = "Selected",
                                tint = hGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (hasPreview) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { onPreviewReciter(option.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    previewLoadingId == option.id -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = hGold
                                        )
                                    }
                                    previewPlayingId == option.id -> {
                                        Icon(
                                            imageVector = NurIcons.PauseFilled,
                                            contentDescription = "Pause",
                                            tint = hGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = NurIcons.PlayFilled,
                                            contentDescription = "Preview",
                                            tint = hInkMuted,
                                            modifier = Modifier.size(16.dp)
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
