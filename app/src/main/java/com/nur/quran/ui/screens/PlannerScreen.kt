package com.nur.quran.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.draw.alpha
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    homeViewModel: HomeViewModel,
    surahViewModel: SurahViewModel,
    plannerViewModel: PlannerViewModel,
    onReadAssignment: (Int) -> Unit
) {
    val activePlan by plannerViewModel.activePlan.collectAsState()
    val archivedPlans by plannerViewModel.archivedPlans.collectAsState()
    val prayerTimings by plannerViewModel.prayerTimings.collectAsState()
    val chapters by plannerViewModel.chapters.collectAsState()
    // Web parity: journal highlights come from per-plan bookmarks.
    val planBookmarks by plannerViewModel.plannerBookmarks.collectAsState()
    val context = LocalContext.current

    var showSettingsDrawer by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(if (activePlan != null) "dashboard" else "intention") }
    var currentTab by remember { mutableStateOf("Today") }
    var showRebalanceDialog by remember { mutableStateOf(false) }
    var showArchivesDialog by remember { mutableStateOf(false) }
    var showPlannerSettings by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val todayStr = remember { PlannerEngine.formatPlannerDate() }
    val allPlans by plannerViewModel.allPlans.collectAsState()
    val activePlannerId by plannerViewModel.activePlannerId.collectAsState()
    // Web parity: persisted per-day reading seconds (session chip) + duplicate-title guard state.
    val sessionTotals by plannerViewModel.sessionTotals.collectAsState()
    var pendingDuplicatePlan by remember { mutableStateOf<ReadingPlan?>(null) }

    LaunchedEffect(activePlan) {
        if (activePlan != null && viewMode == "intention") {
            viewMode = "dashboard"
        }
    }

    var hasCheckedRebalance by remember { mutableStateOf(false) }
    LaunchedEffect(activePlan, hasCheckedRebalance) {
        if (activePlan != null && !hasCheckedRebalance) {
            val overview = PlannerEngine.getPlannerOverview(activePlan)
            if (overview != null) {
                val firstIncompleteDate = overview.firstIncomplete.date
                if (firstIncompleteDate < todayStr) {
                    showRebalanceDialog = true
                }
            }
            hasCheckedRebalance = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // Top Navbar
        TopNavbar(
            title = "Quran Nur",
            subTitle = "Planner",
            onThemeToggle = {
                isDarkThemeGlobal = !isDarkThemeGlobal
                context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
            },
            onSettingsClick = { showSettingsDrawer = true }
        )

        if (viewMode == "intention" || activePlan == null) {
            IntentionView(
                chapters = chapters,
                hasActivePlan = activePlan != null,
                allPlans = allPlans,
                activePlannerId = activePlannerId,
                onViewActive = { viewMode = "dashboard" },
                onBeginPlan = { plan ->
                    // Web parity (Planner.jsx handleBegin): confirm replace if title exists.
                    val existing = allPlans.find { it.title.isNotBlank() && it.title == plan.title }
                    if (existing != null) {
                        pendingDuplicatePlan = plan
                    } else {
                        runCatching { plannerViewModel.setActivePlan(plan) }
                        viewMode = "dashboard"
                    }
                },
                onSwitchPlan = { planId ->
                    plannerViewModel.switchActivePlan(planId)
                    viewMode = "dashboard"
                },
                onDeletePlan = { planId ->
                    plannerViewModel.deletePlan(planId)
                }
            )
        } else {
            val plan = activePlan!!
            val overview = remember(plan) { PlannerEngine.getPlannerOverview(plan) }
            val metrics = remember(plan) { PlannerEngine.getPlannerSuccessMetrics(plan) }
            val prefs = context.getSharedPreferences("PlannerSettings", Context.MODE_PRIVATE)
            val pref = prefs.getString("reading_preference", "after") ?: "after"
            val activePrayers by plannerViewModel.activePrayers.collectAsState()
            val todayAssignment = remember(plan, todayStr) {
                plan.assignments.find { PlannerEngine.getAssignmentStatus(plan, it) == "today" || PlannerEngine.getAssignmentStatus(plan, it) == "partial" }
                    ?: plan.assignments.find { it.date == todayStr }
                    ?: plan.assignments.firstOrNull { !plan.completedDays.contains(it.dayNumber) }
            }
            val prayerSlots = remember(plan, todayAssignment, prayerTimings, pref, activePrayers) {
                buildPrayerSlots(
                    plan, todayAssignment, prayerTimings,
                    readPreference = pref, activePrayers = activePrayers
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Dashboard Header & Mode Switcher Button
                item {
                    var showPlanSwitcher by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.clickable { if (allPlans.size > 1) showPlanSwitcher = true }) {
                            Text("ACTIVE PLAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono, letterSpacing = 2.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(plan.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                                if (allPlans.size > 1) {
                                    Icon(NurIcons.ChevronDown, contentDescription = "Switch Plan", tint = hInkMid, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showPlanSwitcher,
                                onDismissRequest = { showPlanSwitcher = false },
                                modifier = Modifier.background(hCream)
                            ) {
                                allPlans.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (p.id == activePlannerId) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(hTeal))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                }
                                                Text(p.title, color = hInk, fontSize = 14.sp)
                                            }
                                        },
                                        onClick = {
                                            plannerViewModel.switchActivePlan(p.id)
                                            showPlanSwitcher = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Share button
                            IconButton(
                                onClick = {
                                    val plan1 = activePlan
                                    if (plan1 != null) {
                                        val ov = PlannerEngine.getPlannerOverview(plan1)
                                        val mt = PlannerEngine.getPlannerSuccessMetrics(plan1)
                                        val pct = Math.round((ov?.completionRatio ?: 0f) * 100)
                                        val shareText = "📖 Quran Reading Progress\n" +
                                            "${plan1.title}: $pct% complete\n" +
                                            "Completed ${ov?.completedCount ?: 0} days of my ${plan1.durationDays}-day plan \u2022 Day ${ov?.currentDayNumber ?: 1} \u2022 ${mt?.consistencyStreak ?: 0}d streak\n" +
                                            "#QuranNur"
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share Progress"))
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(NurIcons.Share2, contentDescription = "Share", tint = hInkMid, modifier = Modifier.size(16.dp))
                            }
                            // Web parity (Planner.jsx handleExportCalendar): ICS export via FileProvider share.
                            IconButton(
                                onClick = {
                                    val plan1 = activePlan
                                    if (plan1 != null) {
                                        runCatching { sharePlannerIcs(context, plan1) }
                                            .onFailure {
                                                Toast.makeText(context, "Calendar export failed", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(NurIcons.CalendarDays, contentDescription = "Export calendar", tint = hInkMid, modifier = Modifier.size(16.dp))
                            }
                            if (archivedPlans.isNotEmpty()) {
                                TextButton(onClick = { showArchivesDialog = true }) {
                                    Text("Archives (${archivedPlans.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hInkMid)
                                }
                            }
                            TextButton(onClick = { showRebalanceDialog = true }) {
                                Text("Rebalance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hGold)
                            }
                            TextButton(onClick = { viewMode = "intention" }) {
                                Text("+ New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hTeal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Hero Progress Ring & Stats Card
                item {
                    val pct = Math.round((overview?.completionRatio ?: 0f) * 100)

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(hTeal, hTealMid)))
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$pct%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = fontFamilyMono)
                                        Text("COMPLETE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), fontFamily = fontFamilyMono)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${overview?.completedCount ?: 0}/${plan.durationDays}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = fontFamilyMono)
                                        Text("DAYS DONE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = fontFamilyMono)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${metrics?.consistencyStreak ?: 0}d", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = fontFamilyMono)
                                        Text("STREAK", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = fontFamilyMono)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${metrics?.successRate ?: 0}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = fontFamilyMono)
                                        Text("ON TIME", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = fontFamilyMono)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Tabs Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Today", "Progress", "Journal").forEach { tab ->
                            val isSelected = currentTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(if (isSelected) hGold else hBone)
                                    .clickable { currentTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else hInkMid
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Settings gear
                        IconButton(
                            onClick = { showPlannerSettings = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(NurIcons.Settings, contentDescription = "Settings", tint = hInkMid, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                when (currentTab) {
                    "Today" -> {
                        item {
                            val isPlanComplete = overview?.isFinishedWindow == true && plan.assignments.all { PlannerEngine.getAssignmentStatus(plan, it) == "completed" }

                            if (isPlanComplete) {
                                // Plan Complete Celebration
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Brush.verticalGradient(listOf(hTeal, hTealMid)))
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🎉", fontSize = 48.sp)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Plan Complete!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = fontFamilyUi)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("You've completed all ${plan.durationDays} days. Well done!", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(
                                                onClick = { plannerViewModel.buildRevisionPlan() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Start Revision Plan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hTeal)
                                            }
                                        }
                                    }
                                }
                            } else if (todayAssignment != null) {
                                val status = PlannerEngine.getAssignmentStatus(plan, todayAssignment)
                                val readPages = plan.assignmentReadPages[todayAssignment.dayNumber]?.size ?: 0
                                val totalPages = todayAssignment.pageEnd - todayAssignment.pageStart + 1
                                val pct = if (totalPages > 0) Math.round((readPages.toFloat() / totalPages) * 100) else 0

                                // Daily Focus Ring
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = pct / 100f,
                                        modifier = Modifier.size(160.dp),
                                        color = hTeal,
                                        trackColor = hBone,
                                        strokeWidth = 12.dp,
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$pct%", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyMono)
                                        Text("TODAY'S FOCUS", fontSize = 10.sp, color = hInkMuted, fontWeight = FontWeight.Bold, fontFamily = fontFamilyMono)
                                    }
                                }

                                // Web parity: today session chip (persisted reading seconds).
                                val todaySeconds = sessionTotals[todayAssignment.dayNumber] ?: 0L
                                if (todaySeconds > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(100),
                                            color = hTeal.copy(alpha = 0.1f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hTeal.copy(alpha = 0.25f))
                                        ) {
                                            Text(
                                                formatPlannerSessionLabel(todaySeconds),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hTeal,
                                                fontFamily = fontFamilyMono,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Today's assignment card
                                val difficultyMap = remember(plan) { PlannerEngine.getDifficultyIndicators(plan.assignments) }
                                val difficulty = difficultyMap[todayAssignment.dayNumber] ?: "moderate"
                                val difficultyLabel = difficulty.replaceFirstChar { it.uppercase() }

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = hCream),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, hGold),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(todayAssignment.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                                                Surface(
                                                    shape = RoundedCornerShape(100),
                                                    color = when (difficulty) {
                                                        "heavy" -> Color(0xFFFEF2F2)
                                                        "light" -> hTeal.copy(alpha = 0.12f)
                                                        else -> hBone
                                                    }
                                                ) {
                                                    Text(
                                                        text = difficultyLabel,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (difficulty) {
                                                            "heavy" -> Color(0xFFEF4444)
                                                            "light" -> hTeal
                                                            else -> hInkMuted
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Pages ${todayAssignment.pageStart} - ${todayAssignment.pageEnd}", fontSize = 13.sp, color = hInkMid)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Status: ${status.replaceFirstChar { it.uppercase() }}", fontSize = 12.sp, color = hTeal, fontWeight = FontWeight.Bold)
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onReadAssignment(todayAssignment.dayNumber) },
                                            colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val resumePage = PlannerEngine.getAssignmentResumePageNumber(plan, todayAssignment)
                                            Text(if (readPages > 0) "Resume at Page $resumePage" else "Start Reading", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // ── Prayer Schedule Integration ────────────────────────
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("PRAYER READING SCHEDULE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono, letterSpacing = 1.5.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    prayerSlots.indices.forEach { i ->
                                        val slot = prayerSlots[i]
                                        val isCompleted = slot.status == "completed"
                                        val isCurrent = slot.status == "current"
                                        val isEmpty = slot.status == "empty"
                                        val isLocked = slot.status == "locked"
                                        val containerBg = when {
                                            isCompleted -> hTeal.copy(alpha = 0.08f)
                                            isCurrent -> hGoldSoft
                                            isEmpty -> hBone.copy(alpha = 0.5f)
                                            isLocked -> hBone.copy(alpha = 0.35f)
                                            else -> hCream
                                        }
                                        val borderColor = when {
                                            isCompleted -> hTeal.copy(alpha = 0.3f)
                                            isCurrent -> hGold
                                            else -> hBoneDark
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = containerBg,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when {
                                                                    isCompleted -> hTeal
                                                                    isCurrent -> hGold
                                                                    else -> hBone
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isCompleted) {
                                                            Icon(NurIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        } else if (isCurrent) {
                                                            Icon(NurIcons.BookOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        } else if (isLocked) {
                                                            Icon(NurIcons.Lock, contentDescription = "Locked", tint = hInkMuted, modifier = Modifier.size(16.dp))
                                                        } else {
                                                            Icon(NurIcons.Clock, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(16.dp))
                                                        }
                                                    }

                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text(slot.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                                                            if (slot.time != null) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = hBone
                                                                ) {
                                                                    Text(slot.time, fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMid, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                                }
                                                            }
                                                        }
                                                        val unitLabel = PLANNER_UNITS[plan.unitType]?.plural ?: "Items"
                                                        Text(
                                                            when {
                                                                isCompleted -> "${slot.doneInSlot}/${slot.count} $unitLabel ✓"
                                                                isCurrent -> "${slot.doneInSlot} of ${slot.count} $unitLabel read"
                                                                isEmpty -> "No reading required"
                                                                isLocked -> "${slot.count} $unitLabel • locked"
                                                                else -> "${slot.count} $unitLabel • pending"
                                                            },
                                                            fontSize = 11.sp,
                                                            fontFamily = fontFamilyMono,
                                                            color = if (isCurrent) hGold else hInkMuted
                                                        )
                                                    }
                                                }

                                                // Web parity: mark/undo per prayer slot + read navigation.
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (isCompleted) {
                                                        IconButton(
                                                            onClick = {
                                                                plannerViewModel.setPlannerAssignmentProgress(
                                                                    todayAssignment.dayNumber, slot.slotStart
                                                                )
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                NurIcons.RotateCcw,
                                                                contentDescription = "Undo slot",
                                                                tint = hTeal,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else if (!isEmpty && !isLocked) {
                                                        IconButton(
                                                            onClick = {
                                                                plannerViewModel.setPlannerAssignmentProgress(
                                                                    todayAssignment.dayNumber, slot.slotEnd
                                                                )
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .border(1.5.dp, hTeal, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    NurIcons.Check,
                                                                    contentDescription = "Mark slot done",
                                                                    tint = hTeal,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (!isEmpty && !isLocked) {
                                                        Button(
                                                            onClick = { onReadAssignment(todayAssignment.dayNumber) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) hTeal.copy(alpha = 0.2f) else hTeal),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(if (isCompleted) "Review" else "Read", fontSize = 11.sp, color = if (isCompleted) hTeal else Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("No pending assignments for today!", color = hInkMid, fontSize = 14.sp)
                            }
                        }
                    }
                    "Progress" -> {
                        item {
                            val analytics = remember(plan) { PlannerEngine.getPlannerAnalytics(plan) }
                            val weekly = remember(plan) { PlannerEngine.getWeeklySummary(plan) }
                            val difficultyMap = remember(plan) { PlannerEngine.getDifficultyIndicators(plan.assignments) }
                            var selectedDayNumber by remember { mutableStateOf<Int?>(null) }
                            
                            // Overdue Action Banner
                            if (analytics.catchUpDays > 0) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(NurIcons.Info, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                            Text("You have ${analytics.catchUpDays} overdue assignments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { plannerViewModel.rebalancePlan("spread") },
                                                colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Spread", fontSize = 12.sp, color = Color.White) }
                                            Button(
                                                onClick = { plannerViewModel.rebalancePlan("extend") },
                                                colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Extend", fontSize = 12.sp, color = Color.White) }
                                        }
                                    }
                                }
                            }

                            // Timeline Bubble Grid
                            Text("Timeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk, modifier = Modifier.padding(bottom = 8.dp))
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                plan.assignments.forEach { a ->
                                    val status = PlannerEngine.getAssignmentStatus(plan, a)
                                    val diff = difficultyMap[a.dayNumber] ?: "moderate"
                                    
                                    val bgColor = when (status) {
                                        "completed" -> hGold
                                        "today", "partial" -> hCream
                                        "overdue" -> Color(0xFFFEF2F2)
                                        else -> hBone
                                    }
                                    val borderColor = when (status) {
                                        "today", "partial" -> hGold
                                        "overdue" -> Color(0xFFEF4444)
                                        else -> Color.Transparent
                                    }
                                    val textColor = when (status) {
                                        "completed" -> Color.White
                                        "overdue" -> Color(0xFFEF4444)
                                        else -> hInk
                                    }
                                    val dotColor = when (diff) {
                                        "heavy" -> Color(0xFFEF4444)
                                        "light" -> hGreen
                                        else -> Color.Transparent
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .then(
                                                if (borderColor != Color.Transparent) Modifier.border(2.dp, borderColor, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { 
                                                selectedDayNumber = if (selectedDayNumber == a.dayNumber) null else a.dayNumber 
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${a.dayNumber}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                        if (dotColor != Color.Transparent) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor).align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp))
                                        }
                                    }
                                }
                            }
                            
                            // Selected Day Details Card
                            val selDay = selectedDayNumber
                            if (selDay != null) {
                                val selAssignment = plan.assignments.find { it.dayNumber == selDay }
                                if (selAssignment != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = hCream),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, hGoldSoft),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Day ${selAssignment.dayNumber} • ${PlannerEngine.formatPlannerDateLabel(selAssignment.date)}", fontSize = 12.sp, color = hInkMuted)
                                                Text(selAssignment.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk)
                                                val status = PlannerEngine.getAssignmentStatus(plan, selAssignment)
                                                Text(status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = if(status == "overdue") Color(0xFFEF4444) else hTeal)
                                            }
                                            Button(
                                                onClick = { onReadAssignment(selAssignment.dayNumber) },
                                                colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Read", color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Performance Insight Cards
                            Text("Insights", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = hBone)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("${analytics.onTimeRate}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = hTeal, fontFamily = fontFamilyMono)
                                        Text("On-Time Rate", fontSize = 11.sp, color = hInkMuted)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = hBone)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(String.format(java.util.Locale.US, "%.1f", analytics.avgPagesPerDay), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyMono)
                                        Text("Avg Pages/Day", fontSize = 11.sp, color = hInkMuted)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Weekly Progress Cards
                            Text("Weekly Progress", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk, modifier = Modifier.padding(bottom = 8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                weekly.forEach { w ->
                                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = hCream)) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Week ${w.weekNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                            LinearProgressIndicator(
                                                progress = w.completionRatio,
                                                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                color = hTeal,
                                                trackColor = hBoneDark
                                            )
                                            Text("${w.completedCount}/${w.totalCount}", fontSize = 12.sp, color = hInkMuted, modifier = Modifier.padding(start = 12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Journal" -> {
                        // Reflections Section
                        item {
                            Text("DAILY REFLECTIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (plan.assignmentReflections.isEmpty()) {
                                Text("No reflections yet. Write your thoughts after completing an assignment.", color = hInkMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    plan.assignmentReflections.forEach { (day, text) ->
                                        val assignment = plan.assignments.find { it.dayNumber == day }
                                        val dateLabel = if (assignment != null) PlannerEngine.formatPlannerDateLabel(assignment.date) else ""
                                        
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Day $day", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                                                    Text(dateLabel, fontSize = 11.sp, color = hInkMuted)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(text, fontSize = 14.sp, color = hInk, lineHeight = 20.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Plan Highlights (Bookmarks) Section
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            Text("PLAN HIGHLIGHTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (planBookmarks.isEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = hBone),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(NurIcons.Bookmark, contentDescription = null, tint = hInkMuted.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No highlighted verses yet. Use the bookmark icon while reading!", fontSize = 12.sp, color = hInkMuted, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    planBookmarks.sortedByDescending { it.createdAt }.forEach { bm ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = hCream),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(bm.surahName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                                                    Text(bm.verseKey, fontSize = 11.sp, color = hGold, fontFamily = fontFamilyMono)
                                                }
                                                IconButton(
                                                    onClick = { plannerViewModel.removePlannerBookmark(bm.verseKey) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(NurIcons.X, contentDescription = "Remove highlight", tint = hInkMuted, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quote footer (web parity: Planner.jsx prophetic wisdom footer).
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "\"The best of deeds are those that are consistent, even if they are few.\"",
                            fontSize = 13.sp,
                            color = hInkMid,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "PROPHETIC WISDOM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInkMuted,
                            fontFamily = fontFamilyMono,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Footer Delete Button
                item {
                    Surface(
                        onClick = { showDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE5D5D5)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(NurIcons.X, contentDescription = null, tint = Color(0xFFC0392B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete plan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC0392B))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = hCream,
            title = { Text("Delete this plan?", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi) },
            text = {
                Text("All progress will be permanently lost. This cannot be undone.", fontSize = 14.sp, color = hInkMid)
            },
            confirmButton = {
                Button(
                    onClick = {
                        plannerViewModel.deleteActivePlan()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = hInkMuted)
                }
            }
        )
    }

    // Duplicate-title guard (web parity: Planner.jsx handleBegin confirm replace).
    pendingDuplicatePlan?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDuplicatePlan = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = hCream,
            title = { Text("Replace existing plan?", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi) },
            text = {
                Text(
                    "You already have a \"${pending.title}\" plan. Replace it with a new one?",
                    fontSize = 14.sp,
                    color = hInkMid
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val existingId = allPlans.find { it.title.isNotBlank() && it.title == pending.title }?.id
                        if (existingId != null) runCatching { plannerViewModel.deletePlan(existingId) }
                        runCatching { plannerViewModel.setActivePlan(pending) }
                        pendingDuplicatePlan = null
                        viewMode = "dashboard"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("Replace", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDuplicatePlan = null }) {
                    Text("Cancel", color = hInkMuted)
                }
            }
        )
    }

    if (showRebalanceDialog) {
        var customDaysText by remember(activePlan?.id) {
            mutableStateOf((activePlan?.durationDays ?: 30).toString())
        }
        AlertDialog(
            onDismissRequest = { showRebalanceDialog = false },
            title = { Text("Smart Pace Rebalance", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select how you would like to adjust your unread assignments:", fontSize = 13.sp, color = hInkMid)

                    Button(
                        onClick = {
                            plannerViewModel.rebalancePlan("spread")
                            showRebalanceDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Spread across remaining days", fontSize = 13.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            plannerViewModel.rebalancePlan("extend")
                            showRebalanceDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = hGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Extend Target End Date", fontSize = 13.sp, color = Color.White)
                    }

                    // Web parity: adjust pace to a custom total duration.
                    // Web (Planner.jsx): min = max(1, min(duration, completed + 1)).
                    val dialogPlan = activePlan
                    val dialogCompleted = dialogPlan?.let { PlannerEngine.getPlannerOverview(it)?.completedCount } ?: 0
                    val minNewDuration = kotlin.math.max(1, kotlin.math.min(dialogPlan?.durationDays ?: 30, dialogCompleted + 1))
                    Text("Or set a custom total duration (min $minNewDuration days):", fontSize = 13.sp, color = hInkMid)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customDaysText,
                            onValueChange = { v -> customDaysText = v.filter { it.isDigit() }.take(4) },
                            label = { Text("Total days", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                val days = (customDaysText.toIntOrNull() ?: minNewDuration).coerceAtLeast(minNewDuration)
                                runCatching { plannerViewModel.rebalancePlan("custom_pace", days) }
                                showRebalanceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRebalanceDialog = false }) {
                    Text("Cancel", color = hInkMuted)
                }
            }
        )
    }

    if (showArchivesDialog) {
        AlertDialog(
            onDismissRequest = { showArchivesDialog = false },
            title = { Text("Archived Plans History", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (archivedPlans.isEmpty()) {
                        Text("No archived plans found.", fontSize = 13.sp, color = hInkMuted)
                    } else {
                        archivedPlans.forEach { arch ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(arch.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                                        Text("${arch.durationDays} Days • Start: ${arch.startDate}", fontSize = 11.sp, color = hInkMuted)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                plannerViewModel.restoreArchivedPlan(arch.id)
                                                showArchivesDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Restore", fontSize = 11.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { plannerViewModel.deleteArchivedPlan(arch.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = NurIcons.X, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showArchivesDialog = false }) {
                    Text("Close", color = hInkMuted)
                }
            }
        )
    }

    // ── Planner Settings Dialog ──────────────────────────────────────────
    if (showPlannerSettings) {
        val prefs = context.getSharedPreferences("PlannerSettings", Context.MODE_PRIVATE)
        var showIntentionPrompt by remember { mutableStateOf(prefs.getBoolean("show_intention_prompt", true)) }
        var readingPreference by remember { mutableStateOf(prefs.getString("reading_preference", "after") ?: "after") }
        val useDeviceLocation by plannerViewModel.useDeviceLocation.collectAsState()
        val activePrayers by plannerViewModel.activePrayers.collectAsState()
        val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) plannerViewModel.setUseDeviceLocation(true)
        }

        AlertDialog(
            onDismissRequest = { showPlannerSettings = false },
            title = { Text("Planner Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Intention Prompts Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Intention Prompts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                            Text("Show intention reminder before reading", fontSize = 11.sp, color = hInkMuted)
                        }
                        Switch(
                            checked = showIntentionPrompt,
                            onCheckedChange = {
                                showIntentionPrompt = it
                                prefs.edit().putBoolean("show_intention_prompt", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = hTeal, checkedTrackColor = hTeal.copy(alpha = 0.3f))
                        )
                    }

                    // Reading Preference
                    Text("Reading Preference", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "after" to "Read After Prayer",
                            "before" to "Read Before Prayer",
                            "split" to "Split Before & After"
                        ).forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (readingPreference == key) hGoldSoft else hBone)
                                    .clickable {
                                        readingPreference = key
                                        prefs.edit().putString("reading_preference", key).apply()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = readingPreference == key,
                                    onClick = {
                                        readingPreference = key
                                        prefs.edit().putString("reading_preference", key).apply()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = hGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, fontSize = 13.sp, color = hInk)
                            }
                        }
                    }

                    // Prayer Times Source (web parity: geolocation, Mecca fallback)
                    Text("Prayer Times Location", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use my location", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = hInk)
                            Text(
                                if (useDeviceLocation) "Prayer times for your location" else "Using Makkah times",
                                fontSize = 11.sp, color = hInkMuted
                            )
                        }
                        Switch(
                            checked = useDeviceLocation,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        plannerViewModel.setUseDeviceLocation(true)
                                    } else {
                                        locationPermissionLauncher.launch(
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    }
                                } else {
                                    plannerViewModel.setUseDeviceLocation(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = hTeal, checkedTrackColor = hTeal.copy(alpha = 0.3f))
                        )
                    }

                    // Daily Prayers (web parity: activePrayers drives slot division)
                    Text("Daily Prayers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                    Text("Uncheck prayers to skip them in the daily schedule (min 1).", fontSize = 11.sp, color = hInkMuted)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PRAYER_NAMES.forEach { prayer ->
                            val selected = activePrayers.contains(prayer)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) hGoldSoft else hBone)
                                    .clickable {
                                        val next = if (selected) {
                                            activePrayers - prayer
                                        } else {
                                            activePrayers + prayer
                                        }
                                        plannerViewModel.setActivePrayers(next)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(prayer, fontSize = 13.sp, color = hInk)
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = hGold)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPlannerSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSettingsDrawer) {
        val packVm: PackViewModel = hiltViewModel()
        val tafsirPackRows by packVm.tafsirPacks.collectAsState()
        val wordCached by packVm.wordCachedCount.collectAsState()
        val wordDownloading by packVm.wordIsDownloading.collectAsState()
        val wordProgress by packVm.wordProgressByTafsir.collectAsState()
                val syncState by packVm.syncUiState.collectAsState()
        SettingsDrawer(
            viewModel = surahViewModel,
            onDismiss = { showSettingsDrawer = false },
            tafsirPacks = tafsirPackRows,
            onDownloadTafsir = packVm::downloadTafsirPack,
            onCancelTafsir = packVm::cancelTafsirPack,
            onDeleteTafsir = packVm::deleteTafsirPack,
                    translationPacks = packVm.translationPacks.collectAsState().value, onDownloadTranslation = packVm::downloadTranslationPack, onCancelTranslation = packVm::cancelTranslationPack, onDeleteTranslation = packVm::deleteTranslationPack,
            wordCachedCount = wordCached,
            wordIsDownloading = wordDownloading,
            onDownloadAllWords = packVm::downloadAllMissingWordPacks,
                    onCancelAllWords = packVm::cancelAllWordPacks,
                    syncState = syncState, onBackup = packVm::backupNow, onRestore = packVm::restoreNow,
            wordProgressByTafsir = wordProgress
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntentionView(
    chapters: List<ChapterEntity>,
    hasActivePlan: Boolean,
    allPlans: List<ReadingPlan>,
    activePlannerId: String?,
    onViewActive: () -> Unit,
    onBeginPlan: (ReadingPlan) -> Unit,
    onSwitchPlan: (String) -> Unit,
    onDeletePlan: (String) -> Unit
) {
    var selectedPaceId by remember { mutableStateOf("steady") }
    var showCustomForm by remember { mutableStateOf(false) }

    var unitType by remember { mutableStateOf("page") }
    var startUnitText by remember { mutableStateOf("1") }
    var endUnitText by remember { mutableStateOf("604") }
    var durationDaysText by remember { mutableStateOf("30") }
    var customTitleText by remember { mutableStateOf("") }
    var startDateText by remember { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var excludeDaysList by remember { mutableStateOf(listOf<Int>()) }
    // Web parity (Planner.jsx previewPlan): template tap opens a preview modal, not instant create.
    var previewPlan by remember { mutableStateOf<ReadingPlan?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
    ) {
        if (hasActivePlan) {
            item {
                Button(
                    onClick = onViewActive,
                    colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 16.dp)
                ) {
                    Text("Continue Active Reading Plan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (allPlans.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text("MY PLANS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    allPlans.forEach { plan ->
                        val overview = remember(plan) { PlannerEngine.getPlannerOverview(plan) }
                        val pct = if (overview != null) overview.completionRatio else 0f
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (plan.id == activePlannerId) hCream else hBone),
                            border = if (plan.id == activePlannerId) androidx.compose.foundation.BorderStroke(1.dp, hGoldSoft) else null,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onSwitchPlan(plan.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(plan.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk)
                                        if (plan.id == activePlannerId) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(color = hTeal.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                                Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = hTeal, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text("${plan.durationDays} Days • ${plan.unitType.replaceFirstChar { it.uppercase() }}s", fontSize = 12.sp, color = hInkMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = pct,
                                        modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = hTeal,
                                        trackColor = hBoneDark
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDeletePlan(plan.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(NurIcons.X, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Header Title
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("THE FIRST STEP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hTeal, fontFamily = fontFamilyMono, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Set Your Intention", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Choose a reading pace that resonates with your daily routine.", fontSize = 13.sp, color = hInkMuted, textAlign = TextAlign.Center)
            }
        }

        // 3 Preset Pace Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PresetPaceCard(
                    title = "Ramadan Pace",
                    daysText = "30 DAYS",
                    dailyPages = 21,
                    selected = selectedPaceId == "ramadan" && !showCustomForm,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedPaceId = "ramadan"
                        showCustomForm = false
                    }
                )
                PresetPaceCard(
                    title = "Steady Journey",
                    daysText = "60 DAYS",
                    dailyPages = 11,
                    selected = selectedPaceId == "steady" && !showCustomForm,
                    badge = "BALANCED",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedPaceId = "steady"
                        showCustomForm = false
                    }
                )
                PresetPaceCard(
                    title = "Devotional Path",
                    daysText = "1 YEAR",
                    dailyPages = 2,
                    selected = selectedPaceId == "devotional" && !showCustomForm,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedPaceId = "devotional"
                        showCustomForm = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Custom Plan Toggle
            TextButton(
                onClick = { showCustomForm = !showCustomForm },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showCustomForm) "▲ Hide Custom Builder" else "▼ Create Custom Scope & Exclude Days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = hGold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Custom Plan Form Section
        if (showCustomForm) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = hCream),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, hGold),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("CUSTOM PLAN SCOPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono, letterSpacing = 1.sp)

                        // Unit Type Selector (Page, Juz, Hizb, Surah)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("page" to "Page", "juz" to "Juz", "hizb" to "Hizb", "surah" to "Surah").forEach { (uKey, uLabel) ->
                                Button(
                                    onClick = {
                                        unitType = uKey
                                        val maxU = PLANNER_UNITS[uKey]?.max ?: 604
                                        startUnitText = "1"
                                        endUnitText = maxU.toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (unitType == uKey) hGold else hBone),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(uLabel, fontSize = 11.sp, color = if (unitType == uKey) Color.White else hInk, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Range Inputs
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = startUnitText,
                                onValueChange = { startUnitText = it },
                                label = { Text("Start $unitType") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endUnitText,
                                onValueChange = { endUnitText = it },
                                label = { Text("End $unitType") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Duration & Title
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = durationDaysText,
                                onValueChange = { durationDaysText = it },
                                label = { Text("Duration (Days)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customTitleText,
                                onValueChange = { customTitleText = it },
                                label = { Text("Custom Title") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true
                            )
                        }

                        // Units Per Day Slider
                        val sUnit = startUnitText.toIntOrNull() ?: 1
                        val eUnit = endUnitText.toIntOrNull() ?: (PLANNER_UNITS[unitType]?.max ?: 604)
                        val totalUnits = kotlin.math.max(1, eUnit - sUnit + 1)
                        val maxUnitsPerDay = kotlin.math.min(totalUnits, 50).toFloat()
                        var unitsPerDaySlider by remember { mutableFloatStateOf(
                            totalUnits.toFloat() / (durationDaysText.toIntOrNull() ?: 30).coerceAtLeast(1)
                        ) }

                        Text("Units Per Day: ${"%.1f".format(unitsPerDaySlider)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hInkMid)
                        Slider(
                            value = unitsPerDaySlider.coerceIn(1f, maxUnitsPerDay),
                            onValueChange = { newVal ->
                                unitsPerDaySlider = newVal
                                val newDuration = kotlin.math.ceil(totalUnits.toDouble() / newVal).toInt().coerceAtLeast(1)
                                durationDaysText = newDuration.toString()
                            },
                            valueRange = 1f..maxUnitsPerDay,
                            steps = (maxUnitsPerDay - 2).toInt().coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = hGold,
                                activeTrackColor = hGold,
                                inactiveTrackColor = hBone
                            )
                        )

                        // Start Date (web parity: editable via date picker)
                        Text("Start Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hInkMid)
                        val datePickerContext = LocalContext.current
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = hBone,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parts = startDateText.split("-").mapNotNull { it.toIntOrNull() }
                                    val cal = java.util.Calendar.getInstance()
                                    val initY = parts.getOrNull(0) ?: cal.get(java.util.Calendar.YEAR)
                                    val initM = (parts.getOrNull(1) ?: (cal.get(java.util.Calendar.MONTH) + 1)) - 1
                                    val initD = parts.getOrNull(2) ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    android.app.DatePickerDialog(
                                        datePickerContext, { _, y, m, d ->
                                            startDateText = "%04d-%02d-%02d".format(y, m + 1, d)
                                        }, initY, initM, initD
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    PlannerEngine.formatPlannerDateLabel(startDateText),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                                Icon(NurIcons.CalendarDays, contentDescription = "Pick start date", tint = hGold, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Exclude Days Chips (0=Sun, 1=Mon, ..., 6=Sat)
                        Text("Exclude Days (Skip Reading)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hInkMid)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0 to "Sun", 1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat").forEach { (dIdx, dName) ->
                                val isExcluded = excludeDaysList.contains(dIdx)
                                FilterChip(
                                    selected = isExcluded,
                                    onClick = {
                                        excludeDaysList = if (isExcluded) excludeDaysList - dIdx else excludeDaysList + dIdx
                                    },
                                    label = { Text(dName, fontSize = 10.sp) }
                                )
                            }
                        }

                        // ── Live Summary Row ─────────────────────────────
                        val summaryDuration = durationDaysText.toIntOrNull() ?: 30
                        val summaryStart = startUnitText.toIntOrNull() ?: 1
                        val summaryEnd = endUnitText.toIntOrNull() ?: (PLANNER_UNITS[unitType]?.max ?: 604)
                        val summaryTotalUnits = kotlin.math.max(1, summaryEnd - summaryStart + 1)
                        val summaryUnitsPerDay = if (summaryDuration > 0) "%.1f".format(summaryTotalUnits.toFloat() / summaryDuration) else "—"
                        val completionCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, summaryDuration)
                        }
                        val completionDate = PlannerEngine.formatPlannerDateLabel(
                            PlannerEngine.formatPlannerDate(completionCal.time)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = hTeal.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, hTeal.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(summaryUnitsPerDay, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hTeal, fontFamily = fontFamilyMono)
                                    Text("${unitType}s/day", fontSize = 9.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$summaryDuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyMono)
                                    Text("days", fontSize = 9.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(completionDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono)
                                    Text("finish by", fontSize = 9.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val sUnit = startUnitText.toIntOrNull() ?: 1
                                val eUnit = endUnitText.toIntOrNull() ?: (PLANNER_UNITS[unitType]?.max ?: 604)
                                val dur = durationDaysText.toIntOrNull() ?: 30
                                val title = if (customTitleText.isNotBlank()) customTitleText else "Custom $unitType Plan"

                                val built = PlannerEngine.buildReadingPlanner(
                                    unitType = unitType,
                                    durationDays = dur,
                                    startDate = startDateText,
                                    startUnit = sUnit,
                                    endUnit = eUnit,
                                    customTitle = title,
                                    excludeDays = excludeDaysList,
                                    chapters = chapters
                                )
                                onBeginPlan(built)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Create Custom Plan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Plan Templates Library (web parity: Planner.jsx grid + preview modal + surah gating).
        item {
            val chaptersReady = chapters.isNotEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PLAN TEMPLATES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyMono, letterSpacing = 1.sp)
                Text("CURATED PATHS", fontSize = 9.sp, color = hInkMuted, fontFamily = fontFamilyMono)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PLAN_TEMPLATES.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { tmpl ->
                            // Web parity: surah templates gated until chapters ready.
                            val needsChapters = tmpl.unitType == "surah" && !chaptersReady
                            val unitCount = kotlin.math.max(1, tmpl.endUnit - tmpl.startUnit + 1)
                            val perDay = kotlin.math.max(1, kotlin.math.ceil(unitCount.toDouble() / tmpl.durationDays).toInt())
                            val unitPlural = PLANNER_UNITS[tmpl.unitType]?.plural ?: "units"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (needsChapters) 0.5f else 1f)
                                    .clickable(enabled = !needsChapters) {
                                        runCatching {
                                            PlannerEngine.buildReadingPlanner(
                                                unitType = tmpl.unitType,
                                                durationDays = tmpl.durationDays,
                                                startDate = startDateText,
                                                startUnit = tmpl.startUnit,
                                                endUnit = tmpl.endUnit,
                                                customTitle = tmpl.title,
                                                chapters = chapters
                                            )
                                        }.onSuccess { previewPlan = it }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(tmpl.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tmpl.description, fontSize = 11.sp, color = hInkMid, lineHeight = 16.sp, maxLines = 2)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        if (needsChapters) "Loading surahs…" else "$perDay $unitPlural/day • ${tmpl.durationDays}d",
                                        fontSize = 10.sp,
                                        color = hInkMuted,
                                        fontFamily = fontFamilyMono
                                    )
                                }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Action Buttons
        if (!showCustomForm) {
            item {
                Button(
                    onClick = {
                        val days = when (selectedPaceId) {
                            "ramadan" -> 30
                            "steady" -> 60
                            "devotional" -> 365
                            else -> 60
                        }
                        val title = when (selectedPaceId) {
                            "ramadan" -> "Ramadan Pace Plan"
                            "steady" -> "Steady Journey Plan"
                            else -> "Devotional Path Plan"
                        }
                        val built = PlannerEngine.buildReadingPlanner(
                            unitType = "page",
                            durationDays = days,
                            startDate = startDateText,
                            startUnit = 1,
                            endUnit = 604,
                            customTitle = title,
                            chapters = chapters
                        )
                        onBeginPlan(built)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Begin Journey", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Template preview modal (web parity: Planner.jsx Duration/Pace/Starts/Done-by + Create).
    previewPlan?.let { preview ->
        val planUnits = preview.assignments.sumOf { it.items.size }
        val previewPerDay = kotlin.math.max(1, kotlin.math.ceil(planUnits.toDouble() / preview.durationDays.coerceAtLeast(1)).toInt())
        val previewUnitPlural = PLANNER_UNITS[preview.unitType]?.plural ?: "units"
        val previewDoneBy = runCatching {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val cal = java.util.Calendar.getInstance()
            cal.time = fmt.parse(preview.startDate) ?: java.util.Date()
            cal.add(java.util.Calendar.DAY_OF_YEAR, preview.durationDays - 1)
            PlannerEngine.formatPlannerDateLabel(PlannerEngine.formatPlannerDate(cal.time))
        }.getOrDefault("—")
        val previewStats = listOf(
            "Duration" to "${preview.durationDays} days",
            "Pace" to "$previewPerDay $previewUnitPlural/day",
            "Starts" to PlannerEngine.formatPlannerDateLabel(preview.startDate),
            "Done by" to previewDoneBy
        )
        AlertDialog(
            onDismissRequest = { previewPlan = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = hCream,
            title = { Text("Start \"${preview.title}\"?", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    previewStats.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (label, value) ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = hBone),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(label.uppercase(), fontSize = 9.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = hInk)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val built = preview
                        previewPlan = null
                        onBeginPlan(built)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Plan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewPlan = null }) {
                    Text("Cancel", color = hInkMuted)
                }
            }
        )
    }
}

@Composable
private fun PresetPaceCard(
    title: String,
    daysText: String,
    dailyPages: Int,
    selected: Boolean,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pagesPerPrayer = "%.1f".format(dailyPages / 5f)
    val ringProgress = dailyPages / 21f // Normalize against Ramadan pace (max)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) hCream else hCream.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) hGold else hBoneDark),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Surface(shape = RoundedCornerShape(100), color = hGoldSoft) {
                    Text(badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = hGold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // PaceRing: Circular progress indicator
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringColor = if (selected) hGold else hInkMuted.copy(alpha = 0.4f)
                val trackColor = if (selected) hGoldSoft else hBone
                Canvas(modifier = Modifier.size(72.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress arc
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$dailyPages", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (selected) hGold else hInk, fontFamily = fontFamilyMono)
                    Text("pg/day", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi, textAlign = TextAlign.Center)
            Text(daysText, fontSize = 10.sp, color = hInkMuted, fontFamily = fontFamilyMono)
            Text("$pagesPerPrayer pg/prayer", fontSize = 8.sp, color = hTeal, fontFamily = fontFamilyMono)
        }
    }
}

/** Web parity (Planner.jsx handleExportCalendar): one VEVENT per assignment. */
private fun buildPlannerIcs(plan: ReadingPlan): String {
    val unitPlural = PLANNER_UNITS[plan.unitType]?.plural ?: "units"
    val sb = StringBuilder("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//QuranNur//Planner//EN\n")
    plan.assignments.forEach { a ->
        val dateStr = a.date.replace("-", "")
        sb.append("BEGIN:VEVENT\nDTSTART;VALUE=DATE:$dateStr\nDTEND;VALUE=DATE:$dateStr\nSUMMARY:Quran Plan - Day ${a.dayNumber}\nDESCRIPTION:Read ${a.items.size} $unitPlural\nEND:VEVENT\n")
    }
    sb.append("END:VCALENDAR")
    return sb.toString()
}

/**
 * Writes quran_plan.ics to cache/shared and shares it via the
 * "${packageName}.fileprovider" authority (verified in AndroidManifest.xml).
 */
private fun sharePlannerIcs(context: Context, plan: ReadingPlan) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "quran_plan.ics")
    file.writeText(buildPlannerIcs(plan))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/calendar"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Calendar"))
}

/** Web parity (Planner.jsx formatSessionTime): short "Xh Ym today" label. */
private fun formatPlannerSessionLabel(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h ${m}m today"
        m > 0 -> "${m}m ${s}s today"
        else -> "${s}s today"
    }
}
