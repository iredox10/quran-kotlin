package com.nur.quran.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.viewmodels.HomeStats

sealed class ShareCardType {
    data class Verse(val arabic: String, val translation: String, val reference: String) : ShareCardType()
    data class Progress(val streak: Int, val todayMinutes: Int, val totalHours: String) : ShareCardType()
}

@Composable
fun ShareCardDialog(
    type: ShareCardType,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = hWhite,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (type is ShareCardType.Verse) "Share Verse Card" else "Share Progress Card",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = NurIcons.X,
                            contentDescription = "Close",
                            tint = hInkMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Preview Content
                when (type) {
                    is ShareCardType.Verse -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = hCream,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "﷽",
                                    fontSize = 28.sp,
                                    color = hGold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = type.arabic,
                                    fontFamily = fontFamilyArabic,
                                    fontSize = 22.sp,
                                    color = hInk,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 44.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = type.translation,
                                    fontFamily = fontFamilyBody,
                                    fontSize = 13.sp,
                                    color = hInkMid,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "— ${type.reference}",
                                    fontSize = 11.sp,
                                    color = hInkMuted,
                                    fontFamily = fontFamilyMono
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Quran Nur 🌙",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hTeal,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    is ShareCardType.Progress -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = hTeal
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Brush.verticalGradient(listOf(hTeal, hTealMid)))
                                    .padding(20.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "My Quran Journey",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = fontFamilyUi
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${type.streak}",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = fontFamilyUi
                                            )
                                            Text(
                                                text = "DAY STREAK",
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontFamily = fontFamilyMono
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${type.todayMinutes}m",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = fontFamilyUi
                                            )
                                            Text(
                                                text = "TODAY",
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontFamily = fontFamilyMono
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${type.totalHours}h",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = fontFamilyUi
                                            )
                                            Text(
                                                text = "TOTAL",
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontFamily = fontFamilyMono
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Quran Nur 🌙 · Read & Learn Everyday",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontFamily = fontFamilyMono
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy Text Button
                    OutlinedButton(
                        onClick = {
                            val textToCopy = when (type) {
                                is ShareCardType.Verse -> "${type.arabic}\n\n${type.translation}\n— ${type.reference}\n\nVia Quran Nur"
                                is ShareCardType.Progress -> "My Quran Reading Progress:\n🔥 Streak: ${type.streak} days\n⏱️ Today: ${type.todayMinutes} mins\n📊 Total: ${type.totalHours} hrs\n\nVia Quran Nur"
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QuranNurShare", textToCopy))
                            copied = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (copied) hGreen else hBoneDark)
                    ) {
                        Icon(
                            imageVector = if (copied) NurIcons.Check else NurIcons.Copy,
                            contentDescription = null,
                            tint = if (copied) hGreen else hInkMid,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "Copied!" else "Copy Text",
                            color = if (copied) hGreen else hInkMid,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Native Share Intent Button
                    Button(
                        onClick = {
                            val shareMessage = when (type) {
                                is ShareCardType.Verse -> "${type.arabic}\n\n${type.translation}\n— ${type.reference}\n\nDownloaded via Quran Nur: https://quran-nur.appwrite.network"
                                is ShareCardType.Progress -> "My Quran Reading Progress:\n🔥 Streak: ${type.streak} days\n⏱️ Today: ${type.todayMinutes} mins\n📊 Total: ${type.totalHours} hrs\n\nBuild your daily habit with Quran Nur: https://quran-nur.appwrite.network"
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                setType("text/plain")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hTeal)
                    ) {
                        Icon(
                            imageVector = NurIcons.Share2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
