package com.nur.quran.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.ui.screens.*
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifdhGoalModal(
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
    onAddGoal: (chapterId: Int, targetDateMillis: Long) -> Unit
) {
    var selectedChapterId by remember { mutableStateOf(chapters.firstOrNull()?.id ?: 1) }
    val calendar = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) } }
    val initialDate = remember { calendar.timeInMillis }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

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
                    .widthIn(max = 440.dp)
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
                                Icon(imageVector = NurIcons.Brain, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Set Memorization Goal", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("SELECT SURAH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(hCream)
                            .border(1.dp, hBoneDark, RoundedCornerShape(12.dp))
                    ) {
                        LazyColumn {
                            items(chapters, key = { it.id }) { chapter ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedChapterId = chapter.id }
                                        .background(if (selectedChapterId == chapter.id) hGoldSoft else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${chapter.id}. ${chapter.nameSimple}",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedChapterId == chapter.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedChapterId == chapter.id) hGold else hInk
                                    )
                                    Text(text = "${chapter.versesCount} Ayahs", fontSize = 11.sp, color = hInkMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("TARGET DATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hCream,
                        border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            colors = DatePickerDefaults.colors(
                                selectedDayContainerColor = hGold,
                                todayDateBorderColor = hGold
                            )
                        )
                    }

                    val targetDateMillis = datePickerState.selectedDateMillis ?: initialDate

                    Spacer(modifier = Modifier.height(12.dp))

                    val daysUntil = ((targetDateMillis - System.currentTimeMillis()) / 86_400_000L).toInt()
                    Text(
                        text = if (daysUntil >= 0) "Complete by ${Date(targetDateMillis).toString().take(10)} • ~$daysUntil days away"
                        else "Pick a future date",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (daysUntil >= 0) hGreen else hInkMuted,
                        fontFamily = fontFamilyMono
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onAddGoal(selectedChapterId, targetDateMillis)
                            onDismiss()
                        },
                        enabled = daysUntil >= 0,
                        colors = ButtonDefaults.buttonColors(containerColor = hGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Create Goal", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
