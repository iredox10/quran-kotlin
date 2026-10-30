package com.nur.quran

import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Intent
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
import com.nur.quran.ui.screens.PageScreen
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.LibraryViewModel
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.components.NurIcons
import dagger.hilt.android.AndroidEntryPoint
import com.nur.quran.ui.screens.*

// ── Color Palette (now using shared colors from SurahScreen.kt) ────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.nur.quran.i18n.AppLanguages.wrap(newBase))
    }

    private val homeViewModel: HomeViewModel by viewModels()
    private val surahViewModel: SurahViewModel by viewModels()
    private val plannerViewModel: PlannerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val packViewModel: PackViewModel by viewModels()

    private var pendingDeepLink by mutableStateOf<Pair<Int, String?>?>(null)

    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return
        val isViewVerse = intent.action == "com.nur.quran.action.VIEW_VERSE" ||
            intent.hasExtra("chapterId") ||
            intent.data?.scheme == "quran"
        if (isViewVerse) {
            val uri = intent.data
            val uriChapterId = uri?.pathSegments?.firstOrNull()?.toIntOrNull()
            val uriVerseKey = uri?.pathSegments?.getOrNull(1)?.takeIf { it != "none" }
            val chapterId = uriChapterId ?: intent.getIntExtra("chapterId", 1)

            // If audio is actively playing, prefer the live reciting verse:
            val currentKey = surahViewModel.playingVerseKey.value
            val currentChapterId = currentKey?.split(":")?.firstOrNull()?.toIntOrNull()
            val effectiveChapterId = if (surahViewModel.isPlaying.value && currentChapterId != null) currentChapterId else chapterId
            val effectiveVerseKey = if (surahViewModel.isPlaying.value && !currentKey.isNullOrBlank()) {
                currentKey
            } else {
                uriVerseKey ?: intent.getStringExtra("verseKey")
            }

            pendingDeepLink = effectiveChapterId to effectiveVerseKey
            effectiveVerseKey?.let { surahViewModel.requestScrollToVerse(it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // App backgrounded: checkpoint-flush the active reading session.
            // flushReadingSession() resets start=now, so resume continues cleanly
            // (timer restarts naturally, no double-count on return).
            runCatching { surahViewModel.flushReadingSession() }
            // Persist pending edits when leaving the app; retry once connectivity returns.
            com.nur.quran.data.sync.SyncWorker.enqueueOnce(this@MainActivity)
            // Drop the realtime sync subscription; it re-opens on foreground.
            runCatching { packViewModel.stopRealtimeSync() }
        }

        override fun onStart(owner: LifecycleOwner) {
            // Returning to foreground: if audio is playing, resume tracking and scroll to active reciting ayah
            val activeKey = surahViewModel.playingVerseKey.value
            if (surahViewModel.isPlaying.value && !activeKey.isNullOrBlank()) {
                surahViewModel.requestScrollToVerse(activeKey)
            }
            // Re-open live cross-device sync (signed in only; no-op otherwise).
            runCatching { packViewModel.startRealtimeSync() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Locale/configuration recreation restores the current NavController state.
        // Replaying the original intent here would jump back to its old ayah.
        if (savedInstanceState == null) handleDeepLink(intent)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        
        // Initialize global dark theme preference before composing.
        // "Settings" is canonical (every toggle writes it); fall back to the
        // legacy "hifdh_settings" copy so old installs keep their saved theme.
        val prefs = getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("hifdh_settings", android.content.Context.MODE_PRIVATE)
        isDarkThemeGlobal = when {
            prefs.contains("is_dark_theme") -> prefs.getBoolean("is_dark_theme", false)
            legacyPrefs.contains("is_dark_theme") -> legacyPrefs.getBoolean("is_dark_theme", false)
            else -> false
        }
        
        setContent {
            // Remote restores write Settings too; observe them while the app is open.
            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { source, key ->
                    if (key == "is_dark_theme") isDarkThemeGlobal = source.getBoolean(key, false)
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val navController = rememberNavController()

            LaunchedEffect(pendingDeepLink) {
                pendingDeepLink?.let { (chapterId, verseKey) ->
                    val currentBackStack = navController.currentBackStackEntry
                    val currentRoute = currentBackStack?.destination?.route
                    val curChapterId = currentBackStack?.arguments?.getInt("chapterId")

                    if (currentRoute == Screen.SurahDetail.route && curChapterId == chapterId) {
                        verseKey?.let { surahViewModel.requestScrollToVerse(it) }
                    } else {
                        navController.navigate(Screen.SurahDetail.createRoute(chapterId, verseKey)) {
                            popUpTo(Screen.Quran.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        verseKey?.let { surahViewModel.requestScrollToVerse(it) }
                    }
                    pendingDeepLink = null
                }
            }

            MaterialTheme {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Bottom bar hidden on the Planner section (own in-screen navigation).
                val shouldShowBottomBar = currentDestination?.route in listOf(
                    Screen.Quran.route,
                    Screen.Memorize.route,
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
                                onNavigateToRoute = { route ->
                                    when (route) {
                                        "planner" -> navController.navigate(Screen.Planner.route)
                                        // Web OnboardingProgress + Home tour: Hifdh entry opens /memorize/1.
                                        "memorize" -> navController.navigate(Screen.MemorizeDetail.createRoute(1))
                                        "surah" -> navController.navigate(Screen.SurahDetail.createRoute(1))
                                        "library" -> navController.navigate(Screen.Library.route)
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
                                },
                                onGoalResume = { chapterId, verseKey ->
                                    navController.navigate(Screen.MemorizeDetail.createRoute(chapterId, verseKey))
                                },
                                onBack = {
                                    navController.navigate(Screen.Quran.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable(
                            route = Screen.MemorizeDetail.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("chapterId") { type = androidx.navigation.NavType.IntType },
                                androidx.navigation.navArgument("verseKey") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { backStackEntry ->
                            val args = backStackEntry.arguments
                            val chapterId = runCatching { args?.getInt("chapterId") }.getOrNull()
                                ?.takeIf { it != 0 }
                                ?: args?.getString("chapterId")?.toIntOrNull() ?: 1
                            HifdhReaderScreen(
                                surahViewModel = surahViewModel,
                                chapterId = chapterId,
                                startVerseKey = args?.getString("verseKey"),
                                // Web Layout.jsx: memorize-detail back always lands on /memorize.
                                onBackClick = {
                                    navController.navigate(Screen.Memorize.route) {
                                        popUpTo(Screen.Memorize.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Planner.route) {
                            PlannerScreen(
                                homeViewModel = homeViewModel,
                                surahViewModel = surahViewModel,
                                plannerViewModel = plannerViewModel,
                                onReadAssignment = { dayNumber ->
                                    navController.navigate(Screen.PlannerReaderDetail.createRoute(dayNumber))
                                },
                                onOpenVerse = { surah, verseKey ->
                                    navController.navigate(Screen.SurahDetail.createRoute(surah, verseKey))
                                },
                                onBack = {
                                    navController.navigate(Screen.Quran.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                                },
                                onOpenLibraryClick = {
                                    navController.navigate(Screen.Library.route)
                                },
                                onOpenCollectionsClick = {
                                    navController.navigate(Screen.Library.route)
                                },
                                onOpenActivityHistoryClick = {
                                    navController.navigate(Screen.ActivityHistory.route)
                                },
                                onOpenRecentSurahsClick = {
                                    navController.navigate(Screen.RecentSurahs.route)
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
                        composable(Screen.RecentSurahs.route) {
                            com.nur.quran.ui.screens.RecentSurahsScreen(
                                homeViewModel = homeViewModel,
                                onOpenSurah = { chapterId, verseKey ->
                                    navController.navigate(Screen.SurahDetail.createRoute(chapterId, verseKey))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.ActivityHistory.route) {
                            com.nur.quran.ui.screens.ActivityHistoryScreen(
                                homeViewModel = homeViewModel,
                                chapters = surahViewModel.allChapters.value,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Library.route) {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                onBackClick = { navController.popBackStack() },
                                onNavigateToVerse = { chapterId, verseKey ->
                                    navController.navigate(Screen.SurahDetail.createRoute(chapterId, verseKey))
                                },
                                onLaunchHifdh = { chapterId ->
                                    navController.navigate(Screen.MemorizeDetail.createRoute(chapterId))
                                }
                            )
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
                            PageScreen(
                                pageNumber = pageNum,
                                onBackClick = { navController.popBackStack() },
                                onNavigateToPage = { newPage ->
                                    navController.navigate(Screen.PageDetail.createRoute(newPage)) {
                                        popUpTo(Screen.PageDetail.route) { inclusive = true }
                                    }
                                }
                            )
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
                                NavItem(Screen.Quran, NurIcons.BookOpen, stringResource(R.string.ui_quran_f5df39)),
                                NavItem(Screen.Memorize, NurIcons.Brain, stringResource(R.string.ui_memorize_8a8a35)),
                                NavItem(Screen.Planner, NurIcons.CalendarDays, stringResource(R.string.ui_planner_eca1f9)),
                                NavItem(Screen.Analytics, NurIcons.TrendingUp, stringResource(R.string.ui_analytics_25bc96)),
                                NavItem(Screen.Profile, NurIcons.User, stringResource(R.string.ui_profile_ff4fc0))
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
