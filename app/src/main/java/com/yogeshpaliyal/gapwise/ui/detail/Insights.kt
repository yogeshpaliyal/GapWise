package com.yogeshpaliyal.gapwise.ui.detail

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yogeshpaliyal.gapwise.data.LogEntity
import com.yogeshpaliyal.gapwise.domain.Gap
import com.yogeshpaliyal.gapwise.domain.TimeText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Bars for the gap between each pair of logs; dashed line = your usual. One-offs are faded. */
@Composable
fun GapChart(gaps: List<Gap>, usual: Double?, color: Color, modifier: Modifier = Modifier) {
    val shown = gaps.takeLast(16)
    val max = (shown.maxOfOrNull { it.days } ?: 1.0).coerceAtLeast(usual ?: 0.0).coerceAtLeast(1.0)
    val muted = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / shown.size.coerceAtLeast(1)
            val barW = (slot * 0.6f).coerceAtMost(28.dp.toPx())
            shown.forEachIndexed { i, g ->
                val h = (g.days / max * size.height).toFloat().coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    color = if (g.outlier) muted else color,
                    topLeft = Offset(i * slot + (slot - barW) / 2, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
            if (usual != null) {
                val y = size.height - (usual / max * size.height).toFloat()
                drawLine(
                    color = line,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                shown.firstOrNull()?.let { TimeText.shortDate(it.endedAt) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                L10n.s(R.string.chart_max, max.roundToInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                shown.lastOrNull()?.let { TimeText.shortDate(it.endedAt) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Month grid with logged days (and multi-day spans) filled in. */
@Composable
fun LogCalendar(logs: List<LogEntity>, color: Color, modifier: Modifier = Modifier) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val marked: Set<LocalDate> = remember(logs) {
        buildSet {
            logs.forEach { log ->
                val start = TimeText.localDate(log.startAt)
                val end = log.endAt?.let { TimeText.localDate(it) } ?: start
                var d = start
                var guard = 0
                while (!d.isAfter(end) && guard++ < 120) {
                    add(d); d = d.plusDays(1)
                }
            }
        }
    }
    val today = LocalDate.now()
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, L10n.s(R.string.prev_month))
            }
            Text(
                java.time.format.DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMyyyy"), Locale.getDefault()).format(month),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, L10n.s(R.string.next_month))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.values().forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val lead = month.atDay(1).dayOfWeek.value - 1 // Monday first
        val days = month.lengthOfMonth()
        val cells = lead + days
        val rows = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - lead + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..days) {
                                val date = month.atDay(dayNum)
                                val on = date in marked
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (on) color else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$dayNum",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                        color = if (on) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
