package com.nur.quran.ui.components.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.*

/**
 * Circular arc gauge for pace preview matching web PaceRing in Planner.jsx (size 104px, stroke 7px).
 */
@Composable
fun PaceRing(
    durationDays: Int,
    dailyPages: Int,
    selected: Boolean = false,
    unitLabel: String = "PAGES / DAY",
    modifier: Modifier = Modifier
) {
    val ratio = when {
        durationDays <= 30 -> 0.88f
        durationDays <= 60 -> 0.72f
        else -> 0.45f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(900),
        label = "PaceRingAnim"
    )

    val strokeColor = if (selected) hGold else Color(0x388E9B97)
    val trackColor = Color(0x248E9B97)

    Box(
        modifier = modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val strokeWidth = 7.dp.toPx()
            val arcSize = Size(size.width, size.height)

            // Track arc (270 degrees starting at 135 deg)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Value arc
            drawArc(
                color = strokeColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dailyPages",
                fontFamily = fontFamilyUi,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = unitLabel,
                fontFamily = fontFamilyMono,
                fontSize = 8.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp,
                color = hInkMuted
            )
        }
    }
}

/**
 * Big circular progress hero matching web RingProgress in Planner.jsx (size 240px, stroke 12px).
 */
@Composable
fun RingProgress(
    percent: Int,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val progressFloat = (percent / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFloat,
        animationSpec = tween(1200),
        label = "RingProgressAnim"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(size.width, size.height)

            // Empty track
            drawArc(
                color = Color(0x22C6A87C),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = hGold,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = "$percent%",
                fontFamily = fontFamilyUi,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = hGold,
                lineHeight = 52.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontFamily = fontFamilyMono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                color = hInkMuted
            )
        }
    }
}
