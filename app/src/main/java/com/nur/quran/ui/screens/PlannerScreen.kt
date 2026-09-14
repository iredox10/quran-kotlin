package com.nur.quran.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.components.planner.*
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    homeViewModel: HomeViewModel,
    surahViewModel: SurahViewModel,
    plannerViewModel: PlannerViewModel,
    onReadAssignment: (Int) -> Unit
) {
    val activePlan by plannerViewModel.activePlan.collectAsState()
    val allPlans by plannerViewModel.allPlans.collectAsState()
    val activePlannerId by plannerViewModel.activePlannerId.collectAsState()
    val archivedPlans by plannerViewModel.archivedPlans.collectAsState()
    val prayerTimings by plannerViewModel.prayerTimings.collectAsState()
    val chapters by plannerViewModel.chapters.collectAsState()
    val bookmarks by plannerViewModel.bookmarks.collectAsState()
    val plannerBookmarks by plannerViewModel.plannerBookmarks.collectAsState()
    val useIntentionPrompt by plannerViewModel.useIntentionPrompt.collectAsState()
    val sessionTotals by plannerViewModel.sessionTotals.collectAsState()
    val activePrayers by plannerViewModel.activePrayers.collectAsState()
    val readPreference by plannerViewModel.readPreference.collectAsState()
    val context = LocalContext.current

    var showSettingsDrawer by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(if (activePlan != null) "dashboard" else "intention") }
    var currentTab by remember { mutableStateOf(PlannerTab.TODAY) }

    // Dialog state
    var showAdjustPaceDialog by remember { mutableStateOf(false) }
    var showRebalanceDialog by remember { mutableStateOf(false) }
    var showPlannerSettingsDialog by remember { mutableStateOf(false) }
    var showArchivesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCustomModal by remember { mutableStateOf(false) }

    val todayStr = remember { PlannerEngine.formatPlannerDate() }
    val plannerListState = rememberLazyListState()

    LaunchedEffect(activePlan) {
        if (activePlan != null && viewMode == "intention" && allPlans.isNotEmpty()) {
            viewMode = "dashboard"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // Top Global Navbar
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
            // ─── INTENTION VIEW (MATCHING WEB LINES 220-560) ───
            var selectedPaceId by remember { mutableStateOf("steady") }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // 1. Hero Header with "Continue active plan", Watermark, Title & Subtitle
                item {
                    IntentionHero(
                        hasActivePlan = activePlan != null,
                        onViewActive = { viewMode = "dashboard" }
                    )
                }

                // 2. Preset Pace Cards (Horizontal scroll row)
                item {
                    PresetPacesRow(
                        selectedPaceId = selectedPaceId,
                        onSelectPace = { selectedPaceId = it },
                        onBeginPlan = { paceItem ->
                            val built = PlannerEngine.buildReadingPlanner(
                                unitType = "page",
                                durationDays = paceItem.durationDays,
                                startDate = todayStr,
                                startUnit = 1,
                                endUnit = 604,
                                customTitle = paceItem.title,
                                excludeDays = emptyList(),
                                chapters = chapters
                            )
                            plannerViewModel.setActivePlan(built)
                            viewMode = "dashboard"
                        }
                    )
                }

                // 3. My Plans Section (with Archives button, + custom plan button, plan list)
                item {
                    MyPlansSection(
                        planners = allPlans,
                        activePlannerId = activePlannerId,
                        archivedCount = archivedPlans.size,
                        onSwitchPlan = { planId ->
                            plannerViewModel.switchActivePlan(planId)
                            viewMode = "dashboard"
                        },
                        onDeletePlan = { planId ->
                            plannerViewModel.deletePlan(planId)
                        },
                        onOpenCustomPlanModal = { showCustomModal = true },
                        onOpenArchivesModal = { showArchivesDialog = true }
                    )
                }

                // 4. Plan Templates Library (Curated paths)
                item {
                    PlanTemplatesSection(
                        chapters = chapters,
                        onSelectTemplate = { built ->
                            plannerViewModel.setActivePlan(built)
                            viewMode = "dashboard"
                        }
                    )
                }

                // 5. Prophetic Wisdom Quote at Bottom
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\"The best of deeds are those that are consistent, even if they are few.\"",
                            fontFamily = fontFamilyBody,
                            fontSize = 13.5.sp,
                            fontStyle = FontStyle.Italic,
                            color = hInkMid,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PROPHETIC WISDOM",
                            fontFamily = fontFamilyMono,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = hInkMuted
                        )
                    }
                }
            }
        } else {
            // ─── ACTIVE VIEW / DASHBOARD (MATCHING WEB LINES 833-1318) ───
            val plan = activePlan!!
            val overview = remember(plan) { PlannerEngine.getPlannerOverview(plan) }
            val metrics = remember(plan) { PlannerEngine.getPlannerSuccessMetrics(plan) }
            val currentDay = overview?.currentDayNumber ?: 1

            val todayAssignment = remember(plan, todayStr) {
                plan.assignments.find { it.date == todayStr }
                    ?: plan.assignments.getOrNull(currentDay - 1)
                    ?: plan.assignments.firstOrNull()
            }

            val todayProgress = remember(plan, todayAssignment) {
                if (todayAssignment != null) PlannerEngine.getAssignmentProgress(plan, todayAssignment) else null
            }
            val todayComplete = todayProgress?.isComplete ?: false
            val todayDone = todayProgress?.readPagesCount ?: 0
            val todayTotal = (todayProgress?.totalPagesCount ?: 1).coerceAtLeast(1)
            val todayPct = Math.round((todayDone.toFloat() / todayTotal) * 100).coerceIn(0, 100)

            val prayerSlots = remember(plan, todayAssignment, prayerTimings, readPreference, activePrayers) {
                buildPrayerSlots(
                    plan = plan,
                    todayAssignment = todayAssignment,
                    prayerTimings = prayerTimings,
                    readPreference = readPreference,
                    activePrayers = activePrayers
                )
            }

            val nextPrayer = prayerSlots.find { it.status == "current" || it.status == "upcoming" }
            val unitsLabel = PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
            val overallPct = Math.round((overview?.completionRatio ?: 0f) * 100)
            val completionDate = remember(plan) {
                val cal = Calendar.getInstance()
                val parts = plan.startDate.split("-")
                if (parts.size == 3) {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    cal.add(Calendar.DAY_OF_YEAR, plan.durationDays - 1)
                    val mNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    "${mNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
                } else "—"
            }

            val nextAssignment = remember(plan, todayComplete, todayAssignment) {
                if (!todayComplete) null
                else {
                    plan.assignments.firstOrNull { a ->
                        if (todayAssignment != null && a.dayNumber <= todayAssignment.dayNumber) false
                        else !PlannerEngine.getAssignmentProgress(plan, a).isComplete
                    }
                }
            }

            val planDone = overview?.isFinishedWindow == true && nextAssignment == null

            val daySubtitle = when {
                planDone -> "Plan complete! 🎉"
                todayAssignment == null -> "Starting soon…"
                todayComplete && nextAssignment != null -> "Day $currentDay complete! 🎉 · Day ${nextAssignment.dayNumber} ready"
                todayComplete -> "Day $currentDay complete! 🎉"
                todayDone > 0 && nextPrayer != null -> "$todayDone of $todayTotal $unitsLabel read · ${nextPrayer.name} next"
                nextPrayer != null -> "$todayTotal $unitsLabel today · start with ${nextPrayer.name}"
                else -> "All done for today! ✓"
            }

            val ctaLabel = when {
                planDone -> "Plan Complete ✓"
                todayComplete && nextAssignment != null -> "Continue to Day ${nextAssignment.dayNumber}"
                todayComplete -> "All Caught Up"
                todayDone > 0 -> "Resume Reading"
                else -> "Open Al-Quran"
            }

            val todaySessionSeconds = sessionTotals[todayAssignment?.dayNumber ?: 1] ?: 0L
            val overdueCount = plan.assignments.count { PlannerEngine.getAssignmentStatus(plan, it) == "overdue" }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = plannerListState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Global Header: Back button, plan title, day subtitle, and primary CTA
                item {
                    PlanDashboardHeader(
                        planner = plan,
                        currentDay = currentDay,
                        daySubtitle = daySubtitle,
                        ctaLabel = ctaLabel,
                        ctaEnabled = true,
                        onPrimaryCtaClick = {
                            val targetDay = if (todayComplete && nextAssignment != null) nextAssignment.dayNumber else (todayAssignment?.dayNumber ?: currentDay)
                            onReadAssignment(targetDay)
                        },
                        allPlans = allPlans,
                        onSwitchPlan = { planId -> plannerViewModel.switchActivePlan(planId) },
                        onBackToIntention = { viewMode = "intention" }
                    )
                }

                // 2. Stats Header 3-Card Row (OVERALL, FINISH BY, STREAK)
                item {
                    PlanStatsHeaderCards(
                        overallPct = overallPct,
                        completionDate = completionDate,
                        streakDays = metrics?.consistencyStreak ?: 0
                    )
                }

                // 3. Tab Navigation Segmented Bar (Today, Progress, Journal)
                item {
                    PlanTabBar(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }

                // 4. Tab Contents
                item {
                    when (currentTab) {
                        PlannerTab.TODAY -> {
                            PlanTodayTab(
                                planner = plan,
                                todayAssignment = todayAssignment,
                                todayPct = todayPct,
                                todayDone = todayDone,
                                todayTotal = todayTotal,
                                unitsLabel = unitsLabel,
                                todaySessionSeconds = todaySessionSeconds,
                                prayerSlots = prayerSlots,
                                hasStartedReading = todayDone > 0,
                                onStartReading = {
                                    val targetDay = todayAssignment?.dayNumber ?: currentDay
                                    onReadAssignment(targetDay)
                                },
                                onMarkPrayerDone = { slot ->
                                    if (todayAssignment != null) {
                                        plannerViewModel.setPlannerAssignmentProgress(todayAssignment.dayNumber, slot.slotEnd)
                                    }
                                },
                                onUndoPrayer = { slot ->
                                    if (todayAssignment != null) {
                                        plannerViewModel.setPlannerAssignmentProgress(todayAssignment.dayNumber, slot.slotStart)
                                    }
                                },
                                onShareProgress = {
                                    val shareText = "📖 Quran Reading Progress\n" +
                                        "${plan.title}: $overallPct% complete\n" +
                                        "Completed ${overview?.completedCount ?: 0} days of my ${plan.durationDays}-day plan • Day $currentDay • ${metrics?.consistencyStreak ?: 0}d streak\n" +
                                        "#QuranNur"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Progress"))
                                },
                                onOpenSettings = { showPlannerSettingsDialog = true }
                            )
                        }

                        PlannerTab.PROGRESS -> {
                            PlanProgressTab(
                                planner = plan,
                                completedDays = overview?.completedCount ?: 0,
                                overdueDays = overdueCount,
                                onOpenRebalance = { showRebalanceDialog = true },
                                onOpenAdjustPace = { showAdjustPaceDialog = true },
                                onExtendPlan = { plannerViewModel.rebalancePlan("extend") },
                                onSpreadPlan = { plannerViewModel.rebalancePlan("spread") },
                                onReadAssignment = onReadAssignment
                            )
                        }

                        PlannerTab.JOURNAL -> {
                            PlanJournalTab(
                                reflections = plan.assignmentReflections.mapKeys { it.key.toString() },
                                bookmarks = plannerBookmarks,
                                onNavigateToVerse = { surah, _ ->
                                    val targetAssign = plan.assignments.find { a ->
                                        a.items.any { it.rangeValue == "$surah" }
                                    }
                                    if (targetAssign != null) {
                                        onReadAssignment(targetAssign.dayNumber)
                                    } else {
                                        onReadAssignment(currentDay)
                                    }
                                }
                            )
                        }
                    }
                }

                // 5. Footer: Delete Plan Button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = hWhite,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark),
                            modifier = Modifier.clickable { showDeleteConfirmDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = NurIcons.Trash2,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Delete plan",
                                    fontFamily = fontFamilyBody,
                                    fontSize = 13.5.sp,
                                    color = hInkMid
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── DIALOGS ───

    // Create Custom Plan Modal
    if (showCustomModal) {
        CustomPlanModal(
            chapters = chapters,
            onDismiss = { showCustomModal = false },
            onCreatePlan = { built ->
                plannerViewModel.setActivePlan(built)
                viewMode = "dashboard"
            }
        )
    }

    // Adjust Pace Modal
    if (showAdjustPaceDialog && activePlan != null) {
        AdjustPaceDialog(
            currentDurationDays = activePlan!!.durationDays,
            onConfirmNewDuration = { newDays ->
                plannerViewModel.adjustActivePlannerPace(newDays)
                showAdjustPaceDialog = false
            },
            onDismiss = { showAdjustPaceDialog = false }
        )
    }

    // Planner Settings Modal
    if (showPlannerSettingsDialog) {
        PlannerSettingsDialog(
            activePrayers = activePrayers,
            onTogglePrayer = { prayerName ->
                val next = if (activePrayers.contains(prayerName)) {
                    if (activePrayers.size > 1) activePrayers - prayerName else activePrayers
                } else {
                    activePrayers + prayerName
                }
                plannerViewModel.setActivePrayers(next)
            },
            readingPreference = readPreference,
            onSelectPreference = { pref ->
                plannerViewModel.setReadPreference(pref)
            },
            useIntentionPrompt = useIntentionPrompt,
            onToggleIntentionPrompt = { plannerViewModel.setUseIntentionPrompt(it) },
            onDismiss = { showPlannerSettingsDialog = false }
        )
    }

    // Smart Rebalance Modal
    if (showRebalanceDialog) {
        RebalancePlanDialog(
            onExtend = { plannerViewModel.rebalancePlan("extend") },
            onSpread = { plannerViewModel.rebalancePlan("spread") },
            onDismiss = { showRebalanceDialog = false }
        )
    }

    // Archives Modal
    if (showArchivesDialog) {
        ArchivesDialog(
            archivedPlans = archivedPlans,
            onDismiss = { showArchivesDialog = false }
        )
    }

    // Delete Confirmation Modal
    if (showDeleteConfirmDialog && activePlan != null) {
        DeletePlanConfirmDialog(
            planTitle = activePlan!!.title,
            onConfirmDelete = {
                plannerViewModel.deletePlan(activePlan!!.id)
                showDeleteConfirmDialog = false
                viewMode = "intention"
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Settings Drawer
    if (showSettingsDrawer) {
        SettingsDrawer(
            viewModel = surahViewModel,
            onDismiss = { showSettingsDrawer = false }
        )
    }
}
