package com.yogeshpaliyal.gapwise

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
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
import android.os.Looper
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

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
        val day = 24 * 60 * 60 * 1000L
        runBlocking {
            val id = app.repository.createEvent(
                EventEntity(name = "Water plants", emoji = "🪴", color = 0xFF4E7D57, category = "Home"),
                firstLogAt = System.currentTimeMillis() - 9 * day,
            )
            app.repository.addLog(id, System.currentTimeMillis() - 3 * day, null, "Moved the fern")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("New event").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("New event").assertExists()

            compose.onNode(hasSetTextAction() and hasText("What do you want to track?")).performTextInput("Haircut")
            compose.waitForIdle()
            compose.onNodeWithText("Add event").performScrollTo().assertIsEnabled().performClick()

            // Back on home once the editor is gone and the new card is there.
            waitOrDump("editor closes") { compose.onAllNodesWithText("New event").fetchSemanticsNodes().isEmpty() }
            waitOrDump("event saved") {
                shadowOf(Looper.getMainLooper()).idle()
                runBlocking { app.repository.getAll().any { it.event.name == "Haircut" } }
            }

            // Two taps on the same card within a minute: the second one asks first.
            compose.onAllNodesWithText("Water plants")[0].performClick()
            waitOrDump("first tap logged") { compose.onAllNodesWithText("Logged Water plants").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Water plants")[0].performClick()
            waitOrDump("repeat confirmation") { compose.onAllNodesWithText("Log Water plants again?").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Cancel").performClick()
            compose.waitForIdle()

            // Details + history timeline for an event with two logs.
            compose.onAllNodesWithText("Water plants")[0].performTouchInput { longClick() }

            waitOrDump("details open") { compose.onAllNodesWithText("Log a past day").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Moved the fern"))
            compose.onNodeWithText("Moved the fern").assertExists()

            // Tapping an entry opens the edit sheet.
            compose.onNodeWithText("Moved the fern").performClick()
            waitOrDump("edit sheet") { compose.onAllNodesWithText("Edit log").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Save").assertExists()
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("6 days later", substring = true))
        }
    }

    /** Waits, and on timeout fails with the current screen's semantics tree. */
    private fun waitOrDump(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(10_000, condition)
        } catch (e: Throwable) {
            throw AssertionError("Timed out waiting for: $what\n" + compose.onRoot().printToString(maxDepth = 30), e)
        }
    }

    @Test
    fun welcomeTourAddsEvents() {
        ApplicationProvider.getApplicationContext<GapwiseApp>().prefs.welcomeSeen = false
        ActivityScenario.launch(MainActivity::class.java).use {
            // Intro pages, then the starter picker.
            compose.onNodeWithText("Remember the\nlast time").assertExists()
            compose.onNodeWithText("Skip").performClick()
            compose.onNodeWithText("Water plants").performClick()
            // Asks when it was last done.
            compose.onNodeWithText("When did you last do it?").assertExists()
            compose.onNodeWithText("Yesterday").performClick()
            compose.onNodeWithText("Add").performClick()
            compose.onNodeWithText("Add 1 event").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("GapWise").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
