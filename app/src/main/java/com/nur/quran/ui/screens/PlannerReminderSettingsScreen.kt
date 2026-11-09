package com.nur.quran.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.nur.quran.ui.components.planner.plannerAccentInk
import com.nur.quran.ui.components.planner.plannerPrayerName
import com.nur.quran.R
import com.nur.quran.data.reminders.*
import com.nur.quran.ui.components.NurIcons
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun PlannerReminderSettingsScreen(reminders: PlannerReminderCoordinator, onBack: () -> Unit) {
    val stored by reminders.settings.collectAsState()
    val saver = remember { Saver<ReminderSettings, String>(save = { com.google.gson.Gson().toJson(it) }, restore = { com.google.gson.Gson().fromJson(it, ReminderSettings::class.java) }) }
    var draft by rememberSaveable(stateSaver = saver) { mutableStateOf(if (stored.source == "calculated" && stored.zone != ZoneId.systemDefault().id) stored.copy(confirmed = false) else stored) }
    var latitude by rememberSaveable { mutableStateOf(stored.latitude?.toString().orEmpty()) }
    var longitude by rememberSaveable { mutableStateOf(stored.longitude?.toString().orEmpty()) }
    var error by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val zone = ZoneId.systemDefault()
    LaunchedEffect(zone.id) {
        if (draft.source == "calculated" && draft.zone != zone.id) draft = draft.copy(confirmed = false)
    }
    val prayers = reminders.activePrayers()
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = hGold, unfocusedBorderColor = hBoneDark,
        focusedTextColor = hInk, unfocusedTextColor = hInk, focusedLabelColor = plannerAccentInk, unfocusedLabelColor = hInkMid)
    val next by reminders.next.collectAsState()
    val notifications = remember(revision) { reminders.notificationsAllowed() }
    val exact = remember(revision) { reminders.exactAllowed() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { revision++; reminders.refresh(force = true) } }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    fun openSettings(intent: Intent) { runCatching { context.startActivity(intent) } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        revision++; reminders.refresh(force = true)
        if (!it) openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    fun readLocation() {
        val manager = context.getSystemService(LocationManager::class.java)
        val location = runCatching { manager.getProviders(true).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time in 0..86_400_000 }.maxByOrNull { it.time } }.getOrNull()
        if (location == null) error = R.string.reminder_location_missing
        else { latitude = location.latitude.toString(); longitude = location.longitude.toString(); draft = draft.copy(confirmed = false); error = null }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) readLocation() else error = R.string.reminder_location_missing
    }
    val candidate = draft.copy(latitude = latitude.toDoubleOrNull(), longitude = longitude.toDoubleOrNull(), zone = zone.id)
    val valid = !candidate.enabled || candidate.valid(prayers, zone)
    val preview by produceState<Map<String, Long>>(emptyMap(), candidate.source, candidate.latitude, candidate.longitude, candidate.method, candidate.hanafi, zone.id) {
        value = withContext(Dispatchers.Default) { if (candidate.source == "calculated" && candidate.latitude != null && candidate.longitude != null && candidate.latitude in -90.0..90.0 && candidate.longitude in -180.0..180.0)
            runCatching { PlannerReminderEngine.prayerTimes(candidate, LocalDate.now(zone)) }.getOrDefault(emptyMap()) else emptyMap() }
    }
    Surface(Modifier.fillMaxSize(), color = hCream) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !saving) { Icon(NurIcons.ArrowLeft, stringResource(R.string.ui_back_b52b36), tint = hInk) }
                Text(stringResource(R.string.reminder_title), color = hInk, fontFamily = fontFamilyUi, fontSize = 22.sp)
            }
            Divider(color = hBoneDark)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    Text(stringResource(R.string.reminder_intro), color = hInkMid, lineHeight = 24.sp)
                    ReminderToggle(stringResource(R.string.reminder_enable), draft.enabled) { draft = draft.copy(enabled = it) }
                    Text(stringResource(R.string.reminder_offline_help), color = hInkMid, fontSize = 13.sp, lineHeight = 20.sp)
                }
                item {
                    ReminderHeading(stringResource(R.string.reminder_schedule))
                    ReminderChoice(stringResource(R.string.reminder_custom), draft.source == "custom") { draft = draft.copy(source = "custom", confirmed = false) }
                    ReminderChoice(stringResource(R.string.reminder_calculated), draft.source == "calculated") { draft = draft.copy(source = "calculated", confirmed = false) }
                    Text(stringResource(R.string.reminder_zone, zone.id), color = hInkMid)
                }
                if (draft.source == "custom") item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prayers.forEach { prayer ->
                            OutlinedButton(onClick = {
                                val time = runCatching { LocalTime.parse(draft.times[prayer]) }.getOrDefault(LocalTime.now())
                                TimePickerDialog(context, { _, hour, minute ->
                                    draft = draft.copy(times = draft.times + (prayer to LocalTime.of(hour, minute).toString()), confirmed = false)
                                }, time.hour, time.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Icon(NurIcons.Clock, null, Modifier.size(18.dp), tint = plannerAccentInk)
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.reminder_finish_by, plannerPrayerName(prayer), draft.times[prayer] ?: stringResource(R.string.reminder_pick_time)), color = hInk)
                            }
                        }
                    }
                } else item {
                    Text(stringResource(R.string.reminder_location_help), color = hInkMid, lineHeight = 23.sp)
                    OutlinedTextField(latitude, { latitude = it; draft = draft.copy(confirmed = false) }, label = { Text(stringResource(R.string.reminder_latitude)) },
                        colors = fieldColors, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                    OutlinedTextField(longitude, { longitude = it; draft = draft.copy(confirmed = false) }, label = { Text(stringResource(R.string.reminder_longitude)) },
                        colors = fieldColors, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                    TextButton(onClick = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Text(stringResource(R.string.reminder_location), color = plannerAccentInk) }
                    var expanded by remember { mutableStateOf(false) }
                    Text(stringResource(R.string.reminder_method), color = hInk)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(methodName(draft.method), color = hInk) }
                        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(hCream)) {
                            com.batoulapps.adhan.CalculationMethod.values().filter { it.name != "OTHER" }.forEach { method ->
                                DropdownMenuItem(text = { Text(methodName(method.name), color = hInk) }, onClick = { draft = draft.copy(method = method.name, confirmed = false); expanded = false })
                            }
                        }
                    }
                    ReminderToggle(stringResource(R.string.reminder_hanafi), draft.hanafi) { draft = draft.copy(hanafi = it, confirmed = false) }
                    preview.forEach { (prayer, at) ->
                        Text("${plannerPrayerName(prayer)} · ${Instant.ofEpochMilli(at).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))}", color = hInkMid)
                    }
                    Text(stringResource(R.string.reminder_window, draft.windowMinutes), color = hInk, modifier = Modifier.padding(top = 12.dp))
                    Slider(draft.windowMinutes.toFloat(), { draft = draft.copy(windowMinutes = (it.toInt() / 5 * 5).coerceIn(10, 180), confirmed = false) }, valueRange = 10f..180f, steps = 33, colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold, inactiveTrackColor = hBoneDark))
                    Text(stringResource(R.string.reminder_window_help), color = hInkMid, fontSize = 13.sp)
                }
                item {
                    Row(Modifier.fillMaxWidth().clickable { draft = draft.copy(confirmed = !draft.confirmed) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(draft.confirmed, { draft = draft.copy(confirmed = it) }, colors = CheckboxDefaults.colors(checkedColor = hGold, uncheckedColor = hInkMid, checkmarkColor = hCream))
                        Text(stringResource(R.string.reminder_confirm), color = hInk, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    ReminderHeading(stringResource(R.string.reminder_intensity))
                    listOf("gentle" to R.string.reminder_gentle, "focused" to R.string.reminder_focused, "strong" to R.string.reminder_strong).forEach { (value, label) ->
                        ReminderChoice(stringResource(label), draft.intensity == value) { draft = draft.copy(intensity = value) }
                    }
                    Text(stringResource(when (draft.intensity) { "gentle" -> R.string.reminder_gentle_help; "strong" -> R.string.reminder_strong_help; else -> R.string.reminder_focused_help }), color = hInkMid, lineHeight = 23.sp)
                }
                item {
                    ReminderHeading(stringResource(R.string.reminder_permissions))
                    if (notifications) Text(stringResource(R.string.reminder_notifications_ready), color = hInkMid)
                    else OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text(stringResource(R.string.reminder_allow), color = plannerAccentInk) }
                    if (exact) Text(stringResource(R.string.reminder_exact_ready), color = hInkMid)
                    else {
                        Text(stringResource(R.string.reminder_approximate), color = hInkMid, lineHeight = 23.sp)
                        if (Build.VERSION.SDK_INT >= 31) TextButton(onClick = { openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) {
                            Text(stringResource(R.string.reminder_precision), color = plannerAccentInk)
                        }
                    }
                    TextButton(onClick = { openSettings(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, when (draft.intensity) { "gentle" -> PlannerReminderCoordinator.QUIET; "strong" -> PlannerReminderCoordinator.STRONG; else -> PlannerReminderCoordinator.FOCUSED })) }) {
                        Text(stringResource(R.string.reminder_sound_settings), color = plannerAccentInk)
                    }
                    next?.let { Text(stringResource(R.string.reminder_next, Instant.ofEpochMilli(it.at).atZone(zone).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))), color = hInkMid) }
                }
                item {
                    error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 10.dp)) }
                    if (draft.enabled && !valid) Text(stringResource(R.string.reminder_invalid), color = hInkMid, modifier = Modifier.padding(bottom = 10.dp))
                    Button(onClick = { saving = true; scope.launch {
                        try { reminders.save(candidate); onBack() }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { error = R.string.reminder_save_error }
                        finally { saving = false }
                    } }, enabled = valid && !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = hGold)) {
                        Text(stringResource(R.string.reminder_save), color = hCream)
                    }
                }
            }
        }
    }
}

private fun methodName(value: String): String = when (value) {
    "MUSLIM_WORLD_LEAGUE" -> "Muslim World League"
    "UMM_AL_QURA" -> "Umm al-Qura"
    "MOON_SIGHTING_COMMITTEE" -> "Moonsighting Committee"
    "NORTH_AMERICA" -> "ISNA · North America"
    else -> value.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

@Composable private fun ReminderHeading(text: String) { Text(text, color = hInk, fontFamily = fontFamilyUi, fontSize = 20.sp) }
@Composable private fun ReminderToggle(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = hInk, modifier = Modifier.weight(1f)); Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = hGold, checkedThumbColor = hCream, uncheckedTrackColor = hBoneDark))
    }
}
@Composable private fun ReminderChoice(text: String, checked: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onSelect).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(checked, onClick = onSelect, colors = RadioButtonDefaults.colors(selectedColor = hGold, unselectedColor = hInkMid)); Text(text, color = hInk, modifier = Modifier.weight(1f))
    }
}
