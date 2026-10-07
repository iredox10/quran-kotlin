package com.nur.quran.ui.components.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.analytics.ActivityRecord
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite
import kotlinx.coroutines.delay

private data class LogTypeOption(val storeType: String, val label: String, val color: Color)

private val LOG_TYPE_OPTIONS = listOf(
    LogTypeOption("reading", "Reading", Color(0xFF10B981)),
    LogTypeOption("memorizing", "Memorizing", Color(0xFF3B82F6)),
    LogTypeOption("listening", "Listening", Color(0xFFF59E0B)),
    LogTypeOption("pomodoro", "Focus", Color(0xFF8B5CF6))
)

/**
 * "Log activity" card (web ActivityFlow log form parity): type radio + minutes
 * chips + optional Surah select, writing to the Room reading_sessions store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsLogActivityCard(
    chapters: List<ChapterEntity>,
    onLog: (durationSec: Int, type: String, chapterId: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    // Bump to drop form state on close (web closeLogForm reset parity).
    var resetGen by remember { mutableIntStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Log activity", fontFamily = fontFamilyUi, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = hInk)
                Box(
                    modifier = Modifier.clip(CircleShape).background(if (open) hSurface else hWhite)
                        .clickable { if (open) resetGen++; open = !open }.padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(NurIcons.Plus, null, tint = hGold, modifier = Modifier.size(12.dp))
                        Text(if (open) "CLOSE" else "LOG", fontFamily = fontFamilyMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInk)
                    }
                }
            }
            AnimatedVisibility(visible = open) {
                key(resetGen) {
                    AnalyticsLogActivityForm(chapters = chapters, onLog = onLog)
                }
            }
        }
    }
}

/**
 * The log-activity form body without card chrome, so the Analytics Flow card
 * can host it inline like web ActivityFlow (collapsible under the header).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsLogActivityForm(
    chapters: List<ChapterEntity>,
    onLog: (durationSec: Int, type: String, chapterId: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var logType by remember { mutableStateOf("reading") }
    var minutes by remember { mutableStateOf("") }
    var chapterId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var surahExpanded by remember { mutableStateOf(false) }

    // Web parity: confirm toast auto-clears after 3.5s.
    LaunchedEffect(confirm) {
        if (confirm != null) { delay(3500); confirm = null }
    }

    Column(modifier = modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        LOG_TYPE_OPTIONS.forEach { opt ->
                            val selected = logType == opt.storeType
                            Box(
                                modifier = Modifier.clip(CircleShape)
                                    .background(if (selected) hWhite else hSurface)
                                    .clickable { logType = opt.storeType; error = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(opt.color))
                                    Text(opt.label.uppercase(), fontFamily = fontFamilyMono, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (selected) hInk else hInkMuted)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = minutes, onValueChange = { minutes = it; error = null },
                            label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.width(110.dp)
                        )
                        ActivityRecord.MINUTES_CHIPS.forEach { m ->
                            Box(
                                modifier = Modifier.clip(CircleShape)
                                    .background(if (minutes == m.toString()) hWhite else hSurface)
                                    .clickable { minutes = m.toString(); error = null }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$m", fontFamily = fontFamilyMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted)
                            }
                        }
                    }
                    if (chapters.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        ExposedDropdownMenuBox(expanded = surahExpanded, onExpandedChange = { surahExpanded = it }) {
                            OutlinedTextField(
                                value = chapters.find { it.id.toString() == chapterId }?.let { "${it.id}. ${it.nameSimple}" } ?: "None",
                                onValueChange = {}, readOnly = true, label = { Text("Surah (optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(surahExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = surahExpanded, onDismissRequest = { surahExpanded = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { chapterId = ""; surahExpanded = false })
                                chapters.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.id}. ${c.nameSimple}") },
                                        onClick = { chapterId = c.id.toString(); surahExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    error?.let { Text(it, color = Color(0xFFE75344), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                    confirm?.let { Text(it, color = Color(0xFF10B981), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                val payload = ActivityRecord.buildSessionLog(logType, minutes, chapterId)
                                if (!payload.ok) { error = payload.error; confirm = null; return@Button }
                                onLog(payload.durationSec, payload.type, payload.chapterId)
                                val label = LOG_TYPE_OPTIONS.find { it.storeType == payload.type }?.label ?: payload.type
                                confirm = "Logged ${payload.durationSec / 60} min of $label" +
                                    if (payload.capped) " (capped at ${ActivityRecord.MAX_LOG_MINUTES} min)." else "."
                                error = null; minutes = ""; chapterId = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = hGold)
                        ) {
                            Text("LOG SESSION", fontFamily = fontFamilyMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
    }
}
