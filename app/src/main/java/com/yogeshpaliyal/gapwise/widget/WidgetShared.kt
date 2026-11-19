package com.yogeshpaliyal.gapwise.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.graphics.shapes.toPath
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.domain.HOUR_MS
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.polygon
import com.yogeshpaliyal.gapwise.ui.common.status
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import com.yogeshpaliyal.gapwise.ui.theme.Dark
import com.yogeshpaliyal.gapwise.ui.theme.Light
import kotlin.math.roundToInt

/** The app's own Material 3 scheme, so widgets match the app in light and dark. */
val GapwiseWidgetColors = ColorProviders(light = Light, dark = Dark)

@Composable
fun GapwiseGlanceTheme(content: @Composable () -> Unit) = GlanceTheme(colors = GapwiseWidgetColors, content = content)

private val Rose = Color(0xFFE0569A)
private val Violet = Color(0xFF7B4FC4)

/** Everything a widget needs to draw one event, computed off the UI. */
data class WidgetEvent(
    val id: Long,
    val emoji: String,
    val name: String,
    /** Compact "how long ago": Now, 5h, 12d, 3mo, 1.5y (or "Day 3" while ongoing). */
    val count: String,
    val due: String?,
    val status: GapStatus,
    val dayTint: Color,
    val nightTint: Color,
    val badge: Bitmap,
)

fun compactElapsed(from: Long?, now: Long): String {
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

/** The event's status shape (circle, scallop, sun, burst…) as a bitmap, since widgets can't clip to paths. */
fun badgeBitmap(status: GapStatus, color: Color, px: Int = 120): Bitmap {
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val path = status.polygon().toPath()
    path.transform(Matrix().apply { setScale(px.toFloat(), px.toFloat()) })
    Canvas(bmp).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() })
    return bmp
}

fun EventWithLogs.toWidgetEvent(now: Long): WidgetEvent {
    val ui = EventUi(this, RhythmCalculator.compute(this))
    val status = ui.status(now)
    val c = Color(event.color.toInt())
    val accent = when (status) {
        GapStatus.DUE -> Rose
        GapStatus.PAST -> Violet
        else -> c
    }
    return WidgetEvent(
        id = event.id,
        emoji = event.emoji,
        name = event.name,
        count = if (ui.ongoing) "Day ${TimeText.daysBetween(ui.rhythm.ongoingSince!!, now) + 1}" else compactElapsed(ui.rhythm.lastAt, now),
        due = when {
            event.paused -> "Paused"
            ui.ongoing -> "In progress"
            else -> TimeText.dueText(ui.rhythm.nextExpectedAt, now)
        },
        status = status,
        dayTint = blend(c, Color.White, 0.20f),
        nightTint = blend(c, Color(0xFF241E22), 0.32f),
        badge = badgeBitmap(status, accent.copy(alpha = 0.6f)),
    )
}

/** Status shape with the emoji on top. */
@Composable
fun WidgetBadge(e: WidgetEvent, size: Dp) {
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(e.badge), contentDescription = e.status.label, modifier = GlanceModifier.size(size))
        Text(e.emoji, style = TextStyle(fontSize = (size.value * 0.46f).sp))
    }
}
