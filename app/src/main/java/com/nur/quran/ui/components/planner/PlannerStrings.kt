package com.nur.quran.ui.components.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.nur.quran.R
import com.nur.quran.data.planner.PlanTemplate
import com.nur.quran.data.planner.PlannerEngine
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

/** Only presentation labels are localized; persisted planner/unit/prayer identifiers stay stable. */
@Composable
internal fun plannerUnitName(unit: String?, plural: Boolean = false): String = stringResource(when (unit) {
    "page" -> if (plural) R.string.planner_pages else R.string.planner_page
    "surah" -> if (plural) R.string.planner_surahs else R.string.planner_surah
    "juz" -> if (plural) R.string.planner_ajza else R.string.planner_juz
    "hizb" -> if (plural) R.string.planner_ahzab else R.string.planner_hizb
    else -> R.string.planner_units
})

@Composable
internal fun plannerPrayerName(name: String): String = when (name) {
    "Fajr" -> stringResource(R.string.planner_fajr)
    "Dhuhr" -> stringResource(R.string.planner_dhuhr)
    "Asr" -> stringResource(R.string.planner_asr)
    "Maghrib" -> stringResource(R.string.planner_maghrib)
    "Isha" -> stringResource(R.string.planner_isha)
    else -> name
}

@Composable
internal fun plannerPresetTitle(pace: PresetPaceItem): String = stringResource(when (pace.id) {
    "ramadan" -> R.string.planner_ramadan_pace
    "steady" -> R.string.planner_steady_journey
    else -> R.string.planner_devotional_path
})

@Composable
internal fun plannerTemplateTitle(template: PlanTemplate): String = when (template.id) {
    "ramadan-last-10" -> stringResource(R.string.planner_template_ramadan)
    "juz-amma" -> stringResource(R.string.planner_template_amma)
    "al-kahf" -> stringResource(R.string.planner_template_kahf)
    "tafsir-deep-dive" -> stringResource(R.string.planner_template_tafsir)
    "daily-juz" -> stringResource(R.string.planner_template_daily_juz)
    "quick-revision" -> stringResource(R.string.planner_template_revision)
    else -> template.title
}

@Composable
internal fun plannerTemplateDescription(template: PlanTemplate): String = when (template.id) {
    "ramadan-last-10" -> stringResource(R.string.planner_template_ramadan_description)
    "juz-amma" -> stringResource(R.string.planner_template_amma_description)
    "al-kahf" -> stringResource(R.string.planner_template_kahf_description)
    "tafsir-deep-dive" -> stringResource(R.string.planner_template_tafsir_description)
    "daily-juz" -> stringResource(R.string.planner_template_daily_juz_description)
    "quick-revision" -> stringResource(R.string.planner_template_revision_description)
    else -> template.description
}

@Composable
internal fun plannerTemplateTag(template: PlanTemplate): String = when (template.id) {
    "ramadan-last-10" -> stringResource(R.string.planner_tag_ramadan)
    "juz-amma" -> stringResource(R.string.planner_tag_juz_30)
    "al-kahf" -> stringResource(R.string.planner_tag_friday)
    "tafsir-deep-dive" -> stringResource(R.string.planner_tag_study)
    "daily-juz" -> stringResource(R.string.planner_juz)
    "quick-revision" -> stringResource(R.string.planner_tag_review)
    else -> template.tags.firstOrNull().orEmpty()
}

@Composable
internal fun plannerDateLabel(date: String): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(date, locale) {
        runCatching { SimpleDateFormat("EEE, MMM d, yyyy", locale).format(PlannerEngine.parsePlannerDate(date)) }.getOrDefault(date)
    }
}

@Composable
internal fun plannerWeekdays(): List<String> {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(locale) { DateFormatSymbols(locale).shortWeekdays.drop(1) }
}

@Composable
internal fun plannerDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return when {
        s >= 3600 -> stringResource(R.string.planner_hours_minutes, s / 3600, (s % 3600) / 60)
        s >= 60 -> stringResource(R.string.planner_minutes_seconds, s / 60, s % 60)
        else -> stringResource(R.string.planner_seconds, s)
    }
}

@Composable
internal fun plannerAssignmentTitle(assignment: com.nur.quran.data.planner.PlannerAssignment): String {
    val start = assignment.startUnit
    val end = assignment.endUnit
    if (assignment.unitType == "surah" && start == end) {
        return stringResource(R.string.planner_surah_number, plannerChapterName(start))
    }
    return if (start == end) stringResource(R.string.planner_unit_number, plannerUnitName(assignment.unitType), start)
    else stringResource(R.string.planner_unit_range_numbers, plannerUnitName(assignment.unitType, true), start, end)
}

@Composable
internal fun plannerAssignmentSubtitle(assignment: com.nur.quran.data.planner.PlannerAssignment): String {
    val pageCount = assignment.items.sumOf { (it.pageEnd - it.pageStart + 1).coerceAtLeast(0) }
    return stringResource(R.string.planner_page_count, pageCount)
}

@Composable
internal fun plannerItemTitle(item: com.nur.quran.data.planner.PlannerItem, unit: String): String {
    val start = if (unit == "page") item.pageStart else item.rangeValue.substringBefore('-').toIntOrNull() ?: item.id
    val end = if (unit == "page") item.pageEnd else item.rangeValue.substringAfter('-', item.rangeValue).toIntOrNull() ?: start
    return if (start == end) stringResource(R.string.planner_unit_number, plannerUnitName(unit), start)
    else stringResource(R.string.planner_unit_range_numbers, plannerUnitName(unit, true), start, end)
}

@Composable
internal fun plannerChapterName(chapterId: Int): String {
    val chapter = com.nur.quran.data.mushaf.ChapterMetadata.findById(chapterId) ?: return chapterId.toString()
    return if (LocalConfiguration.current.locales[0]?.language == "ar") chapter.nameArabic else chapter.nameSimple
}
