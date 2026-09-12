package com.nur.quran.ui.components.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerAssignment
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Main circular progress hero matching web Planner.jsx lines 80-97 and 900-950.
 */
@Composable
fun PlanProgressHero(
    readingPlan: ReadingPlan,
    ctaLabel: String,
    onPrimaryCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDays = readingPlan.durationDays
    val completedDaysCount = readingPlan.completedDays.size
    val pct = if (totalDays > 0) (completedDaysCount.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f) else 0f
    val pctInt = (pct * 100f).toInt()

    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(1200),
        label = "HeroRingAnim"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Gauge
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val strokeW = 10.dp.toPx()
                val arcSize = Size(size.width, size.height)

                // Track
                drawArc(
                    color = hBoneDark.copy(alpha = 0.4f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Progress
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(hGold, Color(0xFF10B981), hGold)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedPct,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$pctInt%",
                    fontFamily = fontFamilyUi,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
                Text(
                    text = "$completedDaysCount of $totalDays days",
                    fontFamily = fontFamilyMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = hInkMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = PlannerUtils.formatDaysRemaining(totalDays, completedDaysCount),
                    fontFamily = fontFamilyBody,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = hTeal
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main CTA Button
        Button(
            onClick = onPrimaryCtaClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = hGold)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = NurIcons.BookOpen,
                    contentDescription = null,
                    tint = hWhite,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = ctaLabel,
                    fontFamily = fontFamilyUi,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = hWhite
                )
            }
        }
    }
}
