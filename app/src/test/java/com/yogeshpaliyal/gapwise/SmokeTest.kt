package com.yogeshpaliyal.gapwise

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction as setText
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
    fun milestoneOpensOnTapAndNeverLogs() {
        val app = ApplicationProvider.getApplicationContext<GapwiseApp>()
        val day = 24 * 60 * 60 * 1000L
        val id = runBlocking {
            app.repository.createEvent(
                EventEntity(name = "Wedding", emoji = "💍", color = 0xFFEC4899, category = "People", isMilestone = true),
                firstLogAt = System.currentTimeMillis() - 1000 * day,
            )
        }
        // Opened the way a widget or notification opens it.
        val intent = android.content.Intent(app, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENT_ID, id)
        ActivityScenario.launch<MainActivity>(intent).use {
            waitOrDump("milestone details") { compose.onAllNodesWithText("In total").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("1,000 days today", substring = true).assertExists()
            compose.onNodeWithText("Coming up").performScrollTo().assertExists()
        }
        runBlocking {
            // Taps, widgets and shortcuts all go through logNow, which leaves milestones alone.
            app.repository.logNow(id)
            assertEquals(1, app.repository.get(id)!!.logs.size)
        }
    }

    @Test
    fun milestoneShowsOnHomeAndTapOpensIt() {
        val app = ApplicationProvider.getApplicationContext<GapwiseApp>()
        val day = 24 * 60 * 60 * 1000L
        runBlocking {
            app.repository.createEvent(
                EventEntity(name = "Quit smoking", emoji = "🚭", color = 0xFF22C55E, category = "Health", isMilestone = true),
                firstLogAt = System.currentTimeMillis() - 500 * day,
            )
            app.repository.createEvent(
                EventEntity(name = "Quit control", emoji = "🧪", color = 0xFF22C55E, category = "Health"),
                firstLogAt = System.currentTimeMillis() - 5 * day,
            )
            println("DB: " + app.repository.getAll().map { it.event.name + "/" + it.event.isMilestone })
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("Search").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Quit")
            waitOrDump("milestone tile") { compose.onAllNodesWithText("500 days today").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Quit smoking").performClick()
            waitOrDump("tap opens it") { compose.onAllNodesWithText("In total").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun newMilestoneNeedsADate() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("New event").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Milestone").performClick()
            compose.onNode(hasSetTextAction() and hasText("What do you want to track?")).performTextInput("Moved in")
            compose.onNodeWithText("When was it?").performScrollTo().assertExists()
            compose.onNodeWithText("Add event").performScrollTo().assertIsNotEnabled()
        }
    }

    @Test
    fun newCategoryIsAddedAndSelected() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("New event").performClick()
            compose.waitForIdle()
            compose.onNode(hasText("Or a new category") and setText()).performScrollTo().performTextInput("Garden")
            compose.onNodeWithText("Use").performClick()
            compose.onNodeWithText("Garden").performScrollTo().assertIsOn()
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
            try {
                compose.waitUntil(10_000) { compose.onAllNodesWithText("GapWise").fetchSemanticsNodes().isNotEmpty() }
            } catch (e: Throwable) {
                println(compose.onRoot(useUnmergedTree = true).printToString())
                throw e
            }
        }
    }
}
