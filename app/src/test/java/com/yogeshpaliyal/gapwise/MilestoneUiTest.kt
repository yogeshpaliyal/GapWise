package com.yogeshpaliyal.gapwise

import android.os.Looper
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** New milestone from the editor through to its details, in a JVM of its own (see build.gradle.kts). */
@RunWith(RobolectricTestRunner::class)
class MilestoneUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun skipWelcome() {
        ApplicationProvider.getApplicationContext<GapwiseApp>().prefs.welcomeSeen = true
    }

    private fun waitOrDump(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(10_000, condition)
        } catch (e: Throwable) {
            throw AssertionError("Timed out waiting for: $what\n" + compose.onRoot().printToString(maxDepth = 30), e)
        }
    }

    @Test
    fun newMilestoneNeedsADate_thenOpensFromHome() {
        val app = ApplicationProvider.getApplicationContext<GapwiseApp>()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription("New event").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Milestone").performClick()
            compose.onNode(hasSetTextAction() and hasText("What do you want to track?")).performTextInput("Moved in")
            compose.onNodeWithText("When was it?").performScrollTo().assertExists()
            compose.onNodeWithText("Add event").performScrollTo().assertIsNotEnabled()

            // Pick today, save, and the tile on home opens it instead of logging.
            compose.onNodeWithText("Pick a date").performScrollTo().performClick()
            compose.onNodeWithText("OK").performClick()
            compose.onNodeWithText("Add event").performScrollTo().assertIsEnabled().performClick()
            waitOrDump("saved") {
                shadowOf(Looper.getMainLooper()).idle()
                runBlocking { app.repository.getAll().any { it.event.name == "Moved in" && it.event.isMilestone && it.logs.size == 1 } }
            }
            // The tile, not the editor's name field: its line counts down to the first anniversary.
            waitOrDump("milestone on home") { compose.onAllNodesWithText("1 year in ", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Moved in").performClick()
            waitOrDump("milestone details") { compose.onAllNodesWithText("In total").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Coming up").performScrollTo().assertExists()
        }
    }

}
