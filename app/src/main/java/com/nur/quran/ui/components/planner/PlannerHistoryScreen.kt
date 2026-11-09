package com.nur.quran.ui.components.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.TajweedAndroidText
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.theme.ReaderSystemBars

/** A full navigation screen. It owns no dialog, overlay, or enclosing card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerHistoryScreen(
    plans: List<ReadingPlan>,
    timersByPlan: Map<String, Map<Int, PlannerSessionTimer>>,
    reflectionsByPlan: Map<String, Map<Int, String>>,
    bookmarksByPlan: Map<String, List<PlannerBookmark>>,
    onBack: () -> Unit,
    onOpenPlan: (String) -> Unit,
    selectedPlanId: String? = null,
    loadVerses: suspend (List<String>) -> Map<String, VerseEntity> = { emptyMap() },
    onOpenVerse: (String) -> Unit = {}
) {
    ReaderSystemBars()
    val selected = plans.find { it.id == selectedPlanId }
    val summaries = remember(plans, timersByPlan) { plans.associate { it.id to PlannerHistory.summarize(it,timersByPlan[it.id].orEmpty()) } }
    var completedOnly by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable(selectedPlanId) { mutableIntStateOf(0) }
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    Surface(Modifier.fillMaxSize(), color=hCream) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack) { Icon(NurIcons.ArrowLeft,stringResource(if(selectedPlanId==null) R.string.ui_back_b52b36 else R.string.planner_back_history),tint=hInk) }
                Text(stringResource(if(selectedPlanId==null) R.string.planner_planner_history else R.string.planner_plan_details),fontFamily=fontFamilyUi,fontSize=20.sp,color=hInk,modifier=Modifier.weight(1f))
                Icon(if(selectedPlanId==null) NurIcons.Archive else NurIcons.BookOpen,null,tint=plannerAccentInk,modifier=Modifier.padding(end=16.dp).size(20.dp))
            }
            Divider(color=hBoneDark)
            if (selected!=null) {
                val notes = remember(selected,reflectionsByPlan) { PlannerHistory.reflections(selected,reflectionsByPlan[selected.id].orEmpty()) }
                val highlights = remember(bookmarksByPlan,selected.id) { PlannerHistory.highlights(bookmarksByPlan[selected.id].orEmpty()) }
                Column(Modifier.padding(horizontal=22.dp,vertical=18.dp)) {
                    HistoryStatus(summaries.getValue(selected.id).completed)
                    Text(selected.title,fontFamily=fontFamilyUi,fontSize=28.sp,lineHeight=32.sp,color=hInk,modifier=Modifier.padding(top=8.dp))
                    Text(stringResource(R.string.planner_started_date,plannerDateLabel(selected.startDate)),color=hInkMid,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
                    summaries.getValue(selected.id).completedOn?.let { Text(stringResource(R.string.planner_completed_date,plannerDateLabel(it)),color=hInkMid,fontSize=12.sp) }
                }
                TabRow(selectedTabIndex=tab,containerColor=hCream,contentColor=plannerAccentInk,
                    indicator={ positions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(positions[tab]),color=hGold) }) {
                    listOf(Triple(NurIcons.BarChart3,R.string.planner_history_overview,null),
                        Triple(NurIcons.Bookmark,R.string.planner_history_highlights,highlights.size),
                        Triple(NurIcons.Lightbulb,R.string.planner_history_reflections,notes.size)).forEachIndexed { i,(icon,label,count) ->
                        val tint=if(tab==i) plannerAccentInk else hInkMid
                        Column(Modifier.fillMaxWidth().heightIn(min=56.dp)
                            .selectable(selected=tab==i,role=Role.Tab,onClick={tab=i})
                            .padding(horizontal=4.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                Icon(icon,null,Modifier.size(16.dp),tint=tint)
                                if(count!=null) Text(count.toString(),fontFamily=fontFamilyMono,fontSize=12.sp,color=tint)
                            }
                            Text(stringResource(label),fontSize=12.sp,lineHeight=16.sp,maxLines=2,color=tint,modifier=Modifier.padding(top=4.dp))
                        }
                    }
                }
                key(selected.id,tab) {
                    when(tab) {
                        0 -> HistoryOverview(selected,summaries.getValue(selected.id))
                        1 -> HistoryHighlights(highlights,loadVerses,onOpenVerse)
                        else -> HistoryReflections(selected,notes,timersByPlan[selected.id].orEmpty())
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f),state=scrollState,contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    item {
                        Text(stringResource(R.string.planner_reading_journey),fontFamily=fontFamilyUi,fontSize=28.sp,color=hInk)
                        Text(stringResource(R.string.planner_history_totals,summaries.values.count{it.completed},plannerDuration(summaries.values.sumOf{it.totalSeconds})),fontSize=14.sp,lineHeight=21.sp,color=hInkMid,modifier=Modifier.padding(top=8.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=16.dp)) {
                            HistoryFilter(stringResource(R.string.planner_all_past_plans),!completedOnly){completedOnly=false}
                            HistoryFilter(stringResource(R.string.planner_completed),completedOnly){completedOnly=true}
                        }
                    }
                    val visible=plans.filter{!completedOnly||summaries[it.id]?.completed==true}
                    if(visible.isEmpty()) item { HistoryEmpty(NurIcons.BookOpen,R.string.planner_no_completed,R.string.planner_history_empty) }
                    items(visible,key={it.id}) { plan ->
                        val summary=summaries.getValue(plan.id)
                        Column(Modifier.fillMaxWidth().clickable{onOpenPlan(plan.id)}.padding(vertical=10.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                HistoryProgressRing(summary)
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    HistoryStatus(summary.completed)
                                    Text(plan.title,fontFamily=fontFamilyUi,fontSize=20.sp,lineHeight=27.sp,color=hInk)
                                    val endedOn=summary.completedOn ?: plan.archivedAt?.take(10)
                                    Text(stringResource(if(endedOn!=null) R.string.planner_finished_date else R.string.planner_started_date,plannerDateLabel(endedOn ?: plan.startDate)),color=hInkMid,fontSize=12.sp)
                                    Text(stringResource(R.string.planner_history_progress,summary.completedDays,summary.totalDays,summary.pagesRead,summary.totalPages),color=hInkMid,fontSize=14.sp)
                                }
                                Icon(NurIcons.ChevronRight,stringResource(R.string.planner_view_history),tint=hInkMid,modifier=Modifier.size(20.dp))
                            }
                            Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                                HistoryInline(NurIcons.Clock,plannerDuration(summary.totalSeconds))
                                HistoryInline(NurIcons.Bookmark,PlannerHistory.highlights(bookmarksByPlan[plan.id].orEmpty()).size.toString())
                                HistoryInline(NurIcons.Lightbulb,PlannerHistory.reflections(plan,reflectionsByPlan[plan.id].orEmpty()).size.toString())
                            }
                            Divider(color=hBoneDark,modifier=Modifier.padding(top=20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun HistoryOverview(plan:ReadingPlan,summary:PlannerHistorySummary) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item {
            Text(plannerDuration(summary.totalSeconds),fontFamily=fontFamilyUi,fontSize=36.sp,color=hInk)
            Text(stringResource(R.string.planner_total_time),fontSize=14.sp,color=hInkMid,modifier=Modifier.padding(top=4.dp))
            Row(Modifier.fillMaxWidth().padding(top=22.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f)){HistoryInline(NurIcons.BookOpen,"${summary.pagesRead}/${summary.totalPages}");Text(stringResource(R.string.planner_pages),fontSize=12.sp,color=hInkMid)}
                Column(Modifier.weight(1f)){HistoryInline(NurIcons.CalendarDays,"${summary.completedDays}/${summary.totalDays}");Text(stringResource(R.string.planner_duration_days),fontSize=12.sp,color=hInkMid)}
                Column(Modifier.weight(1f)){HistoryInline(NurIcons.Clock,if(summary.timedDays>0) plannerDuration(summary.averageSeconds) else "—");Text(stringResource(R.string.planner_history_daily),fontSize=12.sp,color=hInkMid)}
            }
        }
        item { PlannerHistoryTimeChart(summary) }
        item { HistoryHeading(NurIcons.Clock,stringResource(R.string.planner_reading_time)) }
        if(summary.readingDays.isEmpty()) item { Text(stringResource(R.string.planner_no_plan_time),fontSize=16.sp,lineHeight=24.sp,color=hInkMid) }
        items(summary.readingDays,key={it.date?:"legacy-${it.dayNumber}"}) { day ->
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(day.date?.let{plannerDateLabel(it)}?:stringResource(R.string.planner_earlier_reading,day.dayNumber?:0),fontSize=14.sp,color=hInk,modifier=Modifier.weight(1f))
                    Text(plannerDuration(day.seconds),fontSize=14.sp,fontFamily=fontFamilyMono,color=plannerAccentInk)
                }
                Text(stringResource(R.string.planner_accumulated,plannerDuration(day.cumulativeSeconds)) + if(day.date==null) stringResource(R.string.planner_date_not_recorded) else "",fontSize=12.sp,color=hInkMid)
                Divider(color=hBoneDark,modifier=Modifier.padding(top=8.dp))
            }
        }
        item { HistoryHeading(NurIcons.CalendarDays,stringResource(R.string.planner_timeline)) }
        items(plan.assignments,key={it.dayNumber}) { day ->
            val progress=PlannerEngine.getAssignmentProgress(plan,day)
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.planner_history_day_title,day.dayNumber,plannerAssignmentTitle(day)),fontFamily=fontFamilyUi,fontSize=20.sp,color=hInk)
                Text(stringResource(R.string.planner_day_read_status,progress.readPagesCount,progress.totalPagesCount,stringResource(if(progress.isComplete) R.string.planner_completed else if(progress.readPagesCount>0) R.string.planner_partially_read else R.string.planner_not_read)),fontSize=14.sp,color=plannerAccentInk)
                LinearProgressIndicator(progress=if(progress.totalPagesCount>0) (progress.readPagesCount.toFloat()/progress.totalPagesCount).coerceIn(0f,1f) else 0f,modifier=Modifier.fillMaxWidth().height(3.dp),color=hGold,trackColor=hBoneDark)
                Text(stringResource(R.string.planner_scheduled_date,plannerDateLabel(day.date)),fontSize=12.sp,color=hInkMid)
                progress.completedAt?.let{Text(stringResource(R.string.planner_finished_date,plannerDateLabel(it)),fontSize=12.sp,color=hInkMid)}
            }
        }
    }
}

@Composable private fun HistoryHighlights(highlights:List<PlannerBookmark>,loadVerses:suspend(List<String>)->Map<String,VerseEntity>,onOpenVerse:(String)->Unit) {
    val keys=remember(highlights){highlights.map{it.verseKey}}
    val verses by produceState<Map<String,VerseEntity>?>(null,keys){value=runCatching{loadVerses(keys)}.getOrDefault(emptyMap())}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        if(highlights.isEmpty()) item { HistoryEmpty(NurIcons.Bookmark,R.string.planner_plan_highlights,R.string.planner_no_highlights) }
        items(highlights,key={it.verseKey}) { highlight ->
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                HistoryHeading(NurIcons.Bookmark,listOf(highlight.surahName.takeIf{it.isNotBlank()},highlight.verseKey).filterNotNull().joinToString(" · "))
                val verse=verses?.get(highlight.verseKey)
                if(verse!=null) {
                    val font=if(!verse.textQpcHafs.isNullOrBlank() && verse.textQpcHafs!=verse.textUthmani) "KFGQPC Hafs" else "Amiri Quran"
                    TajweedAndroidText(remember(verse,font){mushafPlainVerseText(verse,"madani-standard",font)},emptyList(),emptyList(),font,28f,1.8f,
                        textColor=hInk,isInteractive=false,onWordClick={},onTajweedClick={},modifier=Modifier.fillMaxWidth())
                    val translation=remember(verse.translation){com.nur.quran.ui.components.hifdhTranslationText(verse.translation)}
                    if(translation.isNotBlank()) Text(translation,fontFamily=fontFamilyBody,fontSize=16.sp,lineHeight=25.sp,color=hInkMid)
                } else if(verses==null) LinearProgressIndicator(color=hGold,trackColor=hBoneDark,modifier=Modifier.fillMaxWidth())
                else Text(stringResource(R.string.planner_history_verse_unavailable),fontSize=14.sp,color=hInkMid)
                if(highlight.note.isNotBlank()) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.width(2.dp).height(26.dp).background(hGold))
                        Text(highlight.note,fontFamily=fontFamilyBody,fontSize=16.sp,lineHeight=26.sp,color=hInk)
                    }
                }
                if(highlight.createdAt>0) Text(plannerDateLabel(PlannerEngine.formatPlannerDate(java.util.Date(highlight.createdAt))),fontSize=12.sp,color=hInkMid)
                TextButton(onClick={onOpenVerse(highlight.verseKey)},colors=ButtonDefaults.textButtonColors(contentColor=plannerAccentInk)) {
                    Text(stringResource(R.string.ui_go_to_ayah_ee520e)); Spacer(Modifier.width(8.dp)); Icon(NurIcons.ArrowRight,null,Modifier.size(17.dp))
                }
                Divider(color=hBoneDark)
            }
        }
    }
}

@Composable private fun HistoryReflections(plan:ReadingPlan,notes:Map<Int,String>,timers:Map<Int,PlannerSessionTimer>) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(28.dp)) {
        if(notes.isEmpty()) item { HistoryEmpty(NurIcons.Lightbulb,R.string.planner_daily_reflections,R.string.planner_no_reflections) }
        items(notes.entries.toList(),key={it.key}) { (day,note) ->
            val assignment=plan.assignments.find{it.dayNumber==day}
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                HistoryHeading(NurIcons.Lightbulb,stringResource(R.string.planner_day_upper,day))
                assignment?.let {
                    Text(plannerAssignmentTitle(it),fontFamily=fontFamilyUi,fontSize=20.sp,color=hInk)
                    Text(stringResource(R.string.planner_scheduled_date,plannerDateLabel(it.date)),fontSize=12.sp,color=hInkMid)
                }
                timers[day]?.takeIf{it.totalSeconds>0}?.let { HistoryInline(NurIcons.Clock,plannerDuration(it.totalSeconds)) }
                Text(note,fontFamily=fontFamilyBody,fontSize=16.sp,lineHeight=27.sp,color=hInk)
                Divider(color=hBoneDark,modifier=Modifier.padding(top=8.dp))
            }
        }
    }
}

@Composable internal fun HistoryHeading(icon:ImageVector,text:String) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    Icon(icon,null,Modifier.size(20.dp),tint=plannerAccentInk)
    Text(text,fontFamily=fontFamilyUi,fontSize=20.sp,color=hInk)
} }
@Composable internal fun HistoryInline(icon:ImageVector,text:String) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    Icon(icon,null,Modifier.size(16.dp),tint=plannerAccentInk); Text(text,fontSize=14.sp,color=hInk)
} }
@Composable private fun HistoryStatus(complete:Boolean) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
    Icon(if(complete) NurIcons.CheckCircle2 else NurIcons.Archive,null,Modifier.size(15.dp),tint=plannerAccentInk)
    Text(stringResource(if(complete) R.string.planner_completed_upper else R.string.planner_ended_early),fontSize=12.sp,fontWeight=FontWeight.Medium,color=plannerAccentInk,letterSpacing=.6.sp)
} }
@Composable private fun HistoryProgressRing(summary:PlannerHistorySummary) {
    val accent=hGold;val track=hBoneDark
    Box(Modifier.size(44.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(track,-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(),cap=StrokeCap.Round))
            if(summary.totalPages>0) drawArc(accent,-90f,360f*(summary.pagesRead.toFloat()/summary.totalPages).coerceIn(0f,1f),false,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(),cap=StrokeCap.Round))
        }
        Icon(if(summary.completed) NurIcons.Check else NurIcons.BookOpen,null,Modifier.size(18.dp),tint=plannerAccentInk)
    }
}
@Composable private fun HistoryEmpty(icon:ImageVector,title:Int,body:Int) { Column(Modifier.fillMaxWidth().padding(vertical=32.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Icon(icon,null,Modifier.size(32.dp),tint=plannerAccentInk)
    Text(stringResource(title),fontFamily=fontFamilyUi,fontSize=24.sp,color=hInk)
    Text(stringResource(body),fontSize=16.sp,lineHeight=25.sp,color=hInkMid)
} }
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HistoryFilter(label:String,selected:Boolean,onClick:()->Unit) {
    FilterChip(selected,onClick,label={Text(label,fontSize=14.sp)},colors=FilterChipDefaults.filterChipColors(containerColor=hCream,labelColor=hInkMid,selectedContainerColor=hGoldSoft,selectedLabelColor=plannerAccentInk),
        border=FilterChipDefaults.filterChipBorder(borderColor=hBoneDark,selectedBorderColor=hGold))
}
