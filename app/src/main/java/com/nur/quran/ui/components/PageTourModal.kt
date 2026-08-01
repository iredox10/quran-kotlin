package com.nur.quran.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nur.quran.ui.screens.hGold
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen guided tour, ported from the web app's PageTourModal.jsx.
 * Dims the page, punches a hole around the current step's target section
 * (by scrolling the home LazyColumn to that section), and shows a step card
 * with dots, prev/next/skip and optional "Go there" navigation.
 */
data class TourStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val target: String? = null,
    val link: String? = null
)

// Web PageTourModal.jsx palette (slate/emerald in light, gold-tinted in dark)
private val EMERALD = Color(0xFF059669)
private val EMERALD_SOFT = Color(0xFFECFDF5)
private val EMERALD_400 = Color(0xFF34D399)
private val SLATE_900 = Color(0xFF0F172A)
private val SLATE_600 = Color(0xFF475569)
private val SLATE_500 = Color(0xFF64748B)
private val SLATE_400 = Color(0xFF94A3B8)
private val SLATE_300 = Color(0xFFCBD5E1)
private val SLATE_50 = Color(0xFFF8FAFC)
private val SLATE_100 = Color(0xFFF1F5F9)
private val DARK_BG = Color(0xFF2D2D2A)
private val DARK_FOOTER = Color(0xFF1A1A18)
private val DARK_TEXT = Color(0xFFEFECE4)
private val DARK_BODY = Color(0xFFB0ABA5)
private val DARK_DOT_INACTIVE = Color(0xFF4A4A45)
private val WHITE_BORDER = Color(0x0DFFFFFF)

@Composable
fun PageTourModal(
    tourId: String,
    steps: List<TourStep>,
    pageId: String,
    visitThreshold: Int = 1,
    enabled: Boolean,
    completedTours: Set<String>,
    pageVisits: Int,
    isDark: Boolean,
    sectionIndices: Map<String, Int>,
    lazyListState: LazyListState,
    targetRects: Map<String, Rect>,
    onCompleteTour: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    // Web prepends a "Night Mode Active" step to the home tour in dark mode.
    val allSteps = remember(steps, tourId, isDark) {
        if (tourId == "home-tour" && isDark) {
            listOf(
                TourStep(
                    title = "Night Mode Active 🌙",
                    description = "Perfect for reading after Isha. Your eyes will thank you.",
                    icon = NurIcons.Moon
                )
            ) + steps
        } else {
            steps
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }
    var modalSize by remember { mutableStateOf(IntSize(320, 200)) }

    fun finishTour() {
        isVisible = false
        currentStep = 0
        onCompleteTour(tourId)
    }

    fun advanceOrFinish() {
        if (currentStep < allSteps.size - 1) {
            currentStep++
        } else {
            finishTour()
        }
    }

    // Show the tour (with the web's 800ms delay) once the visit threshold is met.
    LaunchedEffect(enabled, completedTours, pageVisits, tourId) {
        if (enabled && tourId !in completedTours && pageVisits >= visitThreshold) {
            delay(800)
            isVisible = true
        }
    }

    // Scroll the target section into view on each step change, then retry/skip
    // if the section is missing (mirrors the web's findAndScroll + retry logic).
    LaunchedEffect(currentStep, isVisible) {
        if (!isVisible || currentStep >= allSteps.size) return@LaunchedEffect
        val targetKey = allSteps[currentStep].target ?: return@LaunchedEffect
        val index = sectionIndices[targetKey] ?: -1
        if (index < 0) {
            delay(250)
            advanceOrFinish()
            return@LaunchedEffect
        }
        lazyListState.animateScrollToItem(index)
        delay(500)
        if (targetRects[targetKey] == null) {
            delay(300)
            if (targetRects[targetKey] == null) advanceOrFinish()
        }
    }

    if (!isVisible || allSteps.isEmpty()) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenH = with(density) { configuration.screenHeightDp.dp.toPx() }
    val modalWidthPx = min(320f * density.density, screenW - 32f * density.density)
    val pad = 16f
    val gap = 26f

    val step = allSteps[currentStep]
    val targetRect = step.target?.let { targetRects[it] }

    val modalPos = if (targetRect == null) {
        IntOffset(((screenW - modalWidthPx) / 2f).toInt(), ((screenH - modalSize.height) / 2f).toInt())
    } else {
        var top = targetRect.bottom + gap
        if (top + modalSize.height > screenH - pad) top = targetRect.top - gap - modalSize.height
        top = max(pad, top)
        val left = max(pad, min(targetRect.center.x - modalWidthPx / 2f, screenW - modalWidthPx - pad))
        IntOffset(left.toInt(), top.toInt())
    }

    Dialog(
        onDismissRequest = { finishTour() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dim overlay with a hole punched around the target (web's clip-path polygon).
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val dim = Color.Black.copy(alpha = 0.5f)
                if (targetRect == null) {
                    drawRect(dim)
                } else {
                    val hole = Rect(
                        targetRect.left - 10f,
                        targetRect.top - 10f,
                        targetRect.right + 10f,
                        targetRect.bottom + 10f
                    )
                    val l = hole.left.coerceIn(0f, size.width)
                    val r = hole.right.coerceIn(0f, size.width)
                    val t = hole.top.coerceIn(0f, size.height)
                    val b = hole.bottom.coerceIn(0f, size.height)
                    drawRect(dim, topLeft = Offset(0f, 0f), size = Size(size.width, t))
                    drawRect(dim, topLeft = Offset(0f, b), size = Size(size.width, size.height - b))
                    drawRect(dim, topLeft = Offset(0f, t), size = Size(l, b - t))
                    drawRect(dim, topLeft = Offset(r, t), size = Size(size.width - r, b - t))
                }
            }

            // Step card
            Surface(
                modifier = Modifier
                    .offset { IntOffset(modalPos.x, modalPos.y) }
                    .width(with(density) { modalWidthPx.toDp() })
                    .onSizeChanged { modalSize = it },
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) DARK_BG else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) WHITE_BORDER else SLATE_100),
                shadowElevation = 24.dp
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        IconButton(
                            onClick = { finishTour() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = NurIcons.X,
                                contentDescription = "Close tour",
                                tint = if (isDark) SLATE_400 else SLATE_400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(end = 24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = if (isDark) hGold else EMERALD,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = step.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) DARK_TEXT else SLATE_900,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = step.description,
                                fontSize = 14.sp,
                                color = if (isDark) DARK_BODY else SLATE_600,
                                lineHeight = 21.sp
                            )
                            if (step.link != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    onClick = {
                                        finishTour()
                                        onNavigateToRoute(step.link)
                                    },
                                    shape = RoundedCornerShape(100.dp),
                                    color = if (isDark) EMERALD.copy(alpha = 0.1f) else EMERALD_SOFT
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Go there",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) EMERALD_400 else EMERALD
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = NurIcons.ArrowRight,
                                            contentDescription = null,
                                            tint = if (isDark) EMERALD_400 else EMERALD,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = if (isDark) WHITE_BORDER else SLATE_100, thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) DARK_FOOTER else SLATE_50)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            allSteps.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .width(if (idx == currentStep) 16.dp else 6.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(
                                            if (idx == currentStep) {
                                                if (isDark) hGold else EMERALD
                                            } else {
                                                if (isDark) DARK_DOT_INACTIVE else SLATE_300
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep > 0) {
                                IconButton(
                                    onClick = { currentStep-- },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.ChevronLeft,
                                        contentDescription = "Previous step",
                                        tint = SLATE_500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Skip Tour",
                                    fontSize = 12.sp,
                                    color = SLATE_500,
                                    modifier = Modifier
                                        .clickable { finishTour() }
                                        .padding(8.dp)
                                )
                            }

                            Surface(
                                onClick = { advanceOrFinish() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) hGold else EMERALD
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentStep < allSteps.size - 1) "Next" else "Finish",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) DARK_FOOTER else Color.White
                                    )
                                    if (currentStep < allSteps.size - 1) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = NurIcons.ChevronRight,
                                            contentDescription = null,
                                            tint = if (isDark) DARK_FOOTER else Color.White,
                                            modifier = Modifier.size(16.dp)
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
