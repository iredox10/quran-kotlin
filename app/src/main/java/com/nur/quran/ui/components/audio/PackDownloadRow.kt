package com.nur.quran.ui.components.audio

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
 * Fallback tafsir catalogue used when the host has not supplied [PackUiState]s
 * yet. Mirrors the assumed `SUPPORTED_TAFSIRS` contract
 * (`data.tafsir.SUPPORTED_TAFSIRS: List<Pair<Int, String>>`); no import of that
 * symbol is attempted here so this file compiles standalone even if the
 * manager-side val does not exist yet — the host maps manager flows to
 * [PackUiState] and passes them in.
 */
val DEFAULT_TAFSIR_PACKS: List<Pair<Int, String>> = listOf(
    169 to "Ibn Kathir (Abridged)",
    168 to "Ibn Kathir (Full)",
    817 to "Al-Muyassar",
    16 to "al-Jalalayn"
)

/**
 * Standalone UI state for one downloadable pack (tafsir edition or similar).
 *
 * Deliberately decoupled from manager types (`TafsirPackStatus` /
 * `WordPackStatus`) so this file compiles without them. The host maps e.g.
 * `tafsirPacks: StateFlow<Map<Int, TafsirPackStatus>>` into
 * `List<PackUiState>` via `PackUiState(id, title, downloaded, total,
 * isDownloading, error)`.
 */
data class PackUiState(
    val id: Int,
    val title: String,
    val downloaded: Int = 0,
    val total: Int = 114,
    val isDownloading: Boolean = false,
    val error: String? = null
)

/**
 * Generic per-pack download row, reused for tafsir packs (one row per pack)
 * and adaptable to any other chapter-granular pack.
 *
 * Props-only, mirroring [DownloadRow]: all state is plain values, every
 * interaction is a lambda, no ViewModel is referenced.
 *
 * States rendered:
 * - idle        → "X/114 surahs" + Download button
 * - downloading → progress bar (downloaded/total) + Cancel button
 * - downloaded  → full bar + OFFLINE badge + Delete action
 * - error       → error text under the status line (hRed)
 */
@Composable
fun PackDownloadRow(
    pack: PackUiState,
    onDownloadClick: (Int) -> Unit = {},
    onCancelClick: (Int) -> Unit = {},
    onDeleteClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryProgress: Pair<Int, Int>? = null
) {
    val total = pack.total.coerceAtLeast(0)
    val downloaded = pack.downloaded.coerceIn(0, total.coerceAtLeast(1))
    val progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
    val isDownloaded = total > 0 && downloaded >= total

    val statusText = when {
        isDownloaded -> "Downloaded • $downloaded/$total surahs"
        pack.isDownloading && total > 0 -> "$downloaded/$total surahs"
        total > 0 -> "$downloaded/$total surahs"
        else -> "Not downloaded"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pack.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isDownloaded) {
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
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = hInkMuted,
                            fontFamily = fontFamilyBody
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = hInkMuted,
                        fontFamily = fontFamilyBody
                    )
                    if (!pack.error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pack.error,
                            fontSize = 12.sp,
                            color = hRed,
                            fontFamily = fontFamilyBody
                        )
                    }
                }
            }

            if (pack.isDownloading || isDownloaded || downloaded > 0) {
                Spacer(modifier = Modifier.height(8.dp))
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

            if (secondaryProgress != null) {
                val wordsTotal = secondaryProgress.second.coerceAtLeast(0)
                val wordsDownloaded =
                    secondaryProgress.first.coerceIn(0, wordsTotal.coerceAtLeast(1))
                val wordsProgress =
                    if (wordsTotal > 0) (wordsDownloaded.toFloat() / wordsTotal).coerceIn(0f, 1f) else 0f
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Words $wordsDownloaded/$wordsTotal",
                    fontSize = 11.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyBody
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = wordsProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100)),
                    color = hGreen,
                    trackColor = hBoneDark.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isDownloaded -> {
                        TextButton(onClick = { onDeleteClick(pack.id) }) {
                            Text(
                                "Delete",
                                color = hRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = fontFamilyUi
                            )
                        }
                    }
                    pack.isDownloading -> {
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
                            OutlinedButton(onClick = { onCancelClick(pack.id) }) {
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
                            onClick = { onDownloadClick(pack.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = hGold)
                        ) {
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
 * Summary row for word-translation packs (chapterId → status on the manager
 * side). The host passes the total cached-chapter count; per-chapter fetching
 * stays on the Surah screen, so this row only offers a "download all missing"
 * action.
 */
@Composable
fun WordPackSummaryRow(
    cachedCount: Int = 0,
    totalCount: Int = 114,
    isDownloading: Boolean = false,
    onDownloadAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val total = totalCount.coerceAtLeast(0)
    val cached = cachedCount.coerceIn(0, total.coerceAtLeast(1))
    val isComplete = total > 0 && cached >= total
    val progress = if (total > 0) (cached.toFloat() / total).coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Word translations",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isComplete) {
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
                        text = if (isComplete) "Downloaded • $cached/$total chapters"
                        else "$cached/$total chapters cached",
                        fontSize = 12.sp,
                        color = hInkMuted,
                        fontFamily = fontFamilyBody
                    )
                }
            }

            if (cached > 0 || isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
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

            if (!isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = hGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloading…",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = hInkMid,
                            fontFamily = fontFamilyBody
                        )
                    } else {
                        Button(
                            onClick = onDownloadAllClick,
                            colors = ButtonDefaults.buttonColors(containerColor = hGold)
                        ) {
                            Text(
                                "Download missing",
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
