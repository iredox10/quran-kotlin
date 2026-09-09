package com.nur.quran.ui.components.audio

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Props-only row for a link-in-place reciter folder
 * (GreenTech / quran_android layout: 001..114.mp3 + timings.db).
 *
 * No ViewModel is referenced here — the host hoists link state
 * (e.g. from `SurahViewModel.linkedState`) and drives the
 * SAF folder picker itself.
 */
@Composable
fun LinkedAudioRow(
    reciterName: String,
    linkedCount: Int,
    totalSurahs: Int = 114,
    hasTiming: Boolean,
    isLinking: Boolean,
    onLinkClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLinked = linkedCount > 0
    val statusText = if (isLinked) {
        "Linked • $linkedCount/$totalSurahs offline"
    } else {
        "Not linked"
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciterName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = if (isLinked) hInkMid else hInkMuted,
                        fontFamily = fontFamilyBody
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Timing capability badge
                Surface(
                    shape = RoundedCornerShape(100),
                    color = if (hasTiming) hGreen.copy(alpha = 0.15f)
                    else hGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (hasTiming) "Ayah-precise" else "Full-surah only",
                        color = if (hasTiming) hGreen else hGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                if (isLinking) {
                    CircularProgressIndicator(
                        color = hGold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Linking…",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = hInkMid,
                        fontFamily = fontFamilyBody
                    )
                } else if (isLinked) {
                    TextButton(onClick = onUnlinkClick) {
                        Text(
                            "Unlink",
                            color = hRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = fontFamilyUi
                        )
                    }
                } else {
                    Button(
                        onClick = onLinkClick,
                        colors = ButtonDefaults.buttonColors(containerColor = hGold)
                    ) {
                        Text(
                            "Link folder",
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
