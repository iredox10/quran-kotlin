package com.nur.quran.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.viewmodels.HomeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
    var isRendering by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

                // Card Preview Content (also used for image export)
                ShareCardContent(type = type)

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons (text) — kept as-is
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

                    // Native Share Intent Button (text)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Image export buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Save Image -> MediaStore Pictures/QuranNur + Toast
                    OutlinedButton(
                        onClick = {
                            if (isRendering) return@OutlinedButton
                            scope.launch {
                                isRendering = true
                                imageError = null
                                try {
                                    val file = renderCardToFile(context, type)
                                    if (file == null) {
                                        imageError = "Could not render image. Try again."
                                    } else {
                                        val saved = saveImageToGallery(context, file)
                                        if (saved) {
                                            Toast.makeText(context, "Image saved to Pictures/QuranNur", Toast.LENGTH_LONG).show()
                                        } else {
                                            imageError = "Could not save image. Try again."
                                        }
                                    }
                                } catch (e: Exception) {
                                    imageError = "Could not save image. Try again."
                                } finally {
                                    isRendering = false
                                }
                            }
                        },
                        enabled = !isRendering,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                    ) {
                        if (isRendering) {
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

                    // Share Image -> FileProvider + ACTION_SEND image/png chooser
                    Button(
                        onClick = {
                            if (isRendering) return@Button
                            scope.launch {
                                isRendering = true
                                imageError = null
                                try {
                                    val file = renderCardToFile(context, type)
                                    if (file == null) {
                                        imageError = "Could not render image. Try again."
                                    } else {
                                        val shared = shareImageFile(context, file)
                                        if (!shared) {
                                            imageError = "Could not share image. Try again."
                                        }
                                    }
                                } catch (e: Exception) {
                                    imageError = "Could not share image. Try again."
                                } finally {
                                    isRendering = false
                                }
                            }
                        },
                        enabled = !isRendering,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hInk)
                    ) {
                        if (isRendering) {
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
                        text = imageError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Preview card content, extracted so the on-screen preview and the
 * off-screen image renderer share the exact same composable.
 */
@Composable
private fun ShareCardContent(type: ShareCardType) {
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
                                    fontFamily = fontFamilyMono
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
}

/**
 * Local renderer: draws [ShareCardContent] into a Bitmap via an off-screen
 * ComposeView (measure/layout/draw) and writes a PNG into
 * cacheDir/shared_images/. Never throws — returns null on failure.
 */
private suspend fun renderCardToFile(
    context: Context,
    type: ShareCardType,
    widthPx: Int = 1080
): File? = withContext(Dispatchers.Main) {
    try {
        val composeView = ComposeView(context).apply {
            setContent { ShareCardContent(type = type) }
        }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
        val measuredW = composeView.measuredWidth.coerceAtLeast(1)
        val measuredH = composeView.measuredHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(measuredW, measuredH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        composeView.draw(canvas)

        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
                val file = File(dir, "share_card_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        return@withContext null
                    }
                }
                file.takeIf { it.exists() && it.length() > 0 }
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

/** Writes the rendered PNG into MediaStore Pictures/QuranNur. Never throws. */
private suspend fun saveImageToGallery(context: Context, file: File): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            val displayName = "quran_nur_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QuranNur")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        return@withContext false
                    }
                } ?: return@withContext false
            } catch (e: Exception) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) { }
                return@withContext false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

/** Shares the rendered PNG via FileProvider + ACTION_SEND chooser. Never throws. */
private fun shareImageFile(context: Context, file: File): Boolean {
    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            setType("image/png")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share image via"))
        true
    } catch (e: Exception) {
        false
    }
}
