package com.yogeshpaliyal.gapwise

import com.yogeshpaliyal.gapwise.data.EventEntity
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.data.LogEntity
import com.yogeshpaliyal.gapwise.data.REPEAT_WINDOW_MS
import com.yogeshpaliyal.gapwise.ui.common.repeatPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatGuardTest {
    private val now = 1_000_000_000L
    private fun event(multiDay: Boolean = false, vararg logs: LogEntity) =
        EventWithLogs(EventEntity(id = 1, name = "Haircut", emoji = "💇", color = 0, category = "x", isMultiDay = multiDay), logs.toList())

    @Test
    fun noLogs_noPrompt() = assertNull(event().repeatPrompt(now))

    @Test
    fun loggedSecondsAgo_prompts() {
        val p = event(false, LogEntity(id = 1, eventId = 1, startAt = now - 20_000)).repeatPrompt(now)
        assertNotNull(p)
        assertEquals(20_000L, p!!.agoMs)
        assertEquals(false, p.wouldEnd)
    }

    @Test
    fun loggedOverAMinuteAgo_noPrompt() =
        assertNull(event(false, LogEntity(id = 1, eventId = 1, startAt = now - REPEAT_WINDOW_MS - 1)).repeatPrompt(now))

    @Test
    fun multiDayStartedJustNow_promptsToEnd() {
        val p = event(true, LogEntity(id = 1, eventId = 1, startAt = now - 5_000, endAt = null)).repeatPrompt(now)
        assertTrue(p!!.wouldEnd)
    }

    @Test
    fun multiDayEndedJustNow_promptsBeforeStartingAgain() {
        val p = event(true, LogEntity(id = 1, eventId = 1, startAt = now - 3 * 86_400_000L, endAt = now - 10_000)).repeatPrompt(now)
        assertEquals(false, p!!.wouldEnd)
    }
}
