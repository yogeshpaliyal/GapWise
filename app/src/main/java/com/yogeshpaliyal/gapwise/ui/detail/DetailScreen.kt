package com.yogeshpaliyal.gapwise.ui.detail

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

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
            val msg = if (r is LogResult.Ended) L10n.s(R.string.snack_ended_short) else L10n.s(R.string.snack_logged_short)
            val res = snackbar.showSnackbar(msg, actionLabel = L10n.s(R.string.undo))
            if (res == SnackbarResult.ActionPerformed) vm.undo(r)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui?.event?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, L10n.s(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = { onEdit(vm.eventId) }) { Icon(Icons.Outlined.Edit, L10n.s(R.string.edit)) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, L10n.s(R.string.more)) }
                        val e = ui?.event
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (e?.pinned == true) L10n.s(R.string.unpin) else L10n.s(R.string.pin_to_top)) },
                                onClick = { vm.togglePin(); menu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (e?.paused == true) L10n.s(R.string.resume) else L10n.s(R.string.pause_menu)) },
                                onClick = { vm.togglePaused(); menu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (e?.hidden == true) L10n.s(R.string.unhide) else L10n.s(R.string.hide_from_home)) },
                                onClick = { vm.toggleHidden(); menu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(L10n.s(R.string.share)) },
                                onClick = {
                                    menu = false
                                    ui?.let { context.startActivity(Intent.createChooser(shareIntent(it), L10n.s(R.string.share))) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(L10n.s(R.string.delete), color = MaterialTheme.colorScheme.error) },
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
                    InfoCard(L10n.s(R.string.card_gaps)) {
                        GapChart(item.rhythm.gaps, item.rhythm.usualDays, color)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            L10n.s(R.string.card_gaps_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item { InfoCard(L10n.s(R.string.card_calendar)) { LogCalendar(item.item.logs, color) } }
            item {
                Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                    Text(L10n.s(R.string.history), style = MaterialTheme.typography.headlineSmall)
                    if (logs.isNotEmpty()) {
                        Text(
                            L10n.s(R.string.history_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (logs.isEmpty()) {
                item { Text(L10n.s(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            title = L10n.s(R.string.log_past_time),
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
            title = L10n.s(R.string.edit_log),
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
            title = { Text(L10n.s(R.string.delete_title, event?.name.orEmpty())) },
            text = { Text(L10n.s(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text(L10n.s(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(L10n.s(R.string.cancel)) } },
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
            Text(L10n.s(R.string.day_n, (TimeText.daysBetween(ongoing, now) + 1).toInt()), style = CountStyle.copy(fontSize = 80.sp, lineHeight = 80.sp))
            Text(L10n.s(R.string.started_on, TimeText.date(ongoing)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val (n, u) = TimeText.elapsed(ui.rhythm.lastAt, now)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(n, style = CountStyle.copy(fontSize = 80.sp, lineHeight = 80.sp))
                Spacer(Modifier.width(10.dp))
                Text(u, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            }
            ui.rhythm.lastAt?.let {
                Text(L10n.s(R.string.last_time_on, TimeText.dateTime(it)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ui.event.paused) {
            Spacer(Modifier.height(6.dp))
            Text(
                L10n.s(R.string.paused_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = when {
                !ui.event.isMultiDay -> L10n.s(R.string.log_now)
                ongoing != null -> L10n.s(R.string.end_now)
                else -> L10n.s(R.string.start_now)
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
            ) { Text(L10n.s(R.string.log_past_day), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun RhythmCard(ui: EventUi) {
    val r = ui.rhythm
    val now = System.currentTimeMillis()
    InfoCard(L10n.s(R.string.card_rhythm)) {
        if (r.usualDays == null) {
            val left = (3 - r.logCount).coerceAtLeast(1)
            Text(
                L10n.p(R.plurals.rhythm_learning, left),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(TimeText.usualText(r.usualDays), style = MaterialTheme.typography.headlineSmall)
            r.nextExpectedAt?.let {
                Text(
                    L10n.s(R.string.expected_around, TimeText.date(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            RhythmBar(ui, ui.status(now), now, eventColor(ui.event.color))
        }
        r.usualLengthDays?.let { Text(L10n.p(R.plurals.usually_lasts, it.roundToInt()), style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Stat(L10n.s(R.string.stat_times), "${r.logCount}")
            Stat(L10n.s(R.string.stat_shortest), r.gaps.filterNot { it.outlier }.minOfOrNull { it.days }?.let { L10n.s(R.string.days_short, it.roundToInt()) } ?: "—")
            Stat(L10n.s(R.string.stat_longest), r.gaps.maxOfOrNull { it.days }?.let { L10n.s(R.string.days_short, it.roundToInt()) } ?: "—")
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
        r.usualDays?.let { append(" · " + TimeText.usualText(it).replaceFirstChar { c -> c.lowercase() }) }
        append("\n" + L10n.s(R.string.share_footer))
    }
    return Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
}
