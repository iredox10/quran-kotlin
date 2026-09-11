package com.nur.quran

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nur.quran.ui.navigation.Screen
import com.nur.quran.ui.screens.HomeScreen
import com.nur.quran.ui.screens.SurahScreen
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.components.NurIcons
import dagger.hilt.android.AndroidEntryPoint
import com.nur.quran.ui.screens.*

// ── Color Palette (now using shared colors from SurahScreen.kt) ────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val surahViewModel: SurahViewModel by viewModels()
    private val plannerViewModel: PlannerViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // App backgrounded: checkpoint-flush the active reading session.
            // flushReadingSession() resets start=now, so resume continues cleanly
            // (timer restarts naturally, no double-count on return).
            runCatching { surahViewModel.flushReadingSession() }
        }

        override fun onStart(owner: LifecycleOwner) {
            // Nothing: timer restarts naturally via checkpoint start=now.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        
        // Initialize global dark theme preference before composing
        val prefs = getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
        isDarkThemeGlobal = prefs.getBoolean("is_dark_theme", false)
        
        setContent {
            val navController = rememberNavController()

            MaterialTheme {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val shouldShowBottomBar = currentDestination?.route in listOf(
                    Screen.Quran.route,
                    Screen.Memorize.route,
                    Screen.Planner.route,
                    Screen.Analytics.route,
                    Screen.Profile.route
                )

                var showSplashScreen by remember { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // ── Main Content ─────────────────────────────
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Quran.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Quran.route) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                surahViewModel = surahViewModel,
                                onChapterClick = { chapterId, verseKey ->
                                    navController.navigate(Screen.SurahDetail.createRoute(chapterId, verseKey))
                                },
                                onPageClick = { pageNum ->
                                    navController.navigate(Screen.PageDetail.createRoute(pageNum))
                                },
                                onNavigateToSauka = {
                                    navController.navigate(Screen.Planner.route)
                                },
                                onNavigateToBookmarks = {
                                    navController.navigate(Screen.Analytics.route)
                                },
                                onNavigateToRoute = { route ->
                                    when (route) {
                                        "planner" -> navController.navigate(Screen.Planner.route)
                                        "memorize" -> navController.navigate(Screen.Memorize.route)
                                        "surah" -> navController.navigate(Screen.SurahDetail.createRoute(1))
                                        "library" -> navController.navigate(Screen.Analytics.route)
                                    }
                                }
                            )
                        }
                        composable(Screen.Memorize.route) {
                            MemorizeScreen(
                                homeViewModel = homeViewModel,
                                surahViewModel = surahViewModel,
                                onSurahClick = { chapterId ->
                                    navController.navigate(Screen.MemorizeDetail.createRoute(chapterId))
                                }
                            )
                        }
                        composable(Screen.MemorizeDetail.route) { backStackEntry ->
                            val chapterId = backStackEntry.arguments?.getString("chapterId")?.toIntOrNull() ?: 1
                            HifdhReaderScreen(
                                surahViewModel = surahViewModel,
                                chapterId = chapterId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Planner.route) {
                            PlannerScreen(
                                homeViewModel = homeViewModel,
                                surahViewModel = surahViewModel,
                                plannerViewModel = plannerViewModel,
                                onReadAssignment = { dayNumber ->
                                    navController.navigate(Screen.PlannerReaderDetail.createRoute(dayNumber))
                                }
                            )
                        }
                        composable(Screen.PlannerReaderDetail.route) { backStackEntry ->
                            val dayNumber = backStackEntry.arguments?.getString("dayNumber")?.toIntOrNull() ?: 1
                            PlannerReaderScreen(
                                dayNumber = dayNumber,
                                plannerViewModel = plannerViewModel,
                                surahViewModel = surahViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Analytics.route) {
                            com.nur.quran.ui.screens.AnalyticsScreen(
                                homeViewModel = homeViewModel,
                                surahViewModel = surahViewModel,
                                onOpenQuranClick = {
                                    navController.navigate(Screen.Quran.route) {
                                        popUpTo(Screen.Quran.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Profile.route) {
                            com.nur.quran.ui.screens.ProfileScreen(
                                homeViewModel = homeViewModel,
                                surahViewModel = surahViewModel,
                                plannerViewModel = plannerViewModel,
                                onNavigateToScreen = { route -> navController.navigate(route) }
                            )
                        }
                        composable(Screen.Downloads.route) {
                            val audioPacksVm: com.nur.quran.ui.viewmodels.AudioPacksViewModel =
                                androidx.hilt.navigation.compose.hiltViewModel()
                            DownloadsScreen(packVm = audioPacksVm, onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.SurahDetail.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("chapterId") { type = androidx.navigation.NavType.IntType },
                                androidx.navigation.navArgument("verseKey") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
                            ),
                            enterTransition = { androidx.compose.animation.EnterTransition.None },
                            exitTransition = { androidx.compose.animation.ExitTransition.None },
                            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                            popExitTransition = { androidx.compose.animation.ExitTransition.None }
                        ) { backStackEntry ->
                            val chapterId = backStackEntry.arguments?.getInt("chapterId") ?: 1
                            val targetVerseKey = backStackEntry.arguments?.getString("verseKey")
                            SurahScreen(
                                viewModel = surahViewModel,
                                chapterId = chapterId,
                                targetVerseKey = targetVerseKey,
                                onBackClick = { navController.popBackStack() },
                                onNavigateToSurah = { nextChapterId, verseKey ->
                                    navController.navigate(Screen.SurahDetail.createRoute(nextChapterId, verseKey)) {
                                        popUpTo(Screen.Quran.route) { inclusive = false }
                                    }
                                }
                            )
                        }
                        composable(Screen.PageDetail.route) { backStackEntry ->
                            val pageNum = backStackEntry.arguments?.getString("pageNumber")?.toIntOrNull() ?: 1
                            Text(text = "Page Detail Screen for Page $pageNum")
                        }
                    }

                    // ── Floating Pill Bottom Nav ─────────────────
                    if (shouldShowBottomBar) {
                        FloatingBottomNav(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .widthIn(max = 500.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            items = listOf(
                                NavItem(Screen.Quran, NurIcons.BookOpen, "Quran"),
                                NavItem(Screen.Memorize, NurIcons.Brain, "Memorize"),
                                NavItem(Screen.Planner, NurIcons.CalendarDays, "Planner"),
                                NavItem(Screen.Analytics, NurIcons.TrendingUp, "Analytics"),
                                NavItem(Screen.Profile, NurIcons.User, "Profile")
                            ),
                            currentRoute = currentDestination?.route,
                            onItemClick = { screen ->
                                if (currentDestination?.route != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }

                    if (showSplashScreen) {
                        com.nur.quran.ui.components.SplashScreen(
                            onFinish = { showSplashScreen = false }
                        )
                    }
                }
            }
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED &&
                !prefs.getBoolean("notif_perm_asked", false)
            ) {
                prefs.edit().putBoolean("notif_perm_asked", true).apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        super.onDestroy()
    }
}

data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
private fun FloatingBottomNav(
    modifier: Modifier = Modifier,
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (Screen) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(100),
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x1F000000)
            ),
        shape = RoundedCornerShape(100),
        color = hWhite.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            hBoneDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.screen.route

                if (isSelected) {
                    // Active Tab: horizontal pill layout (matching web app's BottomNav.jsx)
                    Surface(
                        onClick = { onItemClick(item.screen) },
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(100),
                        color = hGoldSoft
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp),
                                tint = hGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = hGold,
                                fontFamily = fontFamilyMono,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    // Inactive Tab: vertical column layout (matching web app's BottomNav.jsx)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100))
                            .clickable { onItemClick(item.screen) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp),
                                tint = hInkMuted
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hInkMuted,
                                fontFamily = fontFamilyMono
                            )
                        }
                    }
                }
            }
        }
    }
}
