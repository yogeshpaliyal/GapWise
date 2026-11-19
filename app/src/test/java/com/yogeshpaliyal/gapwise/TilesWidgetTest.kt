package com.yogeshpaliyal.gapwise

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.yogeshpaliyal.gapwise.data.EventEntity
import com.yogeshpaliyal.gapwise.widget.TilesWidgetConfigActivity
import com.yogeshpaliyal.gapwise.widget.desiredColumns
import com.yogeshpaliyal.gapwise.widget.gridLabel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import com.yogeshpaliyal.gapwise.widget.GapwiseTilesWidgetReceiver

@RunWith(RobolectricTestRunner::class)
class TilesWidgetTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun gridShapes() {
        assertEquals(listOf(1, 2, 3, 2, 3, 3, 4, 4, 3, 4), (1..10).map(::desiredColumns))
        assertEquals("1 in a row", gridLabel(1))
        assertEquals("3 in a row", gridLabel(3))
        assertEquals("2 × 2 grid", gridLabel(4))
        assertEquals("3 × 2 grid", gridLabel(6))
    }

    @Test
    fun pickEventsForWidget() {
        val app = ApplicationProvider.getApplicationContext<GapwiseApp>()
        runBlocking {
            app.repository.createEvent(EventEntity(name = "Haircut", emoji = "💇", color = 0xFF3B82F6, category = "Self-care"))
            app.repository.createEvent(EventEntity(name = "Call Mom", emoji = "📞", color = 0xFFEC4899, category = "People"))
        }
        val widgetId = shadowOf(AppWidgetManager.getInstance(app))
            .createWidget(GapwiseTilesWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        val intent = Intent(app, TilesWidgetConfigActivity::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        ActivityScenario.launch<TilesWidgetConfigActivity>(intent).use {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Haircut").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Haircut").performClick()
            compose.onNodeWithText("Call Mom").performClick()
            compose.onNodeWithText("2 picked").assertExists()
            compose.onNodeWithText("2 in a row").assertExists()
            compose.onNodeWithText("Done").performClick()
        }
    }
}
