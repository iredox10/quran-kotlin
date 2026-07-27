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
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.screens.*

@Composable
fun ShareVerseDialog(
    verse: VerseEntity,
    chapterName: String,
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
                        text = "Share Ayah",
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
                            text = verse.textUthmani ?: verse.textIndopak ?: "",
                            fontFamily = fontFamilyArabic,
                            fontSize = 22.sp,
                            color = hInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 44.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!verse.translation.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val cleanTranslation = verse.translation.replace(Regex("<[^>]*>"), "")
                            Text(
                                text = "\"$cleanTranslation\"",
                                fontFamily = fontFamilyBody,
                                fontSize = 13.sp,
                                color = hInkMid,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "— $chapterName ${verse.verseKey}",
                            fontSize = 11.sp,
                            color = hInkMuted,
                            fontFamily = fontFamilyMono
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Quran Nur 🌙",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = hTeal,
                                fontFamily = fontFamilyMono,
                                letterSpacing = 1.sp
                            )
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
                            val cleanTranslation = verse.translation?.replace(Regex("<[^>]*>"), "") ?: ""
                            val textToCopy = "${verse.textUthmani ?: ""}\n\n$cleanTranslation\n— $chapterName ${verse.verseKey}\n\nVia Quran Nur"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QuranNurVerse", textToCopy))
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
                            val cleanTranslation = verse.translation?.replace(Regex("<[^>]*>"), "") ?: ""
                            val shareMessage = "${verse.textUthmani ?: ""}\n\n$cleanTranslation\n— $chapterName ${verse.verseKey}\n\nRead & study via Quran Nur: https://quran-nur.appwrite.network"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                setType("text/plain")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Ayah"))
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
