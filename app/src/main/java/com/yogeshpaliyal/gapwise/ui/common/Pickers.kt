package com.yogeshpaliyal.gapwise.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yogeshpaliyal.gapwise.domain.TimeText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private fun utcMidnight(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun fromUtcMidnight(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

private fun combine(date: LocalDate, time: LocalTime): Long =
    date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun timeOf(ms: Long): LocalTime = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()

@OptIn(ExperimentalMaterial3Api::class)
private object PastOrToday : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        !fromUtcMidnight(utcTimeMillis).isAfter(LocalDate.now())
}

/** Picks a date (never in the future), keeping the time-of-day of [initial]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(initial: Long, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = utcMidnight(TimeText.localDate(initial)),
        selectableDates = PastOrToday,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(combine(fromUtcMidnight(it), timeOf(initial))) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: Long, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val t = timeOf(initial)
    val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = t.hour, initialMinute = t.minute, is24Hour = is24)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPicked(combine(TimeText.localDate(initial), LocalTime.of(state.hour, state.minute)))
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

/**
 * Add or edit a single log. For multi-day events there is also an end date
 * (leave it empty for something that's still going on).
 */
@Composable
fun LogEditorDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (multiDay) "Started" else "When")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker = "startDate" }) { Text(TimeText.date(start)) }
                    OutlinedButton(onClick = { picker = "startTime" }) { Text(TimeText.time(start)) }
                }
                if (multiDay) {
                    Text("Ended")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { picker = "endDate" }) {
                            Text(end?.let { TimeText.date(it) } ?: "Still ongoing")
                        }
                        if (end != null) TextButton(onClick = { end = null }) { Text("Clear") }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val fixedEnd = end?.coerceAtLeast(start)
                onSave(start, fixedEnd, note.ifBlank { null })
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    when (picker) {
        "startDate" -> PastDatePickerDialog(start, { picker = null }) { start = it }
        "startTime" -> TimePickerDialog(start, { picker = null }) { start = it }
        "endDate" -> PastDatePickerDialog(end ?: start, { picker = null }) { end = it }
    }
}
