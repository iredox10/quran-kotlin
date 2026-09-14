package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerBookmark
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Journal tab content matching web Planner.jsx lines 1235-1303.
 */
@Composable
fun PlanJournalTab(
    reflections: Map<String, String>,
    bookmarks: List<PlannerBookmark>,
    onNavigateToVerse: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // 1. Daily Reflections Section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(hGold)
                    )
                    Text(
                        text = "Daily Reflections",
                        fontFamily = fontFamilyUi,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                }

                if (reflections.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        reflections.forEach { (dayId, text) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = hWhite,
                                border = BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(hGoldSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayId,
                                                fontFamily = fontFamilyMono,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hGold
                                            )
                                        }
                                        Text(
                                            text = "REFLECTION",
                                            fontFamily = fontFamilyMono,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = hInkMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"$text\"",
                                        fontFamily = fontFamilyBody,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        color = hInk
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = hWhite,
                        border = BorderStroke(1.dp, hBoneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(NurIcons.BookOpen, contentDescription = null, tint = hInkMuted.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No reflections yet. Complete a day to write one!",
                                fontFamily = fontFamilyBody,
                                fontSize = 13.sp,
                                color = hInkMuted
                            )
                        }
                    }
                }
            }
        }

        // 2. Plan Highlights Section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(hTeal)
                    )
                    Text(
                        text = "Plan Highlights",
                        fontFamily = fontFamilyUi,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                }

                if (bookmarks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookmarks.forEach { bm ->
                            val parts = bm.verseKey.split(":")
                            val surahNum = parts.getOrNull(0)?.toIntOrNull() ?: 1
                            val ayahNum = parts.getOrNull(1)?.toIntOrNull() ?: 1

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = hWhite,
                                border = BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigateToVerse(surahNum, ayahNum)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bm.surahName.ifBlank { "Surah $surahNum" },
                                            fontFamily = fontFamilyUi,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInk
                                        )
                                        if (bm.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "\"${bm.note}\"",
                                                fontFamily = fontFamilyBody,
                                                fontSize = 12.sp,
                                                color = hInkMid
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = hGoldSoft
                                            ) {
                                                Text(
                                                    text = bm.verseKey,
                                                    fontFamily = fontFamilyMono,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = hGold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Tap to read verse →",
                                                fontFamily = fontFamilyBody,
                                                fontSize = 11.5.sp,
                                                color = hInkMuted
                                            )
                                        }
                                    }
                                    Icon(NurIcons.Gem, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = hWhite,
                        border = BorderStroke(1.dp, hBoneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(NurIcons.Gem, contentDescription = null, tint = hInkMuted.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No highlighted verses. Use the bookmark icon while reading!",
                                fontFamily = fontFamilyBody,
                                fontSize = 13.sp,
                                color = hInkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
