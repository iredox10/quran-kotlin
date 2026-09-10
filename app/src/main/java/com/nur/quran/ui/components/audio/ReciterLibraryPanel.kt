package com.nur.quran.ui.components.audio

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.data.audio.NetworkPolicy
import com.nur.quran.data.audio.Reciter
import com.nur.quran.data.audio.Reciters
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGreen
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hRed
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.viewmodels.AudioPacksViewModel

private val StyleOrder = listOf(
    Reciters.STYLE_MURATTAL,
    Reciters.STYLE_MUJAWWAD,
    Reciters.STYLE_MUALLIM,
)

/**
 * Full reciter library: search + style-grouped list with voice preview,
 * per-surah / full-mushaf offline downloads and delete-all.
 *
 * Fills its parent (intended as a drawer subview). Selection is hoisted via
 * [selectedReciterId]/[onSelectReciter]; download state comes from
 * [AudioPacksViewModel]. Never throws — preview playback is `runCatching`-
 * guarded and released via [DisposableEffect].
 */
@Composable
fun ReciterLibraryPanel(
    selectedReciterId: Int,
    onSelectReciter: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val packVm: AudioPacksViewModel = hiltViewModel()
    val context = LocalContext.current
    val wifiOnly = runCatching { NetworkPolicy.isWifiOnly(context) }.getOrDefault(true)

    val reciterStates by packVm.reciterStates.collectAsState()
    val downloadState by packVm.downloadState.collectAsState()
    val stateByReciter = remember(reciterStates) {
        runCatching { reciterStates.associateBy { it.reciterId } }.getOrDefault(emptyMap())
    }

    var query by remember { mutableStateOf("") }

    // ── Shared 1:1 sample preview player (AudioSetupSheet pattern) ──
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewPlayingId by remember { mutableIntStateOf(-1) }
    var previewLoadingId by remember { mutableIntStateOf(-1) }

    fun stopPreview() {
        runCatching {
            previewPlayer?.setOnPreparedListener(null)
            previewPlayer?.setOnCompletionListener(null)
            previewPlayer?.setOnErrorListener(null)
            runCatching { previewPlayer?.stop() }
            previewPlayer?.release()
        }
        previewPlayer = null
        previewPlayingId = -1
        previewLoadingId = -1
    }

    fun startPreview(targetId: Int) {
        // Toggle off when tapping the reciter already previewing/loading.
        if (previewPlayingId == targetId || previewLoadingId == targetId) {
            stopPreview()
            return
        }
        val url = runCatching { Reciters.buildAudioUrl(targetId, "1:1") }.getOrNull() ?: return
        stopPreview()
        previewLoadingId = targetId
        runCatching {
            val player = MediaPlayer()
            previewPlayer = player
            player.setDataSource(url)
            player.setOnPreparedListener {
                if (previewLoadingId == targetId) {
                    previewLoadingId = -1
                    previewPlayingId = targetId
                    runCatching { it.start() }
                } else {
                    runCatching { it.release() }
                }
            }
            player.setOnCompletionListener { stopPreview() }
            player.setOnErrorListener { _, _, _ ->
                stopPreview()
                true
            }
            player.prepareAsync()
        }.onFailure { stopPreview() }
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    // Reciter id whose "download one surah" dialog is open, else null.
    var surahDialogFor by remember { mutableStateOf<Int?>(null) }

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) Reciters.ALL
        else runCatching { Reciters.ALL.filter { it.name.lowercase().contains(q) } }
            .getOrDefault(Reciters.ALL)
    }
    val grouped = remember(filtered) {
        runCatching { filtered.groupBy { it.style } }.getOrDefault(emptyMap())
    }
    val orderedStyles = remember(grouped) {
        StyleOrder.filter { grouped.containsKey(it) } +
            (grouped.keys - StyleOrder.toSet()).sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search reciters…",
                    color = hInkMuted,
                    fontFamily = fontFamilyBody,
                    fontSize = 14.sp,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            orderedStyles.forEach { style ->
                item(key = "header-$style") {
                    Text(
                        text = style.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInkMuted,
                        letterSpacing = 1.sp,
                        fontFamily = fontFamilyMono,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                items(grouped[style].orEmpty(), key = { "reciter-${it.id}" }) { reciter ->
                    val dl = stateByReciter[reciter.id]
                    val downloaded = dl?.downloadedSurahs ?: 0
                    val fullyOffline = downloaded >= 114
                    val activeProgress = remember(downloadState, reciter.id) {
                        runCatching {
                            downloadState
                                .filter { (key, p) ->
                                    p.isDownloading && key.startsWith("${reciter.id}:")
                                }
                                .mapNotNull { (key, p) ->
                                    val chapter = key.substringAfter(":").toIntOrNull()
                                        ?: return@mapNotNull null
                                    chapter to p
                                }
                                .sortedBy { it.first }
                        }.getOrDefault(emptyList())
                    }
                    ReciterLibraryRow(
                        reciter = reciter,
                        selected = reciter.id == selectedReciterId,
                        fullyOffline = fullyOffline,
                        downloadedSurahs = downloaded,
                        activeProgress = activeProgress,
                        previewPlaying = previewPlayingId == reciter.id,
                        previewLoading = previewLoadingId == reciter.id,
                        hasPreview = runCatching {
                            Reciters.buildAudioUrl(reciter.id, "1:1") != null
                        }.getOrDefault(false),
                        onSelect = { onSelectReciter(reciter.id) },
                        onPreview = { startPreview(reciter.id) },
                        onSurahDownload = { surahDialogFor = reciter.id },
                        onDownloadAll = { packVm.downloadMushaf(reciter.id) },
                        onDeleteAll = { packVm.deleteReciter(reciter.id) },
                    )
                }
            }
        }
    }

    // ── Per-surah download dialog ──
    val dialogReciterId = surahDialogFor
    if (dialogReciterId != null) {
        var surahText by remember(dialogReciterId) { mutableStateOf("") }
        var surahError by remember(dialogReciterId) { mutableStateOf<String?>(null) }
        val dialogName = runCatching { Reciters.nameOf(dialogReciterId) }
            .getOrDefault("Reciter $dialogReciterId")
        AlertDialog(
            onDismissRequest = { surahDialogFor = null },
            title = {
                Text(
                    text = "Download surah",
                    fontFamily = fontFamilyUi,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontSize = 16.sp,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = dialogName,
                        fontFamily = fontFamilyBody,
                        fontSize = 13.sp,
                        color = hInkMid,
                    )
                    OutlinedTextField(
                        value = surahText,
                        onValueChange = {
                            surahText = it.filter { c -> c.isDigit() }.take(3)
                            surahError = null
                        },
                        label = { Text("Surah number (1–114)", fontSize = 12.sp) },
                        singleLine = true,
                        isError = surahError != null,
                        supportingText = surahError?.let { msg ->
                            { Text(msg, color = hRed, fontSize = 12.sp) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val surah = runCatching { surahText.toInt() }.getOrNull()
                        if (surah == null || surah !in 1..114) {
                            surahError = "Enter a number from 1 to 114"
                            return@TextButton
                        }
                        val keys = runCatching { packVm.verseKeysFor(surah) }
                            .getOrDefault(emptyList())
                        runCatching {
                            packVm.downloadSurah(dialogReciterId, surah, keys, wifiOnly)
                        }
                        surahDialogFor = null
                    },
                ) {
                    Text("Download", color = hGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { surahDialogFor = null }) {
                    Text("Cancel", color = hInkMid)
                }
            },
        )
    }
}

@Composable
private fun ReciterLibraryRow(
    reciter: Reciter,
    selected: Boolean,
    fullyOffline: Boolean,
    downloadedSurahs: Int,
    activeProgress: List<Pair<Int, com.nur.quran.data.audio.DownloadProgress>>,
    previewPlaying: Boolean,
    previewLoading: Boolean,
    hasPreview: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onSurahDownload: () -> Unit,
    onDownloadAll: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) hCream else hSurface)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = reciter.name,
                        fontFamily = fontFamilyUi,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = hInk,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (selected) {
                        Icon(
                            imageVector = NurIcons.Check,
                            contentDescription = "Selected reciter",
                            tint = hGold,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = reciter.style,
                        fontFamily = fontFamilyMono,
                        fontSize = 11.sp,
                        color = hInkMuted,
                    )
                    Text(
                        text = "•  $downloadedSurahs/114",
                        fontFamily = fontFamilyMono,
                        fontSize = 11.sp,
                        color = hInkMuted,
                    )
                    if (fullyOffline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(hGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = NurIcons.CheckCircle2,
                                contentDescription = null,
                                tint = hGreen,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "OFFLINE",
                                fontFamily = fontFamilyMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = hGreen,
                            )
                        }
                    }
                }
            }
            if (hasPreview) {
                when {
                    previewLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = hGold,
                        )
                    }
                    previewPlaying -> {
                        IconButton(onClick = onPreview, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = NurIcons.PauseFilled,
                                contentDescription = "Stop preview",
                                tint = hGold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    else -> {
                        IconButton(onClick = onPreview, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = NurIcons.Play,
                                contentDescription = "Preview ${reciter.name}",
                                tint = hInkMid,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // Live per-surah progress lines ("reciter:chapter" keys).
        activeProgress.forEach { (chapter, p) ->
            Text(
                text = if (p.total > 0) "Surah $chapter — ${p.downloaded}/${p.total}"
                else "Surah $chapter — starting…",
                fontFamily = fontFamilyMono,
                fontSize = 11.sp,
                color = hGold,
            )
        }
        if (!fullyOffline && activeProgress.isEmpty() && downloadedSurahs > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(hBorderColor.copy(alpha = 0.5f))
                    .padding(vertical = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(downloadedSurahs / 114f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(hGold)
                        .padding(vertical = 2.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSurahDownload) {
                Icon(
                    imageVector = NurIcons.Download,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = " Surah",
                    color = hGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = fontFamilyUi,
                )
            }
            TextButton(onClick = onDownloadAll) {
                Icon(
                    imageVector = NurIcons.Download,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = " All",
                    color = hGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = fontFamilyUi,
                )
            }
            if (fullyOffline) {
                TextButton(onClick = onDeleteAll) {
                    Text(
                        text = "Delete all",
                        color = hRed,
                        fontSize = 13.sp,
                        fontFamily = fontFamilyUi,
                    )
                }
            }
        }
    }
}
