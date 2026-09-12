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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.*

/**
 * Circular arc gauge for pace preview matching web PaceRing in Planner.jsx.
 */
@Composable
fun PaceRing(
    progress: Float,
    dailyUnits: Int,
    unitLabel: String = "PAGES",
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "PaceRingAnim"
    )

    val strokeColor = if (isSelected) hGold else hInkMuted.copy(alpha = 0.35f)
    val trackColor = hBoneDark.copy(alpha = 0.35f)

    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val strokeWidth = 6.dp.toPx()
            val arcSize = Size(size.width, size.height)

            // Background track arc (270 degrees)
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
                text = "$dailyUnits",
                fontFamily = fontFamilyUi,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) hGold else hInk
            )
            Text(
                text = unitLabel,
                fontFamily = fontFamilyMono,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = hInkMuted
            )
        }
    }
}
