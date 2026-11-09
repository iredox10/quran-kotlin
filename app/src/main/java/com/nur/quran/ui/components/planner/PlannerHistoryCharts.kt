package com.nur.quran.ui.components.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*
import java.time.LocalDate
import java.text.SimpleDateFormat

@Composable
internal fun PlannerHistoryTimeChart(summary:PlannerHistorySummary) {
    val points=remember(summary){PlannerHistory.chartPoints(summary)}
    if(points.isEmpty()) return
    var cumulative by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(points.lastIndex) }
    val current=points[selected.coerceIn(points.indices)]
    val accent=plannerAccentInk
    val track=hBoneDark
    val rtl=LocalLayoutDirection.current==LayoutDirection.Rtl
    val caption=stringResource(R.string.planner_history_dates_only)
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        HistoryHeading(NurIcons.BarChart3,stringResource(R.string.planner_reading_time))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            HistoryFilter(stringResource(R.string.planner_history_daily),!cumulative){cumulative=false}
            HistoryFilter(stringResource(R.string.planner_history_cumulative),cumulative){cumulative=true}
        }
        Text(caption,fontSize=12.sp,color=hInkMid)
        if(cumulative) {
            val maximum=points.last().cumulativeSeconds.coerceAtLeast(1)
            Text(plannerDuration(maximum),fontFamily=fontFamilyUi,fontSize=24.sp,color=hInk)
            Canvas(Modifier.fillMaxWidth().height(145.dp).semantics{contentDescription=caption}) {
                val padding=8.dp.toPx()
                val w=(size.width-2*padding).coerceAtLeast(1f)
                val h=(size.height-2*padding).coerceAtLeast(1f)
                repeat(3){i->val y=padding+h*i/2;drawLine(track,Offset(padding,y),Offset(size.width-padding,y),1.dp.toPx())}
                val first=LocalDate.parse(points.first().date).toEpochDay()
                val span=(LocalDate.parse(points.last().date).toEpochDay()-first).coerceAtLeast(1)
                val coords=points.map { point ->
                    val fraction=(LocalDate.parse(point.date).toEpochDay()-first).toFloat()/span
                    Offset(padding+w*(if(rtl) 1f-fraction else fraction),padding+h*(1f-point.cumulativeSeconds.toFloat()/maximum))
                }
                val path=Path()
                coords.forEachIndexed { i,p -> if(i==0) path.moveTo(p.x,p.y) else {path.lineTo(p.x,coords[i-1].y);path.lineTo(p.x,p.y)} }
                drawPath(path,accent,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round))
                coords.forEach { drawCircle(accent,3.dp.toPx(),it) }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(historyShortDate(points.first().date),color=hInkMid,fontSize=12.sp)
                Text(historyShortDate(points.last().date),color=hInkMid,fontSize=12.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(plannerDateLabel(current.date),color=hInk,fontSize=14.sp)
                Text(plannerDuration(current.seconds),color=plannerAccentInk,fontFamily=fontFamilyMono,fontSize=14.sp)
            }
            val maximum=points.maxOf{it.seconds}.coerceAtLeast(1)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val barWidth = if (points.size <= 6)
                    ((maxWidth - 8.dp * (points.size - 1)) / points.size.toFloat()).coerceAtLeast(48.dp)
                    else 48.dp
            LazyRow(state=rememberLazyListState(initialFirstVisibleItemIndex=(points.size-4).coerceAtLeast(0)),horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                itemsIndexed(points,key={_,p->p.date}) { i,point ->
                    val description=plannerDateLabel(point.date)+" · "+plannerDuration(point.seconds)
                    Column(Modifier.width(barWidth).clickable{selected=i}.semantics{contentDescription=description},horizontalAlignment=Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(116.dp),contentAlignment=Alignment.BottomCenter) {
                            Box(Modifier.width(18.dp).height((112f*point.seconds.toFloat()/maximum).coerceAtLeast(2f).dp)
                                .clip(RoundedCornerShape(topStart=4.dp,topEnd=4.dp)).background(if(i==selected) accent else accent.copy(alpha=.75f)))
                        }
                        Text(historyShortDate(point.date),fontSize=12.sp,color=if(i==selected) plannerAccentInk else hInkMid,modifier=Modifier.padding(top=10.dp,bottom=6.dp),maxLines=2,lineHeight=16.sp)
                    }
                }
            }
            }
        }
    }
}

@Composable private fun historyShortDate(date:String):String {
    val locale=LocalConfiguration.current.locales[0]
    return remember(date,locale){runCatching{SimpleDateFormat("MMM d",locale).format(PlannerEngine.parsePlannerDate(date))}.getOrDefault(date)}
}
