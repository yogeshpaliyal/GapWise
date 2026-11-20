package com.yogeshpaliyal.gapwise.ui.welcome

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

private val agoOptions = listOf(
    Ago("Today", 0), Ago("Yesterday", 1), Ago("A few days ago", 3), Ago("About a week ago", 7),
    Ago("A couple of weeks ago", 14), Ago("About a month ago", 30), Ago("A few months ago", 90),
)

/** How long a multi-day event lasted, in days; null = still going. */
private val lengths: List<Pair<String, Int?>> = listOf(
    "1 day" to 1, "2 days" to 2, "3 days" to 3, "A week" to 7, "Still going" to null,
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
            else current?.let { c -> lengths.firstOrNull { it.second != null && c.label.endsWith("· ${it.first}") } } ?: lengths[1],
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
                        if (suggestion.multiDay) "When did it last start?" else "When did you last do it?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                agoOptions.forEach { a ->
                    ToggleButton(checked = choice == When.Preset(a), onCheckedChange = { choice = When.Preset(a) }) { Text(a.label) }
                }
                val picked = choice as? When.Picked
                ToggleButton(checked = picked != null, onCheckedChange = { pickDate = true }) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.padding(end = 6.dp))
                    Text(picked?.let { TimeText.date(it.at) } ?: "Pick a date")
                }
                ToggleButton(checked = choice == When.NotSure, onCheckedChange = { choice = When.NotSure }) { Text("Not sure") }
            }

            if (suggestion.multiDay && choice != null && choice != When.NotSure) {
                Text("How long did it last?", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lengths.forEach { l ->
                        ToggleButton(checked = length == l, onCheckedChange = { length = l }) { Text(l.first) }
                    }
                }
            }

            if (choice == When.NotSure) {
                Text(
                    "We'll guess from how often people usually do this. Fix it later from the event's page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (current != null) TextButton(onClick = onRemove) { Text("Remove") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { choice?.let { onPick(resolve(it, suggestion.multiDay, length)) } },
                    enabled = choice != null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 52.dp),
                ) { Text(if (current == null) "Add" else "Save") }
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
        When.NotSure -> return LastTime(null, null, "Not sure, we'll guess")
    }
    if (!multiDay) return LastTime(start, null, label)
    val days = length.second ?: return LastTime(start, null, "$label · ${length.first}")
    val end = (start + (days - 1) * DAY).coerceAtMost(now)
    return LastTime(start, end, "$label · ${length.first}")
}
