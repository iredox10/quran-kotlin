package com.nur.quran.ui.components

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
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.screens.*
import kotlin.random.Random

/**
 * Hifdh recall test modal, mirroring the web HifdhTestModal:
 * ayahs are picked at random from the queue, the verse text is revealed on
 * request, and every rating is logged through onLogReview (FSRS). An "Again"
 * rating surfaces the previous ayah as a transition-link prompt.
 */
@Composable
fun HifdhTestModal(
    testQueue: List<String>,
    queueType: String,
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
    onLogReview: (verseKey: String, rating: Int) -> Unit,
    loadVerses: suspend (List<String>) -> Map<String, VerseEntity>
) {
    var remainingKeys by remember(testQueue) { mutableStateOf(testQueue.toMutableList()) }
    var currentVerseKey by remember { mutableStateOf<String?>(null) }
    var isRevealed by remember { mutableStateOf(false) }
    var lastRating by remember { mutableStateOf<Int?>(null) }
    var showPreviousAyah by remember { mutableStateOf(false) }
    var reviewedCount by remember { mutableStateOf(0) }
    var isCompleted by remember { mutableStateOf(testQueue.isEmpty()) }

    LaunchedEffect(Unit) {
        if (remainingKeys.isNotEmpty()) {
            val idx = Random.nextInt(remainingKeys.size)
            currentVerseKey = remainingKeys.removeAt(idx)
        }
    }

    val previousVerseKey = remember(currentVerseKey) { previousVerseKeyFor(currentVerseKey, chapters) }

    val verses by produceState<Map<String, VerseEntity>>(
        initialValue = emptyMap(),
        key1 = currentVerseKey,
        key2 = showPreviousAyah
    ) {
        value = loadVerses(listOfNotNull(currentVerseKey, if (showPreviousAyah) previousVerseKey else null))
    }

    val currentVerse = currentVerseKey?.let { verses[it] }
    val previousVerse = previousVerseKey?.let { verses[it] }

    fun handleRating(rating: Int) {
        val key = currentVerseKey ?: return
        onLogReview(key, rating)
        lastRating = rating
        reviewedCount++
        isRevealed = false
        if (rating == 1) {
            showPreviousAyah = true
        }
        if (remainingKeys.isEmpty()) {
            isCompleted = true
        }
    }

    fun pickNext() {
        showPreviousAyah = false
        lastRating = null
        if (remainingKeys.isNotEmpty()) {
            val idx = Random.nextInt(remainingKeys.size)
            currentVerseKey = remainingKeys.removeAt(idx)
        } else {
            isCompleted = true
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
                        .verticalScroll(rememberScrollState())
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
                                    text = if (testQueue.isEmpty()) "Queue empty" else "Reviewed $reviewedCount of ${testQueue.size} Verses",
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

                    if (isCompleted) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = hGreen, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Queue Completed!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (testQueue.isEmpty()) "This queue has no ayahs to review."
                                else "You've reviewed all ${testQueue.size} ayahs in this queue.",
                                fontSize = 12.sp,
                                color = hInkMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = hGold), shape = RoundedCornerShape(12.dp)) {
                                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (currentVerseKey != null) {
                        val chapter = chapters.find { it.id == currentVerseKey?.split(":")?.getOrNull(0)?.toIntOrNull() }
                        val ayahNum = currentVerseKey?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0

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
                                    text = "SURAH ${chapter?.id ?: "?"} • ${(chapter?.nameSimple ?: "").uppercase()}",
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

                                if (isRevealed) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (currentVerse?.textUthmani != null) {
                                        Text(
                                            text = currentVerse.textUthmani,
                                            fontSize = 24.sp,
                                            lineHeight = 40.sp,
                                            color = hInk,
                                            textAlign = TextAlign.Center,
                                            fontFamily = fontFamilyArabic
                                        )
                                    }
                                    if (!currentVerse?.translation.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = currentVerse?.translation.orEmpty(),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = hInkMid,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(16.dp))
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
                                }
                            }
                        }

                        if (lastRating == null) {
                            // Not yet rated — show recall + rating buttons
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Recite the ayah from memory, then tap reveal to verify.",
                                fontSize = 11.sp,
                                color = hInkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isRevealed) {
                                Spacer(modifier = Modifier.height(16.dp))
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
                                    RatingButton(label = "Again", color = Color(0xFFEF4444), modifier = Modifier.weight(1f)) { handleRating(1) }
                                    RatingButton(label = "Hard", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f)) { handleRating(2) }
                                    RatingButton(label = "Good", color = Color(0xFF10B981), modifier = Modifier.weight(1f)) { handleRating(3) }
                                    RatingButton(label = "Easy", color = Color(0xFF3B82F6), modifier = Modifier.weight(1f)) { handleRating(4) }
                                }
                            }
                        } else {
                            // Rated — feedback, optional transition link, then continue
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (lastRating!! >= 3) NurIcons.CheckCircle2 else NurIcons.XCircle,
                                    contentDescription = null,
                                    tint = if (lastRating!! >= 3) hGreen else Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lastRating!! >= 3) "Good recall! Review scheduled." else "Marked for review.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lastRating!! >= 3) hGreen else Color(0xFFEF4444)
                                )
                            }

                            if (showPreviousAyah && previousVerse != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = hGoldSoft.copy(alpha = 0.5f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, hGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "TRANSITION LINK — PREVIOUS AYAH",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hGold,
                                            fontFamily = fontFamilyMono,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tie ${currentVerseKey} to ${previousVerseKey} for smooth recall.",
                                            fontSize = 11.sp,
                                            color = hInkMuted,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = previousVerse.textUthmani ?: previousVerse.verseKey,
                                            fontSize = 22.sp,
                                            lineHeight = 36.sp,
                                            color = hInk,
                                            textAlign = TextAlign.Center,
                                            fontFamily = fontFamilyArabic
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { showPreviousAyah = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth(0.7f)
                                        ) {
                                            Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (!showPreviousAyah || previousVerse == null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { pickNext() },
                                        enabled = remainingKeys.isNotEmpty() || !isCompleted,
                                        colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(imageVector = NurIcons.RefreshCw, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test Another", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = hBoneDark),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.8f).height(44.dp)
                                    ) {
                                        Text("Done", color = hInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

private fun previousVerseKeyFor(current: String?, chapters: List<ChapterEntity>): String? {
    if (current == null) return null
    val parts = current.split(":")
    val chapterId = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val verseNumber = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (verseNumber > 1) return "$chapterId:${verseNumber - 1}"
    val prevChapter = chapters.find { it.id == chapterId - 1 } ?: return null
    return "${chapterId - 1}:${prevChapter.versesCount}"
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
