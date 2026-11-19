package com.yogeshpaliyal.gapwise.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.appwidget.components.SquareIconButton
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = System.currentTimeMillis()
        val rows = context.app.repository.getAll()
            .filter { !it.event.hidden && !it.event.paused }
            .sortedWith(
                compareByDescending<EventWithLogs> { it.event.pinned }
                    .thenBy { RhythmCalculator.compute(it).nextExpectedAt ?: Long.MAX_VALUE },
            )
            .take(12)
            .map { it.toWidgetEvent(now) }
        provideContent { GapwiseGlanceTheme { Content(rows) } }
    }

    @Composable
    private fun Content(rows: List<WidgetEvent>) {
        val context = LocalContext.current
        Scaffold(
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(R.drawable.ic_stat_gapwise),
                    title = "Due soon",
                    iconColor = GlanceTheme.colors.primary,
                    textColor = GlanceTheme.colors.onSurface,
                    actions = {
                        SquareIconButton(
                            imageProvider = ImageProvider(R.drawable.ic_add_24),
                            contentDescription = "New event",
                            onClick = actionStartActivity(addEventIntent(context)),
                        )
                    },
                )
            },
            horizontalPadding = 10.dp,
        ) {
            if (rows.isEmpty()) {
                Box(
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity(addEventIntent(context))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Nothing tracked yet. Tap + to add your first event.",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center),
                    )
                }
            } else {
                LazyColumn(GlanceModifier.fillMaxSize()) {
                    items(rows, itemId = { it.id }) { row ->
                        Column {
                            EventRowItem(row)
                            Spacer(GlanceModifier.height(6.dp))
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
                .clickable(actionRunCallback<LogFromWidget>(actionParametersOf(EventIdKey to row.id))),
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
        context.app.repository.logNow(id) // refreshes every widget itself
    }
}

class GapwiseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GapwiseWidget()
}
