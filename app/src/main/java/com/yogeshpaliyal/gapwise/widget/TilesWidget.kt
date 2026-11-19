package com.yogeshpaliyal.gapwise.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
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
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.graphics.shapes.toPath
import com.yogeshpaliyal.gapwise.MainActivity
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.domain.HOUR_MS
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.polygon
import com.yogeshpaliyal.gapwise.ui.common.status
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

private data class Tile(
    val id: Long,
    val emoji: String,
    val name: String,
    val count: String,
    val status: GapStatus,
    val dayColor: Color,
    val nightColor: Color,
    val badge: Bitmap,
)

/** Compact "how long ago": Now, 5h, 12d, 3mo, 1.5y. */
private fun compactElapsed(from: Long?, now: Long): String {
    from ?: return "–"
    val diff = now - from
    val days = TimeText.daysBetween(from, now)
    return when {
        diff < HOUR_MS -> "Now"
        days == 0L -> "${diff / HOUR_MS}h"
        days < 60 -> "${days}d"
        days < 730 -> "${(days / 30.4).roundToInt()}mo"
        else -> "%.1fy".format(days / 365.25)
    }
}

private fun blend(fg: Color, bg: Color, amount: Float): Color = Color(
    red = fg.red * amount + bg.red * (1 - amount),
    green = fg.green * amount + bg.green * (1 - amount),
    blue = fg.blue * amount + bg.blue * (1 - amount),
)

private val Marigold = Color(0xFFE9A93A)
private val Plum = Color(0xFF7A4467)

/** Draws the event's status shape (circle, scallop, sun, burst…) into a small bitmap. */
private fun badgeBitmap(status: GapStatus, color: Color, px: Int = 120): Bitmap {
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val path = status.polygon().toPath()
    path.transform(Matrix().apply { setScale(px.toFloat(), px.toFloat()) })
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
    Canvas(bmp).drawPath(path, paint)
    return bmp
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
        val tiles = items.take(MAX_TILES).map { item ->
            val ui = EventUi(item, RhythmCalculator.compute(item))
            val status = ui.status(now)
            val c = Color(item.event.color.toInt())
            val accent = when (status) {
                GapStatus.DUE -> Marigold
                GapStatus.PAST -> Plum
                else -> c
            }
            Tile(
                id = item.event.id,
                emoji = item.event.emoji,
                name = item.event.name,
                count = if (ui.ongoing) "Day ${TimeText.daysBetween(ui.rhythm.ongoingSince!!, now) + 1}" else compactElapsed(ui.rhythm.lastAt, now),
                status = status,
                dayColor = blend(c, Color.White, 0.18f),
                nightColor = blend(c, Color(0xFF1A2122), 0.30f),
                badge = badgeBitmap(status, accent.copy(alpha = 0.55f)),
            )
        }
        provideContent { GlanceTheme { Content(tiles) } }
    }

    @Composable
    private fun Content(tiles: List<Tile>) {
        val size = LocalSize.current
        val n = tiles.size
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(5.dp),
        ) {
            if (n == 0) {
                Box(
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Add events in Gapwise",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center),
                    )
                }
                return@Column
            }
            // Respect the widget's real width: never squeeze tiles under ~72dp.
            val cols = min(desiredColumns(n), max(1, (size.width.value / 72f).toInt()))
            val rows = ceil(n / cols.toDouble()).toInt()
            val tileH: Dp = (size.height.value / rows).dp
            for (r in 0 until rows) {
                Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    for (c in 0 until cols) {
                        val i = r * cols + c
                        if (i < n) {
                            TileView(tiles[i], tileH, GlanceModifier.defaultWeight().fillMaxHeight().padding(3.dp))
                        } else {
                            Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TileView(tile: Tile, tileH: Dp, modifier: GlanceModifier) {
        val roomy = tileH.value >= 96f
        val badge = (tileH.value * 0.42f).coerceIn(30f, 64f).dp
        Box(
            modifier
                .cornerRadius(18.dp)
                .background(ColorProvider(day = tile.dayColor, night = tile.nightColor))
                .clickable(actionRunCallback<LogFromWidget>(actionParametersOf(EventIdKey to tile.id))),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                GlanceModifier.fillMaxSize().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (roomy) {
                    Box(GlanceModifier.size(badge), contentAlignment = Alignment.Center) {
                        Image(ImageProvider(tile.badge), contentDescription = tile.status.label, modifier = GlanceModifier.size(badge))
                        Text(tile.emoji, style = TextStyle(fontSize = (badge.value * 0.45f).sp))
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        tile.count,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    )
                } else {
                    Text(
                        "${tile.emoji} ${tile.count}",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    )
                }
                Text(
                    tile.name,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center),
                )
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
