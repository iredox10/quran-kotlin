package com.nur.quran.ui.components.audio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGreen
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hRed

/**
 * Reusable per-chapter audio download row.
 *
 * All state is passed in as plain values and every interaction is a lambda,
 * so `SurahScreen` (or any host) can hoist download state however it wants
 * (e.g. from `SurahViewModel.downloadedChapters / isDownloading` plus a
 * per-chapter progress map). No ViewModel is referenced in here.
 *
 * States rendered:
 * - idle        → "N ayahs • <size>" + Download button
 * - downloading → progress bar (downloaded/total) + Cancel button
 * - downloaded  → "Downloaded • X MB" label + offline badge + Delete action
 */
@Composable
fun DownloadRow(
    reciterName: String,
    downloadedCount: Int = 0,
    totalCount: Int = 0,
    sizeLabel: String = "",
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    showOfflineBadge: Boolean = isDownloaded,
    onDownloadClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCount > 0) {
        (downloadedCount.coerceIn(0, totalCount).toFloat() / totalCount).coerceIn(0f, 1f)
    } else 0f

    val statusText = when {
        isDownloaded -> buildString {
            append("Downloaded")
            if (sizeLabel.isNotBlank()) append(" • $sizeLabel")
        }
        isDownloading && totalCount > 0 -> "$downloadedCount/$totalCount ayahs"
        totalCount > 0 -> buildString {
            append("$totalCount ayahs")
            if (sizeLabel.isNotBlank()) append(" • $sizeLabel")
        }
        else -> sizeLabel
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Volume2,
                        contentDescription = null,
                        tint = hGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reciterName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (showOfflineBadge) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = hGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "OFFLINE",
                                    color = hGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = hInkMuted,
                        fontFamily = fontFamilyBody
                    )
                }
            }

            if (isDownloading || isDownloaded) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(100)),
                    color = hGold,
                    trackColor = hBoneDark.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isDownloaded -> {
                        TextButton(onClick = onDeleteClick) {
                            Text(
                                "Delete",
                                color = hRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = fontFamilyUi
                            )
                        }
                    }
                    isDownloading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = hGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInkMid,
                                fontFamily = fontFamilyMono
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = onCancelClick) {
                                Text(
                                    "Cancel",
                                    color = hRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = fontFamilyUi
                                )
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownloadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = hGold)
                        ) {
                            Icon(
                                imageVector = NurIcons.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Download",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = fontFamilyUi
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact icon-only variant for tight spaces (e.g. beside the SurahHeader
 * play pill). Kept here so both variants share one file/import surface.
 */
@Composable
fun DownloadIconButton(
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (isDownloaded) {
        IconButton(onClick = {}, enabled = false, modifier = modifier) {
            Icon(
                imageVector = NurIcons.Check,
                contentDescription = "Downloaded",
                tint = hGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    } else if (isDownloading) {
        IconButton(onClick = onCancelClick, modifier = modifier) {
            CircularProgressIndicator(
                color = hGold,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }
    } else {
        IconButton(onClick = onDownloadClick, modifier = modifier) {
            Icon(
                imageVector = NurIcons.Download,
                contentDescription = "Download audio",
                tint = hInkMid,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
