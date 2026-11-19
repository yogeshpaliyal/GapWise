package com.yogeshpaliyal.gapwise.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
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
import androidx.glance.text.TextStyle
import com.yogeshpaliyal.gapwise.MainActivity
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.domain.Rhythm
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import com.yogeshpaliyal.gapwise.domain.TimeText

private data class WidgetRow(val id: Long, val emoji: String, val name: String, val elapsed: String, val due: String?)

private val EventIdKey = ActionParameters.Key<Long>("eventId")

/** "Due soon" list. Tapping a row logs it; tapping the title opens the app. */
class GapwiseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = System.currentTimeMillis()
        val rows = context.app.repository.getAll()
            .filter { !it.event.hidden && !it.event.paused }
            .map { it to RhythmCalculator.compute(it) }
            .sortedWith(
                compareByDescending<Pair<EventWithLogs, Rhythm>> { it.first.event.pinned }
                    .thenBy { it.second.nextExpectedAt ?: Long.MAX_VALUE },
            )
            .take(12)
            .map { (item, r) ->
                WidgetRow(
                    id = item.event.id,
                    emoji = item.event.emoji,
                    name = item.event.name,
                    elapsed = if (item.ongoing != null) "ongoing" else TimeText.elapsedInline(r.lastAt, now),
                    due = TimeText.dueText(r.nextExpectedAt, now),
                )
            }
        provideContent { GlanceTheme { Content(rows) } }
    }

    @Composable
    private fun Content(rows: List<WidgetRow>) {
        Column(
            GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(12.dp),
        ) {
            Text(
                "Gapwise",
                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
            Spacer(GlanceModifier.height(6.dp))
            if (rows.isEmpty()) {
                Text("Add events in the app", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            } else {
                LazyColumn {
                    items(rows, itemId = { it.id }) { row ->
                        Column {
                            Row(
                                GlanceModifier.fillMaxWidth()
                                    .cornerRadius(12.dp)
                                    .background(GlanceTheme.colors.secondaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .clickable(actionRunCallback<LogFromWidget>(actionParametersOf(EventIdKey to row.id))),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(row.emoji, style = TextStyle(fontSize = 18.sp))
                                Spacer(GlanceModifier.width(8.dp))
                                Column(GlanceModifier.defaultWeight()) {
                                    Text(
                                        row.name,
                                        maxLines = 1,
                                        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontWeight = FontWeight.Medium),
                                    )
                                    row.due?.let {
                                        Text(it, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                                    }
                                }
                                Text(row.elapsed, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp))
                            }
                            Spacer(GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = GapwiseWidget().updateAll(context)
    }
}

class LogFromWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[EventIdKey] ?: return
        context.app.repository.logNow(id) // refreshes the widget itself
    }
}

class GapwiseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GapwiseWidget()
}
