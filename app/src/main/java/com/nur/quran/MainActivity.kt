package com.nur.quran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nur.quran.ui.navigation.Screen
import com.nur.quran.ui.screens.HomeScreen
import com.nur.quran.ui.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

// ── Color Palette (web app variables) ────────────────────────────────
private val hCream    = Color(0xFFFAF7F0)
private val hBoneDark = Color(0xFFDDD7C7)
private val hInk      = Color(0xFF2B3F3C)
private val hInkMuted = Color(0xFF8E9B97)
private val hGold     = Color(0xFFB8924A)
private val hGoldSoft = Color(0x26B8924A) // ~15% opacity
private val hTeal     = Color(0xFF2E4F4A)
private val hWhite    = Color(0xFFFAFAF5)
private val glassBg   = Color(0x99EFECE4) // rgba(239,236,228,0.6)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                onChapterClick = { chapterId ->
                                    navController.navigate(Screen.SurahDetail.createRoute(chapterId))
                                },
                                onPageClick = { pageNum ->
                                    navController.navigate(Screen.PageDetail.createRoute(pageNum))
                                }
                            )
                        }
                        composable(Screen.Memorize.route) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(hWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Memorize",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                            }
                        }
                        composable(Screen.Planner.route) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(hWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Planner",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                            }
                        }
                        composable(Screen.Analytics.route) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(hWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Analytics",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                            }
                        }
                        composable(Screen.Profile.route) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(hWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Profile",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                            }
                        }
                        composable(Screen.SurahDetail.route) { backStackEntry ->
                            val chapterId = backStackEntry.arguments?.getString("chapterId")?.toIntOrNull() ?: 1
                            Text(text = "Surah Detail Screen for Chapter $chapterId")
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            items = listOf(
                                NavItem(Screen.Quran, Icons.Outlined.Menu, "Quran"),
                                NavItem(Screen.Memorize, Icons.Outlined.Star, "Memorize"),
                                NavItem(Screen.Planner, Icons.Outlined.DateRange, "Planner"),
                                NavItem(Screen.Analytics, Icons.Outlined.Favorite, "Analytics"),
                                NavItem(Screen.Profile, Icons.Outlined.Person, "Profile")
                            ),
                            currentRoute = currentDestination?.route,
                            onItemClick = { screen ->
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
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
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(100),
                ambientColor = Color(0x12000000),
                spotColor = Color(0x12000000)
            ),
        shape = RoundedCornerShape(100),
        color = glassBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.screen.route

                Box(
                    modifier = Modifier
                        .weight(if (isSelected) 1.5f else 1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100))
                        .then(
                            if (isSelected) Modifier.background(hGoldSoft)
                            else Modifier
                        )
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
                            tint = if (isSelected) hGold else hInkMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = if (isSelected) 10.sp else 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) hGold else hInkMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
