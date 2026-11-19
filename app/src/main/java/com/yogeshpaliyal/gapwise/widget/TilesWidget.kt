package com.yogeshpaliyal.gapwise.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Comma-separated event ids chosen for one widget, in the order picked. Empty = automatic. */
val TileIdsKey = stringPreferencesKey("event_ids")

const val MAX_TILES = 12

/** How many columns a group of [n] tiles wants: 1, 2, 3, 2×2, 3×2, 4×2, 3×3, then 4 wide. */
fun desiredColumns(n: Int): Int = when (n) {
    0, 1 -> 1
    2 -> 2
    3 -> 3
    4 -> 2
    5, 6 -> 3
    7, 8 -> 4
    9 -> 3
    else -> 4
}

fun gridLabel(n: Int): String {
    if (n == 0) return "Shows the 4 most due events"
    val cols = desiredColumns(n)
    val rows = ceil(n / cols.toDouble()).toInt()
    return if (rows == 1) "$n in a row" else "$cols × $rows grid"
}

class GapwiseTilesWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState<Preferences>(context, id)
        val chosen = prefs[TileIdsKey].orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }
        val all = context.app.repository.getAll()
        val byId = all.associateBy { it.event.id }
        val items: List<EventWithLogs> = if (chosen.isNotEmpty()) {
            chosen.mapNotNull { byId[it] }
        } else {
            all.filter { !it.event.hidden && !it.event.paused }
                .sortedBy { RhythmCalculator.compute(it).nextExpectedAt ?: Long.MAX_VALUE }
                .take(4)
        }
        val now = System.currentTimeMillis()
        val tiles = items.take(MAX_TILES).map { it.toWidgetEvent(now) }
        provideContent { GapwiseGlanceTheme { Content(tiles) } }
    }

    @Composable
    private fun Content(tiles: List<WidgetEvent>) {
        val size = LocalSize.current
        val context = LocalContext.current
        val n = tiles.size
        // Material 3 widget container: system background radius, tiles use the inner radius.
        Scaffold(horizontalPadding = 6.dp, modifier = GlanceModifier.padding(vertical = 6.dp)) {
            if (n == 0) {
                Box(
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity(addEventIntent(context))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Add events in Gapwise",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center),
                    )
                }
                return@Scaffold
            }
            // Fit the grid to the widget's real size: tiles stay at least ~64dp wide
            // and ~48dp tall. If they can't all fit, show as many as do.
            val w = size.width.value - 12f
            val h = size.height.value - 12f
            val maxCols = max(1, (w / 64f).toInt())
            val maxRows = max(1, (h / 48f).toInt())
            var cols = min(desiredColumns(n), maxCols)
            var rows = ceil(n / cols.toDouble()).toInt()
            if (rows > maxRows) {
                cols = min(maxCols, ceil(n / maxRows.toDouble()).toInt())
                rows = min(maxRows, ceil(n / cols.toDouble()).toInt())
            }
            val shown = min(n, rows * cols)
            val tileH: Dp = (h / rows).dp
            Column(GlanceModifier.fillMaxSize()) {
                for (r in 0 until rows) {
                    Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        for (c in 0 until cols) {
                            val i = r * cols + c
                            // Gaps: Glance paints a background under its own padding,
                            // so the spacing lives on an outer box.
                            Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(4.dp)) {
                                if (i < shown) TileView(tiles[i], tileH)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TileView(tile: WidgetEvent, tileH: Dp) {
        val roomy = tileH.value >= 110f
        val tight = tileH.value < 64f
        val badge = (tileH.value * 0.40f).coerceIn(30f, 64f).dp
        Box(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
                .background(ColorProvider(day = tile.dayTint, night = tile.nightTint))
                .clickable(actionRunCallback<LogFromWidget>(actionParametersOf(EventIdKey to tile.id))),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                GlanceModifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (roomy) {
                    WidgetBadge(tile, badge)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        tile.count,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    )
                } else {
                    Text(
                        "${tile.emoji} ${tile.count}",
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = if (tight) 13.sp else 16.sp),
                    )
                }
                if (!tight) {
                    Text(
                        tile.name,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center),
                    )
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = GapwiseTilesWidget().updateAll(context)
    }
}

class GapwiseTilesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GapwiseTilesWidget()
}
