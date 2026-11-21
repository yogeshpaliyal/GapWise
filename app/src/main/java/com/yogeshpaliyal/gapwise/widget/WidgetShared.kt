package com.yogeshpaliyal.gapwise.widget

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

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
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import com.yogeshpaliyal.gapwise.ui.common.ConfirmLogActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
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
import android.content.Intent
import com.yogeshpaliyal.gapwise.MainActivity
import com.yogeshpaliyal.gapwise.domain.HOUR_MS
import com.yogeshpaliyal.gapwise.domain.MilestoneInfo
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
    /** Tapped within the last minute: the next tap asks before logging again. */
    val recent: Boolean,
    /** Milestones have nothing to log, so a tap opens them. */
    val milestone: Boolean = false,
)

fun compactElapsed(from: Long?, now: Long): String {
    from ?: return "–"
    val diff = now - from
    val days = TimeText.daysBetween(from, now)
    return when {
        diff < HOUR_MS -> L10n.s(R.string.time_now)
        days == 0L -> L10n.s(R.string.time_hours_short, (diff / HOUR_MS).toInt())
        days < 60 -> L10n.s(R.string.days_short, days.toInt())
        days < 730 -> L10n.s(R.string.months_short, (days / 30.4).roundToInt())
        else -> L10n.s(R.string.years_short, String.format(java.util.Locale.getDefault(), "%.1f", days / 365.25))
    }
}

/** Compact milestone count: 4y, 7mo, 23d. */
fun milestoneCount(m: MilestoneInfo): String = when {
    m.years >= 1 -> L10n.s(R.string.years_short, m.years.toString())
    m.months >= 1 -> L10n.s(R.string.months_short, m.months)
    else -> L10n.s(R.string.days_short, m.totalDays.toInt())
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
    val m = ui.milestone(now)
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
        count = if (m != null) milestoneCount(m) else if (ui.ongoing) L10n.s(R.string.day_n, (TimeText.daysBetween(ui.rhythm.ongoingSince!!, now) + 1).toInt()) else compactElapsed(ui.rhythm.lastAt, now),
        due = when {
            m != null -> TimeText.milestoneLine(m)
            event.paused -> L10n.s(R.string.status_paused)
            ui.ongoing -> L10n.s(R.string.status_ongoing)
            else -> TimeText.dueText(ui.rhythm.nextExpectedAt, now)
        },
        status = status,
        dayTint = blend(c, Color.White, 0.20f),
        nightTint = blend(c, Color(0xFF241E22), 0.32f),
        badge = badgeBitmap(status, accent.copy(alpha = 0.6f)),
        recent = recentActionAgo(now) != null,
        milestone = m != null,
    )
}

/**
 * What tapping an event in a widget does: log it straight away, or, if it was tapped
 * within the last minute, open the "Log it again?" confirmation.
 */
@Composable
fun tapAction(e: WidgetEvent): Action {
    val context = LocalContext.current
    return if (e.milestone) {
        actionStartActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_EVENT_ID, e.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    } else if (e.recent) {
        actionStartActivity(ConfirmLogActivity.intent(context, e.id))
    } else {
        actionRunCallback<LogFromWidget>(actionParametersOf(EventIdKey to e.id))
    }
}

/** Every 20 seconds while a widget is showing, so "ago" times and the one-minute window stay current. */
val widgetTicker = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(20_000)
    }
}

/** Status shape with the emoji on top. */
@Composable
fun WidgetBadge(e: WidgetEvent, size: Dp) {
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(e.badge), contentDescription = e.status.label, modifier = GlanceModifier.size(size))
        Text(e.emoji, style = TextStyle(fontSize = (size.value * 0.46f).sp))
    }
}
