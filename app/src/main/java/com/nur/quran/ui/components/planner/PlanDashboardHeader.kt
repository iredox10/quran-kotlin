package com.nur.quran.ui.components.planner

import androidx.compose.ui.res.stringResource
import com.nur.quran.R
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Top dashboard header matching web Planner.jsx lines 843-899.
 */
@Composable
fun PlanDashboardHeader(
    planner: ReadingPlan,
    currentDay: Int,
    daySubtitle: String,
    ctaLabel: String,
    ctaEnabled: Boolean,
    onPrimaryCtaClick: () -> Unit,
    allPlans: List<ReadingPlan>,
    onSwitchPlan: (String) -> Unit,
    onBackToIntention: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPlanSwitcher by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.planner_your_reading_plan), fontFamily = fontFamilyMono, fontSize = 10.sp,
            letterSpacing = 1.6.sp, color = plannerAccentInk, modifier = Modifier.padding(bottom = 18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular Back Button (matching web: h-[42px] w-[42px] rounded-full border border-[var(--plr-bone-dark)])
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(hCream)
                    .border(1.dp, hBoneDark, CircleShape)
                    .clickable { onBackToIntention() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.ArrowLeft,
                    contentDescription = stringResource(R.string.planner_back_intention),
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Plan Title & Subtitles
            Column(modifier = Modifier.weight(1f)) {
                if (allPlans.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showPlanSwitcher = true }
                    ) {
                        Text(
                            text = planner.title.ifBlank { stringResource(R.string.planner_day_plan_fallback , planner.durationDays) },
                            fontFamily = fontFamilyUi,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hInk,
                            maxLines = 2,
                            modifier = Modifier.weight(1f, fill = false),
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = NurIcons.ChevronDown,
                            contentDescription = stringResource(R.string.planner_switch_plan),
                            tint = hInkMid,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPlanSwitcher,
                        onDismissRequest = { showPlanSwitcher = false },
                        modifier = Modifier.background(hCream)
                    ) {
                        allPlans.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = p.title.ifBlank { stringResource(R.string.planner_day_plan_switch , p.durationDays) },
                                        fontFamily = fontFamilyBody,
                                        fontWeight = if (p.id == planner.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (p.id == planner.id) hGold else hInk
                                    )
                                },
                                onClick = {
                                    showPlanSwitcher = false
                                    onSwitchPlan(p.id)
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = planner.title.ifBlank { stringResource(R.string.planner_quran_plan) },
                        fontFamily = fontFamilyUi,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = hInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(R.string.planner_day_of , currentDay, planner.durationDays),
                    fontFamily = fontFamilyUi,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = plannerAccentInk
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = daySubtitle,
                    fontFamily = fontFamilyBody,
                    fontSize = 13.sp,
                    color = hInkMid
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Top Primary CTA Button (matching web: rounded-full bg-[var(--accent-primary)] font-ui 1rem font-semibold)
        Button(
            onClick = onPrimaryCtaClick,
            enabled = ctaEnabled,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = hGold,
                disabledContainerColor = hSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text(
                text = ctaLabel,
                fontFamily = fontFamilyUi,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (ctaEnabled) Color(0xFF201D16) else hInkMuted
            )
        }
    }
}
