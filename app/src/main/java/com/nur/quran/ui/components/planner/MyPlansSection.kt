package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * "My Plans" list section matching web Planner.jsx lines 302-355.
 */
@Composable
fun MyPlansSection(
    planners: List<ReadingPlan>,
    activePlannerId: String?,
    archivedCount: Int,
    onSwitchPlan: (String) -> Unit,
    onDeletePlan: (String) -> Unit,
    onOpenCustomPlanModal: () -> Unit,
    onOpenArchivesModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Plans",
                fontFamily = fontFamilyUi,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (archivedCount > 0) {
                    TextButton(
                        onClick = onOpenArchivesModal,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Archives ($archivedCount)",
                            fontFamily = fontFamilyBody,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = hTeal
                        )
                    }
                }

                // Circular '+' button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, hBoneDark, CircleShape)
                        .clickable { onOpenCustomPlanModal() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Plus,
                        contentDescription = "Create custom plan",
                        tint = hTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Divider(color = hBoneDark, thickness = 1.dp)

        Spacer(modifier = Modifier.height(14.dp))

        if (planners.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                planners.forEach { plan ->
                    val overview = PlannerEngine.getPlannerOverview(plan)
                    val pct = overview?.completionRatio ?: 0f
                    val pctInt = Math.round(pct * 100)
                    val isActive = plan.id == activePlannerId

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = hCream),
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (isActive) hGold else hBoneDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSwitchPlan(plan.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.title.ifBlank { "Unnamed Plan" },
                                    fontFamily = fontFamilyBody,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${plan.durationDays} days · ${plan.unitType} · $pctInt% complete",
                                    fontFamily = fontFamilyMono,
                                    fontSize = 10.5.sp,
                                    color = hInkMuted,
                                    letterSpacing = 0.4.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Slim progress bar (height 3dp, rounded-sm)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(hBoneDark)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(pct.coerceIn(0f, 1f))
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(hTeal)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x24B8924A)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontFamily = fontFamilyMono,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hGold,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeletePlan(plan.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Trash2,
                                        contentDescription = "Delete plan",
                                        tint = hInkMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No plans yet. Select a path above or tap + to create a custom plan.",
                fontFamily = fontFamilyBody,
                fontSize = 13.5.sp,
                fontStyle = FontStyle.Italic,
                color = hInkMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        }
    }
}
