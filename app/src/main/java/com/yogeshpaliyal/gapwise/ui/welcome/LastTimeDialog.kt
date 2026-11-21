package com.yogeshpaliyal.gapwise.ui.welcome

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yogeshpaliyal.gapwise.data.Suggestion
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.PastDatePickerDialog
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge

private const val DAY = 24 * 60 * 60 * 1000L

/** When a starter event was last done. [startAt] null means "not sure"; [endAt] null means still going. */
data class LastTime(val startAt: Long?, val endAt: Long?, val label: String)

private data class Ago(val label: String, val days: Int)

private val agoOptions: List<Ago> get() = listOf(
    Ago(L10n.s(R.string.today), 0), Ago(L10n.s(R.string.yesterday), 1), Ago(L10n.s(R.string.ago_few_days), 3), Ago(L10n.s(R.string.ago_week), 7),
    Ago(L10n.s(R.string.ago_two_weeks), 14), Ago(L10n.s(R.string.ago_month), 30), Ago(L10n.s(R.string.ago_few_months), 90),
)

/** How long a multi-day event lasted, in days; null = still going. */
private val lengths: List<Pair<String, Int?>> get() = listOf(
    L10n.p(R.plurals.n_days, 1) to 1, L10n.p(R.plurals.n_days, 2) to 2, L10n.p(R.plurals.n_days, 3) to 3, L10n.s(R.string.a_week) to 7, L10n.s(R.string.still_going) to null,
)

private sealed interface When {
    data class Preset(val ago: Ago) : When
    data class Picked(val at: Long) : When
    data object NotSure : When
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun LastTimeDialog(
    suggestion: Suggestion,
    current: LastTime?,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onPick: (LastTime) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var choice by remember {
        mutableStateOf<When?>(
            when {
                current == null -> null
                current.startAt == null -> When.NotSure
                else -> agoOptions.firstOrNull { it.label == current.label.substringBefore(" · ") }?.let { When.Preset(it) } ?: When.Picked(current.startAt)
            },
        )
    }
    var length by remember {
        mutableStateOf(
            if (current?.startAt != null && current.endAt == null) lengths.last()
            else current?.let { c -> lengths.firstOrNull { it.second != null && c.label == L10n.s(R.string.last_time_with_length, c.label.substringBefore(" · "), it.first) } } ?: lengths[1],
        )
    }
    var pickDate by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(suggestion.emoji, GapStatus.FRESH, container = MaterialTheme.colorScheme.primaryContainer, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(suggestion.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            suggestion.milestone -> L10n.s(R.string.ask_milestone_date)
                            suggestion.multiDay -> L10n.s(R.string.ask_last_start)
                            else -> L10n.s(R.string.ask_last_do)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // A milestone needs its real date, so it gets the calendar only.
                if (!suggestion.milestone) agoOptions.forEach { a ->
                    ToggleButton(checked = choice == When.Preset(a), onCheckedChange = { choice = When.Preset(a) }) { Text(a.label) }
                }
                val picked = choice as? When.Picked
                ToggleButton(checked = picked != null, onCheckedChange = { pickDate = true }) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.padding(end = 6.dp))
                    Text(picked?.let { TimeText.date(it.at) } ?: L10n.s(R.string.pick_date))
                }
                if (!suggestion.milestone) {
                    ToggleButton(checked = choice == When.NotSure, onCheckedChange = { choice = When.NotSure }) { Text(L10n.s(R.string.not_sure)) }
                }
            }

            if (suggestion.multiDay && choice != null && choice != When.NotSure) {
                Text(L10n.s(R.string.ask_how_long), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lengths.forEach { l ->
                        ToggleButton(checked = length == l, onCheckedChange = { length = l }) { Text(l.first) }
                    }
                }
            }

            if (choice == When.NotSure) {
                Text(
                    L10n.s(R.string.not_sure_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (current != null) TextButton(onClick = onRemove) { Text(L10n.s(R.string.remove)) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { choice?.let { onPick(resolve(it, suggestion.multiDay, length)) } },
                    enabled = choice != null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 52.dp),
                ) { Text(if (current == null) L10n.s(R.string.add) else L10n.s(R.string.save)) }
            }
        }
    }

    if (pickDate) {
        PastDatePickerDialog(
            (choice as? When.Picked)?.at ?: System.currentTimeMillis(),
            { pickDate = false },
        ) { choice = When.Picked(it) }
    }
}

private fun resolve(choice: When, multiDay: Boolean, length: Pair<String, Int?>): LastTime {
    val now = System.currentTimeMillis()
    val (start, label) = when (choice) {
        is When.Preset -> (now - choice.ago.days * DAY) to choice.ago.label
        is When.Picked -> choice.at to TimeText.date(choice.at)
        When.NotSure -> return LastTime(null, null, L10n.s(R.string.not_sure_label))
    }
    if (!multiDay) return LastTime(start, null, label)
    val days = length.second ?: return LastTime(start, null, L10n.s(R.string.last_time_with_length, label, length.first))
    val end = (start + (days - 1) * DAY).coerceAtMost(now)
    return LastTime(start, end, L10n.s(R.string.last_time_with_length, label, length.first))
}
