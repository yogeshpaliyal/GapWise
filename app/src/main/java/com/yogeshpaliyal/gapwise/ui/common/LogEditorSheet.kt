package com.yogeshpaliyal.gapwise.ui.common

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogeshpaliyal.gapwise.domain.DAY_MS
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.theme.CountStyle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Moves [ms] to [date], keeping its time of day. */
private fun onDate(ms: Long, date: LocalDate): Long {
    val zone = ZoneId.systemDefault()
    val time = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalTime()
    return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}

private fun longDate(ms: Long): String {
    val d = TimeText.localDate(ms)
    val today = LocalDate.now()
    return when (d) {
        today -> L10n.s(R.string.today)
        today.minusDays(1) -> L10n.s(R.string.yesterday)
        else -> L10n.s(R.string.weekday_time, d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), TimeText.shortDate(ms))
    }
}

/**
 * Add or edit one log, as a bottom sheet. The date is the headline; quick
 * buttons cover the common cases (today, yesterday) and the tiles open the
 * full pickers for anything else.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogEditorSheet(
    title: String,
    multiDay: Boolean,
    initialStart: Long,
    initialEnd: Long?,
    initialNote: String?,
    onDismiss: () -> Unit,
    onSave: (start: Long, end: Long?, note: String?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var start by remember { mutableLongStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var note by remember { mutableStateOf(initialNote.orEmpty()) }
    var picker by remember { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(longDate(start), style = CountStyle.copy(fontSize = 36.sp, lineHeight = 40.sp))
            }

            // Quick date choices.
            val today = LocalDate.now()
            val picked = TimeText.localDate(start)
            val quick = listOf(L10n.s(R.string.today) to today, L10n.s(R.string.yesterday) to today.minusDays(1))
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                quick.forEachIndexed { i, (label, date) ->
                    ToggleButton(
                        checked = picked == date,
                        onCheckedChange = { start = onDate(start, date) },
                        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedMiddleButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(label) }
                }
                ToggleButton(
                    checked = picked.isBefore(today.minusDays(1)),
                    onCheckedChange = { picker = "startDate" },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(L10n.s(R.string.earlier)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerTile(
                    icon = Icons.Outlined.CalendarMonth,
                    label = if (multiDay) L10n.s(R.string.started) else L10n.s(R.string.date),
                    value = TimeText.date(start),
                    onClick = { picker = "startDate" },
                    modifier = Modifier.weight(1f),
                )
                PickerTile(
                    icon = Icons.Outlined.Schedule,
                    label = L10n.s(R.string.time),
                    value = TimeText.time(start),
                    onClick = { picker = "startTime" },
                    modifier = Modifier.weight(1f),
                )
            }

            if (multiDay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L10n.s(R.string.still_going), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (end == null) L10n.s(R.string.still_going_off) else L10n.s(R.string.still_going_on),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = end == null,
                        onCheckedChange = { going -> end = if (going) null else maxOf(start, start + DAY_MS) },
                    )
                }
                end?.let { e ->
                    val days = TimeText.daysBetween(start, e) + 1
                    PickerTile(
                        icon = Icons.Outlined.EventAvailable,
                        label = L10n.s(R.string.ended),
                        value = L10n.p(R.plurals.ended_value, days.toInt(), TimeText.date(e), days.toInt()),
                        onClick = { picker = "endDate" },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(L10n.s(R.string.note_hint)) },
                label = { Text(L10n.s(R.string.note)) },
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedContent(targetState = confirmingDelete, label = "actions") { confirming ->
                if (confirming && onDelete != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(L10n.s(R.string.delete_log_q), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { confirmingDelete = false }) { Text(L10n.s(R.string.keep)) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onDelete,
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text(L10n.s(R.string.delete)) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (onDelete != null) {
                            FilledTonalButton(
                                onClick = { confirmingDelete = true },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.heightIn(min = 56.dp),
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(L10n.s(R.string.delete))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { onSave(start, end?.coerceAtLeast(start), note.trim().ifBlank { null }) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 56.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
                        ) { Text(L10n.s(R.string.save), style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
    }

    when (picker) {
        "startDate" -> PastDatePickerDialog(start, { picker = null }) { start = it }
        "startTime" -> TimePickerDialog(start, { picker = null }) { start = it }
        "endDate" -> PastDatePickerDialog(end ?: start, { picker = null }) { end = it }
    }
}

@Composable
internal fun PickerTile(icon: ImageVector, label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
