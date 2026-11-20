package com.yogeshpaliyal.gapwise.ui.home

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import com.yogeshpaliyal.gapwise.ui.common.status
import com.yogeshpaliyal.gapwise.ui.theme.CountStyle
import com.yogeshpaliyal.gapwise.ui.theme.LocalStatusColors

fun eventColor(argb: Long) = Color(argb.toInt())

/** A soft wash of the event's colour over the surface, so tiles stay readable. */
@Composable
fun tint(color: Color, amount: Float = 0.16f): Color =
    color.copy(alpha = amount).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

fun EventUi.statusLine(now: Long): String = when {
    event.paused -> L10n.s(R.string.status_paused)
    ongoing -> L10n.s(R.string.tile_day_so_far, (TimeText.daysBetween(rhythm.ongoingSince!!, now) + 1).toInt())
    rhythm.hasRhythm -> TimeText.dueText(rhythm.nextExpectedAt, now) ?: ""
    rhythm.logCount == 0 -> L10n.s(R.string.tile_tap_first)
    else -> L10n.s(R.string.tile_log_more)
}

fun EventUi.elapsed(now: Long): Pair<String, String> =
    if (ongoing) TimeText.elapsed(rhythm.ongoingSince, now).let { it.first to L10n.s(R.string.tile_since_started) }
    else TimeText.elapsed(rhythm.lastAt, now)

@Composable
fun statusAccent(status: GapStatus, eventColor: Color): Color {
    val s = LocalStatusColors.current
    return when (status) {
        GapStatus.DUE -> s.due
        GapStatus.PAST -> s.past
        GapStatus.FRESH -> s.fresh
        GapStatus.PAUSED, GapStatus.NEW -> MaterialTheme.colorScheme.outline
        else -> eventColor
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventTile(ui: EventUi, now: Long, onLog: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val color = eventColor(ui.event.color)
    val status = ui.status(now)
    val (num, unit) = ui.elapsed(now)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = tint(color),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClickLabel = L10n.s(R.string.a11y_log_event, ui.event.name),
                onLongClickLabel = L10n.s(R.string.a11y_open_details),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onLog()
                },
                onLongClick = onOpen,
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                StatusBadge(ui.event.emoji, status, container = tint(color, 0.30f), size = 52.dp)
                Spacer(Modifier.weight(1f))
                if (ui.event.pinned) {
                    Icon(Icons.Filled.PushPin, L10n.s(R.string.a11y_pinned), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(ui.event.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(num, style = CountStyle)
                Spacer(Modifier.width(6.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text(
                ui.statusLine(now),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            RhythmBar(ui, status, now, color)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventRow(ui: EventUi, now: Long, onLog: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val color = eventColor(ui.event.color)
    val status = ui.status(now)
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tint(color, 0.12f),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClickLabel = L10n.s(R.string.a11y_log_event, ui.event.name),
                onLongClickLabel = L10n.s(R.string.a11y_open_details),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onLog()
                },
                onLongClick = onOpen,
            ),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(ui.event.emoji, status, container = tint(color, 0.28f), size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(ui.event.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    ui.statusLine(now),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                RhythmBar(ui, status, now, color)
            }
            Spacer(Modifier.width(14.dp))
            val (num, unit) = ui.elapsed(now)
            Column(horizontalAlignment = Alignment.End) {
                Text(num, style = CountStyle.copy(fontSize = 28.sp, lineHeight = 28.sp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Progress toward your usual gap. It turns wavy only when something is around
 * now or past usual, so the wave itself means "this one".
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhythmBar(ui: EventUi, status: GapStatus, now: Long, color: Color, modifier: Modifier = Modifier) {
    if (!ui.rhythm.hasRhythm || ui.ongoing || ui.event.paused) {
        Spacer(modifier.height(4.dp))
        return
    }
    val p = ui.rhythm.progress(now).coerceAtMost(1f)
    val accent = statusAccent(status, color)
    if (status == GapStatus.DUE || status == GapStatus.PAST) {
        LinearWavyProgressIndicator(
            progress = { p },
            modifier = modifier.fillMaxWidth(),
            color = accent,
            trackColor = accent.copy(alpha = 0.22f),
        )
    } else {
        LinearProgressIndicator(
            progress = { p },
            modifier = modifier.fillMaxWidth(),
            color = accent,
            trackColor = accent.copy(alpha = 0.18f),
        )
    }
}

/** The one big card at the top of home: whatever is most due right now. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpNextCard(ui: EventUi, now: Long, onLog: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val color = eventColor(ui.event.color)
    val status = ui.status(now)
    val s = LocalStatusColors.current
    val (num, unit) = ui.elapsed(now)
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        color = tint(color, 0.22f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(ui.event.emoji, status, container = tint(s.due, 0.55f), size = 76.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(L10n.s(R.string.up_next), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ui.event.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(num, style = CountStyle.copy(fontSize = 64.sp, lineHeight = 64.sp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(unit, style = MaterialTheme.typography.titleMedium)
                    Text(ui.statusLine(now), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            RhythmBar(ui, status, now, color)
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onLog, shapes = ButtonDefaults.shapes()) {
                Text(
                    when {
                        !ui.event.isMultiDay -> L10n.s(R.string.action_log_now)
                        ui.ongoing -> L10n.s(R.string.action_end_now)
                        else -> L10n.s(R.string.action_start_now)
                    },
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
