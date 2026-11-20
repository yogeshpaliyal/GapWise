package com.yogeshpaliyal.gapwise.widget

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.RadioButton
import androidx.compose.material3.toShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import com.yogeshpaliyal.gapwise.ui.common.status
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import com.yogeshpaliyal.gapwise.ui.home.elapsed
import com.yogeshpaliyal.gapwise.ui.home.eventColor
import com.yogeshpaliyal.gapwise.ui.home.tint
import com.yogeshpaliyal.gapwise.ui.theme.GapwiseTheme
import kotlinx.coroutines.launch

/**
 * Opens when you add (or reconfigure) the "Event tiles" widget: pick which
 * events it shows, in the order you tap them.
 */
class TilesWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // If the user backs out, the launcher won't add the widget.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // The same picker serves both widgets; the list widget just lays them out as rows.
        val isList = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
            ?.provider?.className == GapwiseWidgetReceiver::class.java.name

        setContent {
            GapwiseTheme {
                val selected = remember { mutableStateListOf<Long>() }
                var loaded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    // Pre-select what this widget already shows, if anything.
                    runCatching {
                        val glanceId = GlanceAppWidgetManager(this@TilesWidgetConfigActivity).getGlanceIdBy(appWidgetId)
                        val prefs = getAppWidgetState(this@TilesWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                        selected.addAll(prefs[TileIdsKey].orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() })
                    }
                    loaded = true
                }
                ConfigScreen(
                    isList = isList,
                    loaded = loaded,
                    selected = selected,
                    onCancel = { finish() },
                    onSave = { save(selected.toList(), isList) },
                )
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun save(ids: List<Long>, isList: Boolean) {
        lifecycleScope.launch {
            val saved = runCatching {
                val glanceId = GlanceAppWidgetManager(this@TilesWidgetConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@TilesWidgetConfigActivity, glanceId) { prefs ->
                    prefs[TileIdsKey] = ids.joinToString(",")
                }
                val widget = if (isList) GapwiseWidget() else GapwiseTilesWidget()
                widget.update(this@TilesWidgetConfigActivity, glanceId)
            }.isSuccess
            if (saved) setResult(RESULT_OK, resultIntent())
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfigScreen(
    isList: Boolean,
    loaded: Boolean,
    selected: MutableList<Long>,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val items by context.app.repository.observeAll().collectAsState(initial = null)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val now = System.currentTimeMillis()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(if (isList) L10n.s(R.string.cfg_title_list) else L10n.s(R.string.cfg_title_tiles)) },
                subtitle = {
                    Text(
                        if (isList) L10n.s(R.string.cfg_sub_list, MAX_TILES)
                        else L10n.s(R.string.cfg_sub_tiles, MAX_TILES),
                    )
                },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, L10n.s(R.string.cancel)) } },
                scrollBehavior = scroll,
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selected.isEmpty()) L10n.s(R.string.cfg_automatic) else L10n.p(R.plurals.cfg_picked, selected.size),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(gridLabel(selected.size, isList), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = { selected.clear() }) { Text(L10n.s(R.string.clear)) }
                        Spacer(Modifier.width(4.dp))
                    }
                    Button(
                        onClick = onSave,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.heightIn(min = 56.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp),
                    ) { Text(L10n.s(R.string.done), style = MaterialTheme.typography.titleMedium) }
                }
            }
        },
    ) { padding ->
        val list = items
        if (!loaded || list == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        if (list.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(L10n.s(R.string.cfg_no_events), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The "pick nothing" choice, spelled out, so an empty selection is clearly on purpose.
            item(key = "automatic") {
                val auto = selected.isEmpty()
                Surface(
                    onClick = { selected.clear() },
                    shape = MaterialTheme.shapes.large,
                    color = if (auto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialShapes.Sunny.toShape(),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, null) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(L10n.s(R.string.cfg_auto_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isList) L10n.s(R.string.cfg_auto_list)
                                else L10n.s(R.string.cfg_auto_tiles),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = auto, onClick = { selected.clear() })
                    }
                }
            }
            item(key = "or") {
                Text(
                    L10n.s(R.string.cfg_or_pick),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                )
            }
            items(list.filter { !it.event.hidden }, key = { it.event.id }) { item ->
                val ui = EventUi(item, RhythmCalculator.compute(item))
                val color = eventColor(item.event.color)
                val position = selected.indexOf(item.event.id)
                val on = position >= 0
                Surface(
                    onClick = {
                        if (on) selected.remove(item.event.id)
                        else if (selected.size < MAX_TILES) selected.add(item.event.id)
                    },
                    shape = MaterialTheme.shapes.large,
                    color = if (on) tint(color, 0.24f) else MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(item.event.emoji, ui.status(now), container = tint(color, 0.3f), size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val (n, u) = ui.elapsed(now)
                            Text("$n $u", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Position in the widget, since order matters.
                        Surface(
                            shape = CircleShape,
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (on) "${position + 1}" else "", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
