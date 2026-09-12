package com.nur.quran.ui.components.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Modular settings group cards matching web app Profile.jsx lines 236-334:
 * - Reading Experience
 * - Onboarding & Tours
 * - Community
 * - App & Storage
 */

@Composable
fun ProfileSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontFamily = fontFamilyMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = hInkMuted,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun ProfileSettingRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hGoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                fontFamily = fontFamilyBody,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = hInk
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    fontFamily = fontFamilyMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = hInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }
            Icon(
                imageVector = NurIcons.ChevronRight,
                contentDescription = null,
                tint = hInkMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ReadingExperienceGroup(
    reciterName: String,
    translationName: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = "READING EXPERIENCE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark)
        ) {
            Column {
                ProfileSettingRow(
                    icon = NurIcons.Settings,
                    label = "Reading Settings",
                    onClick = onOpenSettings
                )
                Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)
                ProfileSettingRow(
                    icon = NurIcons.Mic,
                    label = "Reciter",
                    detail = reciterName,
                    onClick = onOpenSettings
                )
                Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)
                ProfileSettingRow(
                    icon = NurIcons.Languages,
                    label = "Translation",
                    detail = translationName,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
fun OnboardingGroup(
    completedToursCount: Int,
    onReplayTours: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = "ONBOARDING & TOURS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark)
        ) {
            ProfileSettingRow(
                icon = NurIcons.RotateCcw,
                label = "Replay All Tours",
                detail = "${completedToursCount.coerceAtMost(5)} / 5 Completed",
                onClick = onReplayTours
            )
        }
    }
}

@Composable
fun CommunityGroup(
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = "COMMUNITY")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark)
        ) {
            ProfileSettingRow(
                icon = NurIcons.Share2,
                label = "Invite Friends",
                detail = "Share App",
                onClick = onShareApp
            )
        }
    }
}

@Composable
fun AppStorageGroup(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSectionHeader(title = "APP & STORAGE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark)
        ) {
            Column {
                ProfileSettingRow(
                    icon = if (isDarkTheme) NurIcons.Moon else NurIcons.Sun,
                    label = "Appearance",
                    detail = if (isDarkTheme) "Dark" else "Light",
                    onClick = onToggleTheme
                )
                Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)
                ProfileSettingRow(
                    icon = NurIcons.HardDrive,
                    label = "Offline Library",
                    onClick = onNavigateToDownloads
                )
            }
        }
    }
}
