package com.yogeshpaliyal.gapwise.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yogeshpaliyal.gapwise.data.LogResult
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import com.yogeshpaliyal.gapwise.ui.common.status

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var searching by rememberSaveable { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        vm.logged.collect { (name, result) ->
            val msg = when (result) {
                is LogResult.Added -> "Logged $name"
                is LogResult.Ended -> "Ended $name"
            }
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(msg, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) vm.undo(result)
        }
    }

    val now = state.now
    val statuses = state.events.map { it.status(now) }
    val upNext = state.events
        .filter { val s = it.status(now); s == GapStatus.DUE || s == GapStatus.PAST }
        .minByOrNull { it.rhythm.nextExpectedAt ?: Long.MAX_VALUE }
        ?.takeIf { state.filters.query.isBlank() }
    val rest = state.events.filter { it !== upNext }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Gapwise") },
                subtitle = { Text(summary(statuses, state.totalCount)) },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, "New event")
                    }
                },
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            ) {
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) vm.setQuery("")
                }) {
                    Icon(if (searching) Icons.Filled.Close else Icons.Outlined.Search, if (searching) "Close search" else "Search")
                }
                IconButton(onClick = { options = true }) { Icon(Icons.Outlined.Tune, "Sort and layout") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (searching) {
                OutlinedTextField(
                    value = state.filters.query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Search your events") },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (state.categories.size > 1) {
                CategoryToggles(state.categories, state.filters.category, vm::setCategory)
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                state.totalCount == 0 -> EmptyState(
                    "Track your first thing",
                    "Add something you do now and then, like a haircut, an oil change or calling home. Tap + to start.",
                )
                state.events.isEmpty() -> EmptyState("Nothing matches", "Try another word, or clear the category filter.")
                else -> LazyVerticalGrid(
                    columns = if (state.filters.grid) GridCells.Adaptive(minSize = 164.dp) else GridCells.Fixed(1),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "update", span = { GridItemSpan(maxLineSpan) }) {
                        com.yogeshpaliyal.gapwise.update.UpdateBanner()
                    }
                    if (upNext != null) {
                        item(key = "upnext", span = { GridItemSpan(maxLineSpan) }) {
                            UpNextCard(
                                upNext, now,
                                onLog = { vm.log(upNext) },
                                onOpen = { onOpen(upNext.event.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(rest, key = { it.event.id }) { ui ->
                        if (state.filters.grid) {
                            EventTile(ui, now, onLog = { vm.log(ui) }, onOpen = { onOpen(ui.event.id) }, modifier = Modifier.animateItem())
                        } else {
                            EventRow(ui, now, onLog = { vm.log(ui) }, onOpen = { onOpen(ui.event.id) }, modifier = Modifier.animateItem())
                        }
                    }
                    item(key = "hint", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Tap a card to log it. Press and hold to see its history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (options) {
        ModalBottomSheet(onDismissRequest = { options = false }) {
            ViewOptions(state, vm)
        }
    }
}

private fun summary(statuses: List<GapStatus>, total: Int): String {
    if (total == 0) return "Remember the last time. Know the next."
    val due = statuses.count { it == GapStatus.DUE }
    val past = statuses.count { it == GapStatus.PAST }
    return when {
        due == 0 && past == 0 -> "Nothing is due. Enjoy the quiet."
        past == 0 -> "$due around now"
        due == 0 -> "$past past your usual"
        else -> "$due around now, $past past your usual"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryToggles(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ToggleButton(checked = selected == null, onCheckedChange = { onSelect(null) }) { Text("All") }
        }
        items(categories) { c ->
            ToggleButton(checked = selected == c, onCheckedChange = { onSelect(if (selected == c) null else c) }) { Text(c) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ViewOptions(state: HomeState, vm: HomeViewModel) {
    Column(
        Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sort by", style = MaterialTheme.typography.titleLarge)
        val modes = SortMode.entries
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            modes.forEachIndexed { i, m ->
                ToggleButton(
                    checked = state.filters.sort == m,
                    onCheckedChange = { vm.setSort(m) },
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(m.shortLabel, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Layout", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            ToggleButton(
                checked = state.filters.grid,
                onCheckedChange = { if (!state.filters.grid) vm.toggleGrid() },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) { Text("Cards") }
            ToggleButton(
                checked = !state.filters.grid,
                onCheckedChange = { if (state.filters.grid) vm.toggleGrid() },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) { Text("List") }
        }
        if (state.hiddenCount > 0) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show hidden events", style = MaterialTheme.typography.titleMedium)
                    Text("${state.hiddenCount} hidden", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.filters.showHidden, onCheckedChange = { vm.toggleHidden() })
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusBadge("⏳", GapStatus.NEW, container = MaterialTheme.colorScheme.primaryContainer, size = 96.dp)
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
