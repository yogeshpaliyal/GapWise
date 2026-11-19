package com.yogeshpaliyal.gapwise

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.yogeshpaliyal.gapwise.data.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Walks the main flows on the JVM so CI catches crashes before an APK ships:
 * home → + → editor → save → card on home → hold → details.
 */
@RunWith(RobolectricTestRunner::class)
class SmokeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun skipWelcome() {
        ApplicationProvider.getApplicationContext<GapwiseApp>().prefs.welcomeSeen = true
    }

    @Test
    fun addEventFromPlus_thenOpenDetails() {
        // A logged event next to a never-logged one used to crash the home sort.
        val app = ApplicationProvider.getApplicationContext<GapwiseApp>()
        runBlocking {
            app.repository.createEvent(
                EventEntity(name = "Water plants", emoji = "🪴", color = 0xFF4E7D57, category = "Home"),
                firstLogAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,
            )
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("New event").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("New event").assertExists()

            compose.onAllNodes(hasSetTextAction())[1].performTextInput("Haircut")
            compose.onNodeWithText("Add event").performScrollTo().performClick()

            compose.waitUntil(10_000) { compose.onAllNodesWithText("Haircut").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Haircut")[0].performTouchInput { longClick() }

            compose.waitUntil(10_000) { compose.onAllNodesWithText("History").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun welcomeTourAddsEvents() {
        ApplicationProvider.getApplicationContext<GapwiseApp>().prefs.welcomeSeen = false
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Haircut").performClick()
            compose.onNodeWithText("Add 1 event").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Gapwise").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
