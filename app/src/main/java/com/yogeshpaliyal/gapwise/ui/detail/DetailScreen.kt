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
import com.yogeshpaliyal.gapwise.ui.common.LogEditorSheet
import com.yogeshpaliyal.gapwise.ui.common.ConfirmRepeatDialog
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import com.yogeshpaliyal.gapwise.ui.home.eventColor
import com.yogeshpaliyal.gapwise.ui.home.RhythmBar
import com.yogeshpaliyal.gapwise.ui.home.statusAccent
import com.yogeshpaliyal.gapwise.ui.home.tint
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import com.yogeshpaliyal.gapwise.ui.common.status
import com.yogeshpaliyal.gapwise.ui.theme.CountStyle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
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
        ) {
            item { Header(item, color, onLog = { vm.logNow() }, onPast = { addingPast = true }) }
            item { Spacer(Modifier.height(16.dp)) }
            item { RhythmCard(item) }
            if (item.rhythm.gaps.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    InfoCard("Time between logs") {
                        GapChart(item.rhythm.gaps, item.rhythm.usualDays, color)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The dashed line is your usual gap. Faded bars are one-off long gaps, and they don't count.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item { InfoCard("Calendar") { LogCalendar(item.item.logs, color) } }
            item {
                Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                    Text("History", style = MaterialTheme.typography.headlineSmall)
                    if (logs.isNotEmpty()) {
                        Text(
                            "Tap any entry to change its date, time or note.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (logs.isEmpty()) {
                item { Text("Nothing logged yet. Tap Log now, or add a past day.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            historyTimeline(
                logs = item.item.logs,
                gaps = item.rhythm.gaps,
                multiDay = item.event.isMultiDay,
                color = color,
                usualDays = item.rhythm.usualDays,
                onClick = { editing = it },
            )
        }
    }

    val confirm by vm.confirm.collectAsStateWithLifecycle()
    confirm?.let { p ->
        ConfirmRepeatDialog(prompt = p, onConfirm = { vm.confirmRepeat() }, onDismiss = vm::cancelRepeat)
    }

    val event = ui?.event
    if (addingPast && event != null) {
        LogEditorSheet(
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
        LogEditorSheet(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Header(ui: EventUi, color: Color, onLog: () -> Unit, onPast: () -> Unit) {
    val now = System.currentTimeMillis()
    val ongoing = ui.rhythm.ongoingSince
    val status = ui.status(now)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        StatusBadge(ui.event.emoji, status, container = tint(statusAccent(status, color), 0.38f), size = 132.dp)
        Spacer(Modifier.height(10.dp))
        Text(status.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (ongoing != null) {
            Text("Day ${TimeText.daysBetween(ongoing, now) + 1}", style = CountStyle.copy(fontSize = 80.sp, lineHeight = 80.sp))
            Text("Started ${TimeText.date(ongoing)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val (n, u) = TimeText.elapsed(ui.rhythm.lastAt, now)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(n, style = CountStyle.copy(fontSize = 80.sp, lineHeight = 80.sp))
                Spacer(Modifier.width(10.dp))
                Text(u, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            }
            ui.rhythm.lastAt?.let {
                Text("Last time ${TimeText.dateTime(it)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ui.event.paused) {
            Spacer(Modifier.height(6.dp))
            Text(
                "No nudges until you resume. The gap from this pause won't count against your rhythm.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = when {
                !ui.event.isMultiDay -> "Log now"
                ongoing != null -> "End now"
                else -> "Start now"
            }
            Button(
                onClick = onLog,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp),
                contentPadding = PaddingValues(horizontal = 28.dp),
            ) { Text(label, style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(
                onClick = onPast,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) { Text("Log a past day", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun RhythmCard(ui: EventUi) {
    val r = ui.rhythm
    val now = System.currentTimeMillis()
    InfoCard("Rhythm") {
        if (r.usualDays == null) {
            val left = (3 - r.logCount).coerceAtLeast(1)
            Text(
                "Log this $left more time${if (left > 1) "s" else ""} and GapWise will learn how often you usually do it.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(TimeText.usualText(r.usualDays), style = MaterialTheme.typography.headlineSmall)
            r.nextExpectedAt?.let {
                Text(
                    "Expected around ${TimeText.date(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            RhythmBar(ui, ui.status(now), now, eventColor(ui.event.color))
        }
        r.usualLengthDays?.let { Text("Usually lasts ${it.roundToInt()} days", style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Stat("Times logged", "${r.logCount}")
            Stat("Shortest gap", r.gaps.filterNot { it.outlier }.minOfOrNull { it.days }?.let { "${it.roundToInt()}d" } ?: "—")
            Stat("Longest gap", r.gaps.maxOfOrNull { it.days }?.let { "${it.roundToInt()}d" } ?: "—")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = CountStyle.copy(fontSize = 26.sp, lineHeight = 28.sp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

private fun shareIntent(ui: EventUi): Intent {
    val r = ui.rhythm
    val text = buildString {
        append("${ui.event.emoji} ${ui.event.name}: ${TimeText.elapsedInline(r.lastAt)}")
        r.usualDays?.let { append(" · ${TimeText.usualText(it).lowercase()}") }
        append("\n— tracked with GapWise")
    }
    return Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
}
