package com.nur.quran.ui.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hGreen
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hRed

/**
 * Translation picker row with inline offline-download controls.
 *
 * Props-only mirror of the private `TafsirPickerRow` in SettingsDrawer:
 * gold check when selected, gold Download icon when idle, determinate
 * spinner + downloaded/total count + cancel while fetching, green check +
 * delete X once fully offline, plus a thin gold progress bar.
 *
 * All state is plain values, every interaction is a lambda — no ViewModel
 * is referenced. The host passes `pack` looked up from its
 * `translationPacks: List<PackUiState>`.
 */
@Composable
fun TranslationPickerRow(
    title: String,
    subtitle: String = "",
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
        val downloaded = pack?.downloaded ?: 0
        val total = pack?.total ?: 114
        val isDownloaded = pack != null && total > 0 && downloaded >= total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) hGold else hInk
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = hInkMuted
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(imageVector = NurIcons.Check, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
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
        val packError = pack?.error
        if (!packError.isNullOrBlank() && pack?.isDownloading != true && !isDownloaded) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = packError,
                    fontSize = 11.sp,
                    color = hRed,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDownload) {
                    Text(
                        text = "Retry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = hGold
                    )
                }
            }
        }
    }
    Divider(color = hBoneDark)
}
