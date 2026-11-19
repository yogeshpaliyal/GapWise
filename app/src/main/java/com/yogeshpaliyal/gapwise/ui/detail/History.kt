package com.yogeshpaliyal.gapwise.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogeshpaliyal.gapwise.data.LogEntity
import com.yogeshpaliyal.gapwise.domain.DAY_MS
import com.yogeshpaliyal.gapwise.domain.Gap
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.theme.CountStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_COL = 64.dp
private val RAIL_COL = 28.dp

/**
 * History as a timeline, newest first. The space between two entries is the
 * gap itself: taller for longer gaps, labelled ("36 days later"), and dashed
 * when it was a one-off that doesn't count toward your rhythm.
 */
fun LazyListScope.historyTimeline(
    logs: List<LogEntity>,
    gaps: List<Gap>,
    multiDay: Boolean,
    color: Color,
    usualDays: Double?,
    onClick: (LogEntity) -> Unit,
) {
    val newestFirst = logs.sortedByDescending { it.startAt }
    val gapByEnd = gaps.associateBy { it.endedAt }
    var lastYear = TimeText.localDate(System.currentTimeMillis()).year

    newestFirst.forEachIndexed { i, log ->
        val year = TimeText.localDate(log.startAt).year
        if (year != lastYear) {
            val y = year
            item(key = "year-$y-${log.id}") { YearMarker(y, color) }
            lastYear = year
        }
        item(key = "log-${log.id}") {
            LogNode(
                log = log,
                multiDay = multiDay,
                color = color,
                isFirst = i == 0,
                isLast = i == newestFirst.lastIndex,
                onClick = { onClick(log) },
            )
        }
        val older = newestFirst.getOrNull(i + 1)
        if (older != null) {
            val gap = gapByEnd[log.startAt]
            val days = gap?.days ?: ((log.startAt - older.startAt).toDouble() / DAY_MS)
            item(key = "gap-${log.id}") {
                GapConnector(days = days, outlier = gap?.outlier == true, usualDays = usualDays, color = color)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogNode(
    log: LogEntity,
    multiDay: Boolean,
    color: Color,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val date = TimeText.localDate(log.startAt)
    val rail = color.copy(alpha = 0.35f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = "Edit this log", onClick = onClick),
    ) {
        // Date block: the day number is the anchor you scan for.
        Column(Modifier.width(DATE_COL).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${date.dayOfMonth}", style = CountStyle.copy(fontSize = 30.sp, lineHeight = 30.sp))
            Text(
                date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Rail with the node. The newest entry gets the "fresh" circle shape.
        Box(Modifier.width(RAIL_COL).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxHeight().width(RAIL_COL)) {
                val x = size.width / 2
                val mid = size.height / 2
                val stroke = 3.dp.toPx()
                if (!isFirst) drawLine(rail, Offset(x, 0f), Offset(x, mid), stroke, StrokeCap.Round)
                if (!isLast) drawLine(rail, Offset(x, mid), Offset(x, size.height), stroke, StrokeCap.Round)
            }
            val nodeShape = if (isFirst) MaterialShapes.Cookie9Sided.toShape() else CircleShape
            Surface(shape = nodeShape, color = color, modifier = Modifier.width(if (isFirst) 20.dp else 12.dp).height(if (isFirst) 20.dp else 12.dp)) {}
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier.weight(1f).padding(vertical = 12.dp).padding(end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text("$weekday, ${TimeText.time(log.startAt)}", style = MaterialTheme.typography.titleMedium)
            if (multiDay) {
                val end = log.endAt
                val span = if (end == null) {
                    "Still going"
                } else {
                    val d = TimeText.daysBetween(log.startAt, end) + 1
                    "Lasted $d day${if (d == 1L) "" else "s"}, until ${end.let { TimeText.shortDate(it) }}"
                }
                Text(span, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            log.note?.let { note ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun GapConnector(days: Double, outlier: Boolean, usualDays: Double?, color: Color) {
    // Height grows with the gap so long stretches feel long, within limits.
    val height: Dp = (20 + days.coerceAtMost(120.0) * 0.8).dp.coerceAtMost(116.dp)
    val rail = if (outlier) MaterialTheme.colorScheme.outlineVariant else color.copy(alpha = 0.35f)
    val d = days.roundToInt()
    val label = when {
        days < 1 -> "Same day"
        d == 1 -> "1 day later"
        else -> "$d days later"
    }
    val vsUsual = usualDays?.takeIf { !outlier && it >= 1 }?.let { u ->
        val diff = (days - u).roundToInt()
        when {
            kotlin.math.abs(diff) <= (u * 0.1).coerceAtLeast(1.0) -> "right on your usual"
            diff > 0 -> "$diff longer than usual"
            else -> "${-diff} sooner than usual"
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = height), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(DATE_COL))
        Canvas(Modifier.width(RAIL_COL).height(height)) {
            val x = size.width / 2
            drawLine(
                color = rail,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (outlier) PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = if (outlier) MaterialTheme.colorScheme.surfaceContainerHigh else color.copy(alpha = 0.16f),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                val extra = if (outlier) "one-off, not counted" else vsUsual
                if (extra != null) {
                    Text(
                        "  $extra",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun YearMarker(year: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$year",
            style = MaterialTheme.typography.titleLarge,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(DATE_COL),
        )
    }
}
