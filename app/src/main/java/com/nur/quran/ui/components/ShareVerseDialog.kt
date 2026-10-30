package com.nur.quran.ui.components

import androidx.compose.ui.res.stringResource
import com.nur.quran.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.screens.*
import com.nur.quran.utils.TajweedProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ShareVerseDialog(verse: VerseEntity, chapterName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var theme by remember { mutableStateOf(ShareCardTheme.Ivory) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val arabic = remember(verse) { TajweedProcessor.foldWaqfMarkSeparator(verse.textQpcHafs?.takeIf { it.isNotBlank() } ?: verse.textUthmani ?: verse.textIndopak ?: "") }
    val translation = remember(verse) { verse.translation?.replace(Regex("<[^>]*>"), "")?.trim()?.takeIf { it.isNotEmpty() } }
    val reference = "$chapterName • Ayah ${verse.verseKey.substringAfter(':')}"
    val shareText = "$arabic\n\n${translation.orEmpty()}\n— $chapterName ${verse.verseKey}\n\nQuran Nur"
    val page = remember(arabic, translation) { VerseCardPage(arabic, translation.orEmpty()) }
    val preview by produceState<android.graphics.Bitmap?>(null, page, theme, reference) {
        value = null
        error = null
        try {
            value = withContext(Dispatchers.Default) {
                renderVerseCardBitmap(context, page, reference, theme,
                    outputWidth = 720)
            }
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            error = "Couldn't prepare the card. Please try again."
        }
    }
    fun export(save: Boolean) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                val file = renderVerseCardsToFiles(context, verse.verseKey, arabic, translation, reference, theme).single()
                if (save) {
                    val location = saveVerseCardToGallery(context, file, file.name).getOrThrow()
                    Toast.makeText(context, "Saved to $location", Toast.LENGTH_LONG).show()
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        clipData = ClipData.newUri(context.contentResolver, "Ayah card", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Ayah").apply {
                        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } catch (exception: kotlinx.coroutines.CancellationException) {
                throw exception
            } catch (_: Exception) {
                error = if (save) "Couldn't save the image. Please try again." else "Couldn't share the image. Please try again."
            } finally { busy = false }
        }
    }
    val cream = Color(0xFFFAF7F1)
    val green = Color(0xFF123D32)
    val gold = Color(0xFFAA8032)
    LaunchedEffect(copied) { if (copied) { kotlinx.coroutines.delay(2000); copied = false } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val cardWidth = minOf(maxWidth, 560.dp) - 32.dp
            val cardHeight = preview?.let { cardWidth * (it.height.toFloat() / it.width) } ?: 280.dp
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val sheetHeight = minOf(maxHeight * .94f, cardHeight + 120.dp + bottomInset)
            Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = cream,
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().height(sheetHeight)) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(reference, color = green, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 2)
                        IconButton(onClick = onDismiss) { Icon(NurIcons.X, stringResource(R.string.ui_close_bbfa77), tint = green) }
                    }
                    // Controls stay visible while a long ayah scrolls below them.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ShareCardTheme.entries.forEach { option ->
                            val themeDescription = stringResource(R.string.ui_1_s_theme_b3f953, option.label)
                            IconButton(onClick = { theme = option }, enabled = !busy,
                                modifier = Modifier.semantics {
                                    contentDescription = themeDescription
                                    selected = theme == option
                                }) {
                                Surface(Modifier.size(26.dp), shape = androidx.compose.foundation.shape.CircleShape,
                                    color = Color(option.background), border = BorderStroke(if (theme == option) 2.dp else 1.dp,
                                        if (theme == option) gold else Color(0xFFCBC6BB))) {}
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Quran Nur", shareText))
                            copied = true
                        }) { Icon(if (copied) NurIcons.Check else NurIcons.Copy, if (copied) "Copied" else "Copy ayah text", tint = green) }
                        IconButton(onClick = { export(true) }, enabled = !busy && preview != null) {
                            Icon(NurIcons.Download, stringResource(R.string.ui_save_ayah_image_d6ab7d), tint = green)
                        }
                        IconButton(onClick = { export(false) }, enabled = !busy && preview != null) {
                            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = gold, strokeWidth = 2.dp)
                            else Icon(NurIcons.Share2, stringResource(R.string.ui_share_ayah_image_748994), tint = green)
                        }
                    }
                    if (error != null) Text(error!!, color = Color(0xFFA63232), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        val bitmap = preview
                        if (bitmap != null) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                                Image(bitmap.asImageBitmap(), "Complete ayah and translation card",
                                    modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height))
                            }
                        } else CircularProgressIndicator(Modifier.padding(top = 40.dp).size(28.dp), color = gold, strokeWidth = 2.dp)
                    }
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
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("MediaStore write failed")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (exception: Exception) {
                resolver.delete(uri, null, null)
                throw exception
            }
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
