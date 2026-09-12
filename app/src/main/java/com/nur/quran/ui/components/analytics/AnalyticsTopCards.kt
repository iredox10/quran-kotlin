package com.nur.quran.ui.components.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldLight
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite

/**
 * Analytics Dashboard Header matching web Progress.jsx lines 230-234.
 *
 * Displays the app logo, uppercase monospace dashboard badge, "Your Progress" title,
 * and dynamic greeting message.
 */
@Composable
fun AnalyticsDashboardHeader(
    greeting: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Quran Nur Logo",
            modifier = Modifier
                .size(56.dp)
                .padding(bottom = 6.dp)
        )
        Text(
            text = "ANALYTICS DASHBOARD",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontFamilyMono,
            letterSpacing = 1.2.sp,
            color = hInkMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your Progress",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamilyUi,
            color = hInk
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$greeting. Here's the story of your consistency.",
            fontSize = 13.sp,
            fontFamily = fontFamilyBody,
            color = hInkMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Smart Insight Banner matching web Progress.jsx lines 237-241.
 *
 * Rounded banner with gold soft background, subtle border, lightbulb icon,
 * and highlighted dynamic insight text.
 */
@Composable
fun SmartInsightBanner(
    insight: String,
    modifier: Modifier = Modifier
) {
    if (insight.isBlank()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = hGoldSoft),
        border = BorderStroke(1.dp, hGold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = NurIcons.Lightbulb,
                contentDescription = "Smart Insight",
                tint = hGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = insight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamilyBody,
                color = hInk,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Consistency Card matching web Progress.jsx lines 244-259.
 *
 * Displays the current active streak in days, top-right circular badge with
 * flame icon (pulsing animation when streak > 0, tinted #e75344 with gold glow),
 * and subtle ambient glow background.
 */
@Composable
fun ConsistencyCard(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flamePulse")
    val flameAlpha by if (streak > 0) {
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flameAlpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    val flameScale by if (streak > 0) {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flameScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Ambient top-right gold blur glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(128.dp)
                    .offset(x = 32.dp, y = (-32).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                hGold.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Top row: Label & Flame Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSISTENCY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.5.sp,
                        color = hInkMuted
                    )

                    // Circular badge with flame icon (gold glow + pulse if streak > 0)
                    Box(contentAlignment = Alignment.Center) {
                        if (streak > 0) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(hGold.copy(alpha = 0.35f), Color.Transparent)
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(hGoldLight)
                                .then(
                                    if (streak > 0) {
                                        Modifier.border(1.dp, hGold.copy(alpha = 0.3f), CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.Flame,
                                contentDescription = "Streak flame",
                                tint = if (streak > 0) Color(0xFFE75344) else hInkMuted,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = flameScale
                                        scaleY = flameScale
                                        alpha = flameAlpha
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Value and Unit
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$streak",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = fontFamilyUi,
                        color = hInk,
                        lineHeight = 48.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Days",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hInkMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Current active streak",
                    fontSize = 13.sp,
                    fontFamily = fontFamilyBody,
                    color = hInkMuted
                )
            }
        }
    }
}

/**
 * Calculates percentage completion toward weekly goal clamped between 0 and 100.
 */
fun calculateWeeklyGoalPercent(weeklyMins: Int, weeklyGoalMins: Int = 180): Int =
    if (weeklyGoalMins > 0) {
        ((weeklyMins.toFloat() / weeklyGoalMins) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

/**
 * Calculates fractional progress toward daily focus goal clamped between 0.0f and 1.0f.
 */
fun calculateTodayFocusProgress(todayMins: Int, goalMins: Int = 30): Float =
    if (goalMins > 0) {
        (todayMins.toFloat() / goalMins.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

/**
 * Today's Focus Card matching web Progress.jsx lines 261-280.
 *
 * Displays total reading minutes today in gold, clock icon in circle,
 * and horizontal progress bar toward the 30-minute daily goal.
 */
@Composable
fun TodayFocusCard(
    todayMins: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header Row: Label & Clock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S FOCUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.5.sp,
                    color = hInkMuted
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(hSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Clock,
                        contentDescription = "Today Focus Clock",
                        tint = hInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Value and Unit in hGold
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$todayMins",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = fontFamilyUi,
                    color = hGold,
                    lineHeight = 48.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mins",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    color = hGold.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Progress Bar with Goal Text
            val progress = calculateTodayFocusProgress(todayMins = todayMins, goalMins = 30)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(hBorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress)
                            .clip(CircleShape)
                            .background(hGold)
                    )
                }
                Text(
                    text = "Goal: 30m",
                    fontSize = 10.sp,
                    fontFamily = fontFamilyMono,
                    color = hInkMuted
                )
            }
        }
    }
}

/**
 * Weekly Goal Card matching web Progress.jsx lines 282-301.
 *
 * Displays circular progress indicator showing completion percentage toward
 * the weekly goal (default 180 mins), with total minutes and target beside it.
 */
@Composable
fun WeeklyGoalCard(
    weeklyMins: Int,
    weeklyGoalMins: Int = 180,
    percent: Int = calculateWeeklyGoalPercent(weeklyMins, weeklyGoalMins),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header Row: Label & Target Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEKLY GOAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.5.sp,
                    color = hInkMuted
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(hSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Target,
                        contentDescription = "Weekly Target",
                        tint = hInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Progress Indicator + Numbers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(70.dp)
                ) {
                    CircularProgressIndicator(
                        progress = (percent.toFloat() / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.size(70.dp),
                        color = hGold,
                        strokeWidth = 6.dp,
                        trackColor = hBorderColor,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "$percent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hInk
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$weeklyMins",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamilyUi,
                            color = hInk,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ ${weeklyGoalMins}m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = fontFamilyUi,
                            color = hInkMuted,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total this week",
                        fontSize = 12.sp,
                        fontFamily = fontFamilyBody,
                        color = hInkMuted
                    )
                }
            }
        }
    }
}

/**
 * Overload for WeeklyGoalCard when weeklyGoalMins is omitted and percent is supplied positionally.
 */
@Composable
fun WeeklyGoalCard(
    weeklyMins: Int,
    percent: Int,
    modifier: Modifier = Modifier
) {
    WeeklyGoalCard(
        weeklyMins = weeklyMins,
        weeklyGoalMins = 180,
        percent = percent,
        modifier = modifier
    )
}

/**
 * Top 3 Metric Cards Grid/Column helper for convenience.
 */
@Composable
fun TopMetricCards(
    streak: Int,
    todayMins: Int,
    weeklyMins: Int,
    weeklyGoalMins: Int = 180,
    percent: Int = if (weeklyGoalMins > 0) ((weeklyMins.toFloat() / weeklyGoalMins) * 100).toInt().coerceIn(0, 100) else 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConsistencyCard(streak = streak)
        TodayFocusCard(todayMins = todayMins)
        WeeklyGoalCard(
            weeklyMins = weeklyMins,
            weeklyGoalMins = weeklyGoalMins,
            percent = percent
        )
    }
}

/**
 * Analytics Empty State matching web Progress.jsx lines 304-314.
 *
 * Displays large BookOpen icon, "Start Your Journey" heading,
 * encouraging description text, and dark pill Button "Open Quran".
 */
@Composable
fun AnalyticsEmptyState(
    onOpenQuranClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Subtle ambient gold glow in center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                hGold.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = NurIcons.BookOpen,
                    contentDescription = "Start Journey Book",
                    tint = hGold.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Start Your Journey",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    color = hInk,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your reading and memorization activity will beautifully visualize here as you use the app.",
                    fontSize = 14.sp,
                    fontFamily = fontFamilyBody,
                    color = hInkMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.widthIn(max = 380.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onOpenQuranClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = hInk,
                        contentColor = hWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Open Quran",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hWhite
                    )
                }
            }
        }
    }
}
