package com.nur.quran.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.core.content.FileProvider
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ShareVerseDialog(
    verse: VerseEntity,
    chapterName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isImageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }

    // Card inputs derived from the dialog's existing verse props,
    // matching the existing share-text format ("— chapterName verseKey").
    val cardArabic = verse.textUthmani ?: verse.textIndopak ?: ""
    val cardTranslation = verse.translation?.replace(Regex("<[^>]*>"), "")?.takeIf { it.isNotBlank() }
    val cardReference = "$chapterName ${verse.verseKey}"
    val imageFileName = "Quran_${verse.verseKey.replace(Regex("[^A-Za-z0-9]+"), "_")}.png"

    fun onSaveImage() {
        if (isImageBusy) return
        scope.launch {
            isImageBusy = true
            imageError = null
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    renderVerseCardToFile(context, verse.verseKey, cardArabic, cardTranslation, cardReference)
                } ?: throw IllegalStateException("render failed")
                saveVerseCardToGallery(context, file, imageFileName).getOrThrow()
            }.onSuccess { location ->
                Toast.makeText(context, "Saved to $location", Toast.LENGTH_LONG).show()
            }.onFailure {
                imageError = "Couldn't render image"
                Toast.makeText(context, "Couldn't save image", Toast.LENGTH_SHORT).show()
            }
            isImageBusy = false
        }
    }

    fun shareTextMessage(message: String) {
        // Tier 1: system share sheet. Tier 2: clipboard + toast (web parity:
        // text-only share -> download+clipboard fallback). Never throws.
        runCatching {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                setType("text/plain")
            }
            val chooser = Intent.createChooser(sendIntent, "Share Ayah")
            if (context !is android.app.Activity) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            onDismiss()
        }.onFailure {
            runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("QuranNurVerse", message))
            }
            Toast.makeText(context, "Sharing unavailable — verse copied instead", Toast.LENGTH_LONG).show()
        }
    }

    fun onShareImage() {
        if (isImageBusy) return
        scope.launch {
            isImageBusy = true
            imageError = null
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    renderVerseCardToFile(context, verse.verseKey, cardArabic, cardTranslation, cardReference)
                } ?: throw IllegalStateException("render failed")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val cleanTranslation = verse.translation?.replace(Regex("<[^>]*>"), "") ?: ""
                val shareMessage = "${verse.textUthmani ?: ""}\n\n$cleanTranslation\n— $chapterName ${verse.verseKey}\n\nRead & study via Quran Nur: https://quran-nur.appwrite.network"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Ayah image")
                if (context !is android.app.Activity) {
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                // Web parity: file share if a handler exists, else text share.
                val handlesFile = shareIntent.resolveActivity(context.packageManager) != null
                if (handlesFile) {
                    context.startActivity(chooser)
                } else {
                    shareTextMessage(shareMessage)
                }
            }.onFailure {
                imageError = "Couldn't render image"
            }
            isImageBusy = false
        }
    }

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
                            shareTextMessage(shareMessage)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Image Export Buttons (PNG card via ShareImageRenderer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Save Image Button
                    OutlinedButton(
                        onClick = { onSaveImage() },
                        enabled = !isImageBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                    ) {
                        if (isImageBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = hInkMid
                            )
                        } else {
                            Icon(
                                imageVector = NurIcons.Download,
                                contentDescription = null,
                                tint = hInkMid,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Image",
                            color = hInkMid,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Share Image Button
                    Button(
                        onClick = { onShareImage() },
                        enabled = !isImageBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hTeal)
                    ) {
                        if (isImageBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = NurIcons.Share2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share Image",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (imageError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = imageError!!,
                        color = hRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Copies a rendered verse-card PNG into the public gallery (MediaStore on Q+,
 * app-external Pictures dir on pre-Q). Returns the user-facing location string.
 */
private suspend fun saveVerseCardToGallery(
    context: Context,
    source: File,
    displayName: String
): Result<String> = runCatching {
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QuranNur")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            } ?: throw IllegalStateException("MediaStore write failed")
            "Pictures/QuranNur/$displayName"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "QuranNur")
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, displayName)
            source.copyTo(dest, overwrite = true)
            dest.absolutePath
        }
    }
}
