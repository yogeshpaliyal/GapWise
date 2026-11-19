package com.yogeshpaliyal.gapwise.ui.detail

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yogeshpaliyal.gapwise.data.LogEntity
import com.yogeshpaliyal.gapwise.data.LogResult
import com.yogeshpaliyal.gapwise.domain.DAY_MS
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.LogEditorDialog
import com.yogeshpaliyal.gapwise.ui.home.EmojiBadge
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import com.yogeshpaliyal.gapwise.ui.home.SectionLabel
import com.yogeshpaliyal.gapwise.ui.home.eventColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, vm: DetailViewModel = viewModel()) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var addingPast by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LogEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.logged.collect { r ->
            val msg = if (r is LogResult.Ended) "Ended" else "Logged"
            val res = snackbar.showSnackbar(msg, actionLabel = "Undo")
            if (res == SnackbarResult.ActionPerformed) vm.undo(r)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui?.event?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { onEdit(vm.eventId) }) { Icon(Icons.Outlined.Edit, "Edit") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                        val e = ui?.event
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (e?.pinned == true) "Unpin" else "Pin to top") },
                                onClick = { vm.togglePin(); menu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (e?.paused == true) "Resume" else "Pause (travel, injury…)") },
                                onClick = { vm.togglePaused(); menu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (e?.hidden == true) "Unhide" else "Hide from home") },
                                onClick = { vm.toggleHidden(); menu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    menu = false
                                    ui?.let { context.startActivity(Intent.createChooser(shareIntent(it), "Share")) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { confirmDelete = true; menu = false },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val item = ui
        if (item == null) {
            Box(Modifier.padding(padding).fillMaxSize())
            return@Scaffold
        }
        val color = eventColor(item.event.color)
        val logs = item.item.sortedLogs.asReversed()
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Header(item, color, onLog = { vm.logNow() }, onPast = { addingPast = true }) }
            item { RhythmCard(item) }
            if (item.rhythm.gaps.isNotEmpty()) {
                item {
                    InfoCard("Gaps between logs") {
                        GapChart(item.rhythm.gaps, item.rhythm.usualDays, color)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Dashed line is your usual. Faded bars are one-off long gaps that don't count.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { InfoCard("Calendar") { LogCalendar(item.item.logs, color) } }
            item { SectionLabel("History · ${logs.size}", Modifier.padding(top = 4.dp)) }
            if (logs.isEmpty()) {
                item { Text("No logs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(logs, key = { it.id }) { log ->
                LogRow(log, item.event.isMultiDay, onClick = { editing = log })
                HorizontalDivider()
            }
        }
    }

    val event = ui?.event
    if (addingPast && event != null) {
        LogEditorDialog(
            title = "Log a past time",
            multiDay = event.isMultiDay,
            initialStart = System.currentTimeMillis() - DAY_MS,
            initialEnd = if (event.isMultiDay) System.currentTimeMillis() - DAY_MS else null,
            initialNote = null,
            onDismiss = { addingPast = false },
            onSave = { s, e, n -> vm.addLog(s, e, n); addingPast = false },
        )
    }
    editing?.let { log ->
        LogEditorDialog(
            title = "Edit log",
            multiDay = event?.isMultiDay == true,
            initialStart = log.startAt,
            initialEnd = log.endAt,
            initialNote = log.note,
            onDismiss = { editing = null },
            onSave = { s, e, n -> vm.updateLog(log.copy(startAt = s, endAt = e, note = n)); editing = null },
            onDelete = { vm.deleteLog(log.id); editing = null },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${event?.name}”?") },
            text = { Text("This removes the event and all of its history.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(ui: EventUi, color: Color, onLog: () -> Unit, onPast: () -> Unit) {
    val now = System.currentTimeMillis()
    val ongoing = ui.rhythm.ongoingSince
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        EmojiBadge(ui.event.emoji, color, size = 72)
        Spacer(Modifier.height(12.dp))
        if (ongoing != null) {
            Text("Day ${TimeText.daysBetween(ongoing, now) + 1}", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
            Text("ongoing since ${TimeText.date(ongoing)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val (n, u) = TimeText.elapsed(ui.rhythm.lastAt, now)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(n, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(u, modifier = Modifier.padding(bottom = 10.dp))
            }
            ui.rhythm.lastAt?.let {
                Text("Last time: ${TimeText.dateTime(it)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ui.event.paused) {
            Spacer(Modifier.height(6.dp))
            Text("Paused — no nudges until you resume. A long gap from this pause is ignored as a one-off.", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val label = when {
                !ui.event.isMultiDay -> "Log now"
                ongoing != null -> "End now"
                else -> "Start now"
            }
            Button(onClick = onLog) { Text(label) }
            OutlinedButton(onClick = onPast) { Text("Log a past day") }
        }
    }
}

@Composable
private fun RhythmCard(ui: EventUi) {
    val r = ui.rhythm
    InfoCard("Rhythm") {
        if (r.usualDays == null) {
            val left = (3 - r.logCount).coerceAtLeast(1)
            Text("Log this $left more time${if (left > 1) "s" else ""} and Gapwise will learn how often you usually do it.")
        } else {
            Text(TimeText.usualText(r.usualDays), style = MaterialTheme.typography.titleMedium)
            r.nextExpectedAt?.let {
                Text("Next expected around ${TimeText.date(it)} · ${TimeText.dueText(it)}")
            }
        }
        r.usualLengthDays?.let { Text("Usually lasts ${it.roundToInt()} days") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("Logs", "${r.logCount}")
            Stat("Shortest", r.gaps.filterNot { it.outlier }.minOfOrNull { it.days }?.let { "${it.roundToInt()}d" } ?: "—")
            Stat("Longest", r.gaps.maxOfOrNull { it.days }?.let { "${it.roundToInt()}d" } ?: "—")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel(title)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun LogRow(log: LogEntity, multiDay: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        val title = if (multiDay) {
            val end = log.endAt
            if (end == null) "${TimeText.date(log.startAt)} → ongoing"
            else "${TimeText.shortDate(log.startAt)} → ${TimeText.shortDate(end)} · ${TimeText.daysBetween(log.startAt, end) + 1} days"
        } else {
            TimeText.dateTime(log.startAt)
        }
        Text(title, style = MaterialTheme.typography.bodyLarge)
        log.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun shareIntent(ui: EventUi): Intent {
    val r = ui.rhythm
    val text = buildString {
        append("${ui.event.emoji} ${ui.event.name}: ${TimeText.elapsedInline(r.lastAt)}")
        r.usualDays?.let { append(" · ${TimeText.usualText(it).lowercase()}") }
        append("\n— tracked with Gapwise")
    }
    return Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
}
