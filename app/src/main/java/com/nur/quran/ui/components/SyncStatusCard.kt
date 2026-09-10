package com.nur.quran.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldLight
import com.nur.quran.ui.screens.hGreen
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hRed
import com.nur.quran.ui.screens.hSurface

/**
 * Props-only sync status card. All state arrives via [syncState]; all actions
 * leave via callbacks — no ViewModel / WorkManager observation in here.
 */
data class SyncUiState(
    val signedIn: Boolean,
    val email: String?,
    val lastSync: Long?,
    val syncing: Boolean,
    val error: String?
)

@Composable
fun SyncStatusCard(
    syncState: SyncUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = hSurface),
        border = BorderStroke(1.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(hGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.RefreshCw,
                            contentDescription = null,
                            tint = hGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cloud Sync",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi
                        )
                        Text(
                            text = if (syncState.signedIn) "AUTO-SYNC EVERY 6H" else "BACK UP YOUR PROGRESS",
                            fontSize = 10.sp,
                            fontFamily = fontFamilyMono,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = hInkMuted
                        )
                    }
                }
                if (syncState.syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = hGold,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (syncState.signedIn) hGreen.copy(alpha = 0.15f)
                                else hInkMuted.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (syncState.signedIn) "ON" else "OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamilyMono,
                            color = if (syncState.signedIn) hGreen else hInkMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!syncState.signedIn) {
                Text(
                    text = "Sign in to back up bookmarks, hifdh progress and settings to the cloud.",
                    fontSize = 13.sp,
                    color = hInkMid,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = hGold,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = NurIcons.User,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Login to sync",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi
                    )
                }
            } else {
                Text(
                    text = syncState.email ?: "Signed in",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Last sync: ${relativeSyncTime(syncState.lastSync)}",
                    fontSize = 12.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyMono
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackup,
                        enabled = !syncState.syncing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = hGold,
                            contentColor = Color.White,
                            disabledContainerColor = hGold.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = NurIcons.RefreshCw,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (syncState.syncing) "Syncing…" else "Backup now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamilyUi
                        )
                    }
                    OutlinedButton(
                        onClick = onRestore,
                        enabled = !syncState.syncing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, hGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = hGold)
                    ) {
                        Icon(
                            imageVector = NurIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Restore",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamilyUi
                        )
                    }
                }
            }

            if (syncState.error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = NurIcons.AlertTriangle,
                        contentDescription = null,
                        tint = hRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = syncState.error,
                        fontSize = 12.sp,
                        color = hRed,
                        fontFamily = fontFamilyUi
                    )
                }
            }
        }
    }
}

private fun relativeSyncTime(lastSync: Long?): String {
    if (lastSync == null) return "never"
    val diff = System.currentTimeMillis() - lastSync
    if (diff < 0) return "just now"
    val mins = diff / 60_000L
    if (mins < 1) return "just now"
    if (mins < 60) return "$mins min ago"
    val hours = mins / 60
    if (hours < 24) return "$hours hr ago"
    val days = hours / 24
    if (days < 30) return "$days day${if (days == 1L) "" else "s"} ago"
    val months = days / 30
    if (months < 12) return "$months mo ago"
    return "${months / 12} yr ago"
}
