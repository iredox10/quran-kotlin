package com.nur.quran.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.ui.screens.*

@Composable
fun HifdhTestModal(
    testQueue: List<String>,
    queueType: String,
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
    onLogReview: (verseKey: String, rating: Int) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    val currentVerseKey = remember(currentIndex, testQueue) {
        testQueue.getOrNull(currentIndex) ?: testQueue.firstOrNull()
    }

    val parsedInfo = remember(currentVerseKey) {
        if (currentVerseKey == null) null
        else {
            val parts = currentVerseKey.split(":")
            val sId = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val aNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val chapter = chapters.find { it.id == sId }
            Triple(chapter, sId, aNum)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 460.dp)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = hWhite,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(hGoldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.Brain,
                                    contentDescription = null,
                                    tint = hGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$queueType Review Test",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk,
                                    fontFamily = fontFamilyUi
                                )
                                Text(
                                    text = "${currentIndex + 1} of ${testQueue.size} Verses",
                                    fontSize = 11.sp,
                                    color = hInkMuted,
                                    fontFamily = fontFamilyMono
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (parsedInfo?.first != null && currentVerseKey != null) {
                        val chapter = parsedInfo.first!!
                        val ayahNum = parsedInfo.third

                        // Prompt Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = hCream),
                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SURAH ${chapter.id} • ${chapter.nameSimple.uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hGold,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ayah $ayahNum",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk,
                                    fontFamily = fontFamilyUi
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (!isRevealed) {
                                    Button(
                                        onClick = { isRevealed = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        Icon(imageVector = NurIcons.Eye, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tap to Reveal Verse", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Revealed Verse Prompt
                                    Text(
                                        text = "Recite the Ayah from memory and verify your recall.",
                                        fontSize = 12.sp,
                                        color = hInkMid,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rating Buttons (FSRS Spaced Repetition)
                        if (isRevealed) {
                            Text(
                                text = "HOW WELL DID YOU RECALL THIS AYAH?",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInkMuted,
                                fontFamily = fontFamilyMono,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RatingButton(
                                    label = "Again",
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onLogReview(currentVerseKey, 1)
                                        if (currentIndex + 1 < testQueue.size) {
                                            currentIndex++
                                            isRevealed = false
                                        } else {
                                            onDismiss()
                                        }
                                    }
                                )
                                RatingButton(
                                    label = "Hard",
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onLogReview(currentVerseKey, 2)
                                        if (currentIndex + 1 < testQueue.size) {
                                            currentIndex++
                                            isRevealed = false
                                        } else {
                                            onDismiss()
                                        }
                                    }
                                )
                                RatingButton(
                                    label = "Good",
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onLogReview(currentVerseKey, 3)
                                        if (currentIndex + 1 < testQueue.size) {
                                            currentIndex++
                                            isRevealed = false
                                        } else {
                                            onDismiss()
                                        }
                                    }
                                )
                                RatingButton(
                                    label = "Easy",
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onLogReview(currentVerseKey, 4)
                                        if (currentIndex + 1 < testQueue.size) {
                                            currentIndex++
                                            isRevealed = false
                                        } else {
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = hGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Queue Review Complete!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = hGold)) {
                                Text("Done", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
