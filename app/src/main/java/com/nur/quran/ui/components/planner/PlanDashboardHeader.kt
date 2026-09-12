package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Top dashboard header matching web Planner.jsx lines 843-900.
 */
@Composable
fun PlanDashboardHeader(
    planTitle: String,
    paceBadge: String?,
    onBack: () -> Unit,
    onOpenSettingsDrawer: () -> Unit,
    onAdjustPace: () -> Unit,
    onRebalance: () -> Unit,
    onViewArchives: () -> Unit,
    onDeletePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(hCream)
                    .border(1.dp, hBoneDark, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.ArrowLeft,
                    contentDescription = "Back to intention",
                    tint = hTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = planTitle,
                        fontFamily = fontFamilyUi,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (paceBadge != null) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = hGoldSoft
                        ) {
                            Text(
                                text = paceBadge.uppercase(),
                                fontSize = 8.sp,
                                fontFamily = fontFamilyMono,
                                fontWeight = FontWeight.Bold,
                                color = hGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "ACTIVE JOURNEY",
                    fontFamily = fontFamilyMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = hInkMuted
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(hCream)
                        .border(1.dp, hBoneDark, CircleShape)
                ) {
                    Icon(
                        imageVector = NurIcons.MoreVertical,
                        contentDescription = "Plan Options",
                        tint = hInkMid,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(hCream)
                ) {
                    DropdownMenuItem(
                        text = { Text("Adjust Pace", color = hInk) },
                        leadingIcon = { Icon(NurIcons.Sliders, null, tint = hGold) },
                        onClick = { showMenu = false; onAdjustPace() }
                    )
                    DropdownMenuItem(
                        text = { Text("Smart Rebalance", color = hInk) },
                        leadingIcon = { Icon(NurIcons.RotateCcw, null, tint = hTeal) },
                        onClick = { showMenu = false; onRebalance() }
                    )
                    DropdownMenuItem(
                        text = { Text("Reading Settings", color = hInk) },
                        leadingIcon = { Icon(NurIcons.Settings, null, tint = hInkMid) },
                        onClick = { showMenu = false; onOpenSettingsDrawer() }
                    )
                    DropdownMenuItem(
                        text = { Text("Archived Plans", color = hInk) },
                        leadingIcon = { Icon(NurIcons.Archive, null, tint = hInkMid) },
                        onClick = { showMenu = false; onViewArchives() }
                    )
                    Divider(color = hBoneDark.copy(alpha = 0.5f))
                    DropdownMenuItem(
                        text = { Text("Delete Plan", color = hRed) },
                        leadingIcon = { Icon(NurIcons.Trash2, null, tint = hRed) },
                        onClick = { showMenu = false; onDeletePlan() }
                    )
                }
            }
        }
    }
}
