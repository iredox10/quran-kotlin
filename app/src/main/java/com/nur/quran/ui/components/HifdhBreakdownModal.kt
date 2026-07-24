package com.nur.quran.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HifdhBreakdownModal(
    title: String,
    items: List<String>,
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit
) {
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
                                Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = hGold, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items logged yet.", fontSize = 13.sp, color = hInkMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { itemStr ->
                                val parts = itemStr.split(":")
                                val sId = parts.getOrNull(0)?.toIntOrNull()
                                val chapter = chapters.find { it.id == sId }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = hCream),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (chapter != null) "${chapter.id}. ${chapter.nameSimple}" else "Item $itemStr",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInk
                                        )
                                        Surface(shape = RoundedCornerShape(100), color = hGreen.copy(alpha = 0.15f)) {
                                            Text(
                                                text = if (parts.size > 1) "Ayah ${parts[1]}" else "Memorized",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
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
}
