package com.nur.quran.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.mushaf.ReaderMushafPreference
import com.nur.quran.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Stable
internal class NativeAutoScroll(context: Context) {
    private val prefs = context.getSharedPreferences("NarrationReader", 0)
    var active by mutableStateOf(false)
    var paused by mutableStateOf(false)
    var speed by mutableIntStateOf(prefs.getInt("scroll_speed", 3).coerceIn(1, 7))
    fun changeSpeed(value: Int) { speed = value; prefs.edit().putInt("scroll_speed", value).apply() }
}

@Composable
internal fun rememberNativeAutoScroll(
    page: Int, canScroll: () -> Boolean, scroll: suspend (Float) -> Unit, next: () -> Boolean
): NativeAutoScroll {
    val context = LocalContext.current
    val state = remember { NativeAutoScroll(context) }
    val canScrollNow by rememberUpdatedState(canScroll)
    val scrollNow by rememberUpdatedState(scroll)
    val nextNow by rememberUpdatedState(next)
    LaunchedEffect(state.active, state.paused, state.speed, page) {
        if (!state.active || state.paused) return@LaunchedEffect
        val rate = floatArrayOf(12f, 24f, 45f, 90f, 150f, 270f, 450f)[state.speed - 1]
        var atEnd = 0L
        var moved = false
        while (true) {
            delay(32)
            if (canScrollNow()) {
                try { scrollNow(rate * .032f) }
                catch (cancel: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    state.paused = true // A manual drag takes ownership of scrolling.
                    return@LaunchedEffect
                }
                atEnd = 0L; moved = true
            }
            else {
                atEnd += 32
                // A page fitting the display still gets a full reading interval before advancing.
                val hold = if (moved) 2000L else (60_000L / state.speed).coerceAtLeast(8_000L)
                if (atEnd >= hold) { if (!nextNow()) state.active = false; break }
            }
        }
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeReaderHeader(
    title: String, auto: NativeAutoScroll, onBack: () -> Unit, onNavigate: () -> Unit,
    onReadingMode: () -> Unit, onSettings: () -> Unit
) {
    val context = LocalContext.current
    TopAppBar(
        title = { Row(Modifier.clickable(onClick = onNavigate), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = hInk, fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold,
                fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
            Icon(NurIcons.ChevronDown, stringResource(R.string.ui_select_surah_97d36a), tint = hInkMuted, modifier = Modifier.size(14.dp))
        } },
        navigationIcon = { IconButton(onClick = onBack) { Icon(NurIcons.ArrowLeft, stringResource(R.string.ui_back_b52b36), tint = hInk, modifier = Modifier.size(20.dp)) } },
        actions = {
            TopBarIconBtn(NurIcons.ChevronsDown, auto.active, stringResource(R.string.ui_auto_scroll_a03e7f)) { auto.active = !auto.active; auto.paused = false }
            TopBarIconBtn(NurIcons.BookOpen, false, stringResource(R.string.ui_reading_mode_d75871), onReadingMode)
            TopBarIconBtn(if (isDarkThemeGlobal) NurIcons.Sun else NurIcons.Moon, isDarkThemeGlobal,
                stringResource(R.string.ui_toggle_theme_9b0eaf)) {
                isDarkThemeGlobal = !isDarkThemeGlobal
                context.getSharedPreferences("Settings", 0).edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
            }
            TopBarIconBtn(NurIcons.Settings, false, stringResource(R.string.ui_settings_c7f73b), onSettings)
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = hCream)
    )
}

@Composable
internal fun NativeReaderSettings(
    size: Float, minimum: Float, maximum: Float, onSize: (Float) -> Unit,
    onJump: () -> Unit, onDismiss: () -> Unit, onSelectHafs: (String) -> Unit
) {
    val context = LocalContext.current
    var chooseMushaf by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = hCream,
        title = { Text(stringResource(if (chooseMushaf) R.string.settings_choose_mushaf else R.string.ui_settings_c7f73b), color = hInk, fontFamily = fontFamilyUi) },
        text = { Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
            if (chooseMushaf) MushafPicker(ReaderMushafPreference.selected(context), {
                if (!ReaderMushafPreference.isNative(it)) onSelectHafs(it)
                onDismiss()
            }, openOnSelect = false)
            else {
                Text(stringResource(R.string.ui_text_sizes_4dfc49), color = hInk)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onSize((size - (maximum - minimum) / 8).coerceAtLeast(minimum)) }) { Text("A−", color = hInk) }
                    Slider(value = size, onValueChange = onSize, valueRange = minimum..maximum, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onSize((size + (maximum - minimum) / 8).coerceAtMost(maximum)) }) { Text("A+", color = hInk) }
                }
                TextButton(onClick = { chooseMushaf = true }) { Text(stringResource(R.string.settings_choose_mushaf), color = hInk) }
                TextButton(onClick = onJump) { Text(stringResource(R.string.ui_go_to_page_af1382), color = hInk) }
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_done_e9b450), color = hInk) } }
    )
}

@Composable
internal fun NativeAutoScrollerBar(auto: NativeAutoScroll, up: () -> Unit, down: () -> Unit) {
    AutoScrollerBar(auto.active, auto.paused, auto.speed, { auto.paused = !auto.paused }, auto::changeSpeed,
        up, down, { auto.active = false; auto.paused = false })
}
