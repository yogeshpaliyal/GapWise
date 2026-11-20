package com.yogeshpaliyal.gapwise.ui.common

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

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
            }) { Text(L10n.s(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L10n.s(R.string.cancel)) } },
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
            }) { Text(L10n.s(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L10n.s(R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}
