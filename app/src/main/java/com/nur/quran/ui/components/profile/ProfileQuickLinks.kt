package com.nur.quran.ui.components.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.navigation.Screen
import com.nur.quran.ui.screens.*

data class QuickLinkItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Quick Links Row matching web app Profile.jsx lines 216-234.
 */
@Composable
fun ProfileQuickLinks(
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        QuickLinkItem(Screen.Library.route, "Bookmarks", NurIcons.Bookmark),
        QuickLinkItem(Screen.Analytics.route, "Analytics", NurIcons.TrendingUp),
        QuickLinkItem(Screen.Planner.route, "Planner", NurIcons.CalendarDays),
        QuickLinkItem(Screen.Memorize.route, "Memorize", NurIcons.Brain),
        QuickLinkItem(Screen.Planner.route, "Sauka", NurIcons.Users)
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier.clickable { onNavigateToRoute(item.route) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = hCream),
                border = BorderStroke(1.5.dp, hBoneDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(hGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = hGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        fontFamily = fontFamilyUi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                }
            }
        }
    }
}
