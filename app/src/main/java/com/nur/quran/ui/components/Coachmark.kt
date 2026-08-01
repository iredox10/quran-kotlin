package com.nur.quran.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.isDarkThemeGlobal
import kotlinx.coroutines.delay

/**
 * Pulsing hint dot anchored to a widget's corner, ported from the web app's
 * Coachmark.jsx. Tapping the dot reveals a small tooltip with a label and a
 * dismiss button; any interaction with the wrapped content dismisses it.
 */
@Composable
fun Coachmark(
    id: String,
    label: String,
    isDismissed: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (isDismissed) {
        content()
        return
    }

    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(3000)
            showTooltip = false
        }
    }

    val pulse = rememberInfiniteTransition(label = "coachmark-$id")
    val pingAlpha by pulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "coachmark-ping-alpha"
    )
    val pingScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "coachmark-ping-scale"
    )

    Box {
        content()
        // Pulsing dot at the top-right corner (web: absolute top-0 right-0 translate-1/3 -1/3)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(12.dp)
                .clickable { showTooltip = !showTooltip },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = pingScale
                        scaleY = pingScale
                        alpha = pingAlpha
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
        if (showTooltip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = 12.dp)
                    .widthIn(max = 150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkThemeGlobal) Color.White else Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkThemeGlobal) Color(0xFF0F172A) else Color.White,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = NurIcons.X,
                        contentDescription = "Dismiss hint",
                        tint = if (isDarkThemeGlobal) Color(0xFF334155) else Color(0xFFCBD5E1),
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable {
                                showTooltip = false
                                onDismiss()
                            }
                    )
                }
            }
        }
    }
}
