package com.yogeshpaliyal.gapwise.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogeshpaliyal.gapwise.domain.TimeText

fun eventColor(argb: Long) = Color(argb.toInt())

private fun EventUi.statusLine(now: Long): String = when {
    event.paused -> "Paused"
    ongoing -> "Ongoing · day ${TimeText.daysBetween(rhythm.ongoingSince!!, now) + 1}"
    rhythm.hasRhythm -> TimeText.dueText(rhythm.nextExpectedAt, now) ?: ""
    rhythm.logCount == 0 -> "Tap to log the first time"
    else -> "Learning your rhythm…"
}

private fun EventUi.elapsed(now: Long): Pair<String, String> =
    if (ongoing) TimeText.elapsed(rhythm.ongoingSince, now).let { it.first to "since start" }
    else TimeText.elapsed(rhythm.lastAt, now)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventTile(ui: EventUi, now: Long, onLog: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val color = eventColor(ui.event.color)
    val (num, unit) = ui.elapsed(now)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLog()
                },
                onLongClick = onOpen,
            ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(ui.event.emoji, color)
                Spacer(Modifier.weight(1f))
                if (ui.event.pinned) Icon(Icons.Filled.PushPin, "Pinned", Modifier.size(16.dp), tint = color)
                if (ui.event.paused) Icon(Icons.Outlined.PauseCircle, "Paused", Modifier.size(16.dp))
                IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Info, "Details", Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                ui.event.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(num, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(
                ui.statusLine(now),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            RhythmBar(ui, now, color)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventRow(ui: EventUi, now: Long, onLog: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val color = eventColor(ui.event.color)
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLog()
                },
                onLongClick = onOpen,
            ),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(ui.event.emoji, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ui.event.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    ui.statusLine(now),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                RhythmBar(ui, now, color)
            }
            Spacer(Modifier.width(12.dp))
            val (num, unit) = ui.elapsed(now)
            Column(horizontalAlignment = Alignment.End) {
                Text(num, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(unit, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onOpen) { Icon(Icons.Outlined.Info, "Details") }
        }
    }
}

@Composable
private fun RhythmBar(ui: EventUi, now: Long, color: Color) {
    if (!ui.rhythm.hasRhythm || ui.ongoing || ui.event.paused) {
        Spacer(Modifier.height(4.dp))
        return
    }
    val p = ui.rhythm.progress(now)
    LinearProgressIndicator(
        progress = { p.coerceAtMost(1f) },
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
        color = color,
        trackColor = color.copy(alpha = 0.18f),
        drawStopIndicator = {},
    )
}

@Composable
fun EmojiBadge(emoji: String, color: Color, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji.ifBlank { "•" }, fontSize = (size * 0.5).sp)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}
