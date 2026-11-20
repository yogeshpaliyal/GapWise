package com.yogeshpaliyal.gapwise.widget

import com.yogeshpaliyal.gapwise.L10n

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import com.yogeshpaliyal.gapwise.ui.common.ConfirmLogActivity
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.yogeshpaliyal.gapwise.MainActivity
import com.yogeshpaliyal.gapwise.R
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator

internal val EventIdKey = ActionParameters.Key<Long>("eventId")

/** Intent that opens the editor for a new event. */
internal fun addEventIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_ADD_EVENT)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** "Due soon" list in Material 3 widget style. Tap a row to log it; + adds an event. */
class GapwiseWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    /** Holds the events picked for this widget (same key as the tiles widget). */
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.app.repository
        val initial = repo.getAll()
        provideContent {
            // Read the database live: logging from the widget (or the app) updates it right away.
            val items by remember { repo.observeAll() }.collectAsState(initial)
            val now by remember { widgetTicker }.collectAsState(System.currentTimeMillis())
            val chosen = currentState<Preferences>()[TileIdsKey].orEmpty()
                .split(',').mapNotNull { it.trim().toLongOrNull() }
            val rows = remember(items, chosen, now) { buildRows(items, chosen, now) }
            GapwiseGlanceTheme { Content(rows, picked = chosen.isNotEmpty()) }
        }
    }

    private fun buildRows(items: List<EventWithLogs>, chosen: List<Long>, now: Long): List<WidgetEvent> {
        // Picked events, in the order picked.
        if (chosen.isNotEmpty()) {
            val byId = items.associateBy { it.event.id }
            return chosen.mapNotNull { byId[it] }.take(MAX_TILES).map { it.toWidgetEvent(now) }
        }
        // Nothing picked: what's due soonest.
        return items
            .filter { !it.event.hidden && !it.event.paused }
            .sortedWith(
                compareByDescending<EventWithLogs> { it.event.pinned }
                    .thenBy { RhythmCalculator.compute(it).nextExpectedAt ?: Long.MAX_VALUE },
            )
            .take(12)
            .map { it.toWidgetEvent(now) }
    }

    @Composable
    private fun Content(rows: List<WidgetEvent>, picked: Boolean) {
        val context = LocalContext.current
        val size = LocalSize.current
        // Small widgets drop the title bar so the list itself gets the room.
        val showTitle = size.height.value >= 150f && size.width.value >= 150f
        Scaffold(
            titleBar = if (showTitle) {
                @Composable {
                    TitleBar(
                        startIcon = ImageProvider(R.drawable.ic_stat_gapwise),
                        title = if (picked) L10n.s(R.string.brand) else L10n.s(R.string.sort_due),
                        iconColor = GlanceTheme.colors.primary,
                        textColor = GlanceTheme.colors.onSurface,
                        actions = {
                            CircleIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_add_24),
                                contentDescription = L10n.s(R.string.new_event),
                                onClick = actionStartActivity(addEventIntent(context)),
                                backgroundColor = GlanceTheme.colors.primary,
                                contentColor = GlanceTheme.colors.onPrimary,
                            )
                        },
                    )
                }
            } else {
                null
            },
            horizontalPadding = 8.dp,
            modifier = GlanceModifier.padding(top = if (showTitle) 0.dp else 8.dp, bottom = 8.dp),
        ) {
            if (rows.isEmpty()) {
                Box(
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity(addEventIntent(context))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        L10n.s(R.string.widget_empty),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center),
                    )
                }
            } else {
                LazyColumn(GlanceModifier.fillMaxSize()) {
                    items(rows, itemId = { it.id }) { row ->
                        // Outer box carries the gap; the row paints its own background.
                        Box(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            EventRowItem(row)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EventRowItem(row: WidgetEvent) {
        Row(
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
                .background(ColorProvider(day = row.dayTint, night = row.nightTint))
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(tapAction(row)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetBadge(row, 36.dp)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    row.name,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp),
                )
                row.due?.let {
                    Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                row.count,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) = GapwiseWidget().updateAll(context)
    }
}

class LogFromWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[EventIdKey] ?: return
        val repo = context.app.repository
        // The widget may not have redrawn since the last tap: check again before logging.
        if (repo.get(id)?.recentActionAgo() != null) {
            runCatching { context.startActivity(ConfirmLogActivity.intent(context, id)) }
            return
        }
        repo.logNow(id) // refreshes every widget itself
    }
}

class GapwiseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GapwiseWidget()
}
