package com.nur.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.audio.PackDownloadRow
import com.nur.quran.ui.components.audio.PackUiState
import com.nur.quran.ui.viewmodels.AudioPacksViewModel
import java.util.Locale

/**
 * Downloads manager: storage summary + one flat row per reciter
 * (progress bar, MB, Download-full / Cancel / Delete).
 *
 * Props-only rows ([PackDownloadRow]); back navigation is delegated to
 * [onBack] — no nav code lives in here.
 */
@Composable
fun DownloadsScreen(
    packVm: AudioPacksViewModel,
    onBack: () -> Unit,
) {
    val states by packVm.reciterStates.collectAsState()
    val live by packVm.downloadState.collectAsState()
    val totalBytes by packVm.totalBytes.collectAsState()

    LaunchedEffect(Unit) { packVm.refresh() }

    val failedEntries = live.filter { (_, p) -> !p.error.isNullOrBlank() }
    val busyCount = states.count { it.isBusy }
    val offlineCount = states.count { it.downloadedSurahs >= it.totalSurahs && it.totalSurahs > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(hCream)
            .padding(16.dp)
    ) {
        // ── Top bar (SurahScreen header card pattern) ────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = hInk,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audio Downloads",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Text(
                    text = "${formatDlBytes(totalBytes)} • $offlineCount reciters offline",
                    fontSize = 12.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyBody
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Storage summary card ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = hWhite),
            border = BorderStroke(1.dp, hBoneDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "STORAGE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInkMuted,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDlBytes(totalBytes),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (busyCount > 0) {
                        "Downloading • $busyCount reciter${if (busyCount == 1) "" else "s"} active"
                    } else {
                        "Idle • $offlineCount/${states.size} full mushafs offline"
                    },
                    fontSize = 12.sp,
                    color = hInkMid,
                    fontFamily = fontFamilyBody
                )
            }
        }

        // ── Failed / notice text ─────────────────────────────────────────
        if (failedEntries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${failedEntries.size} download${if (failedEntries.size == 1) "" else "s"} failed: " +
                    (failedEntries.values.firstNotNullOfOrNull { it.error } ?: "download_failed") +
                    " — retry from the reciter row.",
                fontSize = 12.sp,
                color = hRed,
                fontFamily = fontFamilyBody
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Full-mushaf downloads run in the background, chapter by chapter.",
                fontSize = 12.sp,
                color = hInkMuted,
                fontFamily = fontFamilyBody
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Flat reciter rows ────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(states, key = { it.reciterId }) { s ->
                val rowError = live
                    .filter { (key, p) ->
                        key.startsWith("${s.reciterId}:") && !p.error.isNullOrBlank()
                    }
                    .values
                    .firstOrNull()
                    ?.error
                PackDownloadRow(
                    pack = PackUiState(
                        id = s.reciterId,
                        title = s.name,
                        downloaded = s.downloadedSurahs,
                        total = s.totalSurahs,
                        isDownloading = s.isBusy,
                        error = rowError
                    ),
                    subtitle = "${s.style} • ${formatDlBytes(s.bytes)}",
                    onDownloadClick = { packVm.downloadMushaf(it) },
                    onCancelClick = { packVm.pauseReciter(it) },
                    onDeleteClick = { packVm.deleteReciter(it) }
                )
            }
        }
    }
}

private fun formatDlBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb < 1) {
        String.format(Locale.US, "%d KB", bytes / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
