package com.nur.quran.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Inline Juz/Hizb divider chip — same gold gradient style as [PageDivider],
 * fed by in-memory markers ([com.nur.quran.data.juzStartForVerseKey] /
 * [com.nur.quran.data.hizbStartForVerseKey]) so no Room schema change is needed.
 */
@Composable
fun DivisionDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, hGold, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(100))
                .background(hGoldLight)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = hGold,
                fontFamily = fontFamilyBody,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, hGold, Color.Transparent)))
        )
    }
}

/**
 * Small sajdah marker (۩) shown next to the verse key for the 15 sajdah
 * verses ([com.nur.quran.data.isSajdahVerse]). Additive and rendering-safe:
 * plain text chip, no layout change when absent.
 */
@Composable
fun SajdahBadge(sajdahNumber: Int? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(hGoldLight)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (sajdahNumber != null) "۩ Sajdah $sajdahNumber" else "۩ Sajdah",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = hGold,
            fontFamily = fontFamilyBody,
            textAlign = TextAlign.Center
        )
    }
}
