package com.yogeshpaliyal.gapwise

import com.yogeshpaliyal.gapwise.domain.Milestones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MilestonesTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun breakdownAndTotals() {
        val m = Milestones.info(d("2022-01-15"), d("2026-03-24"))
        assertEquals(4, m.years)
        assertEquals(2, m.months)
        assertEquals(9, m.days)
        assertEquals(1529L, m.totalDays)
    }

    @Test
    fun nextAnniversaryLaterThisYearOrNext() {
        val before = Milestones.info(d("2020-06-10"), d("2026-05-01"))
        assertEquals(d("2026-06-10"), before.nextAnniversary)
        assertEquals(6, before.nextAnniversaryYears)
        assertEquals(40L, before.daysToAnniversary)

        val after = Milestones.info(d("2020-06-10"), d("2026-07-01"))
        assertEquals(d("2027-06-10"), after.nextAnniversary)
        assertEquals(7, after.nextAnniversaryYears)
    }

    @Test
    fun anniversaryToday() {
        val m = Milestones.info(d("2021-11-21"), d("2026-11-21"))
        assertTrue(m.anniversaryToday)
        assertEquals(5, m.nextAnniversaryYears)
    }

    @Test
    fun addedToday_firstAnniversaryIsNextYear() {
        val m = Milestones.info(d("2026-11-21"), d("2026-11-21"))
        assertEquals(0L, m.totalDays)
        assertEquals(1, m.nextAnniversaryYears)
        assertEquals(d("2027-11-21"), m.nextAnniversary)
        assertEquals(100L, m.nextRoundDays)
    }

    @Test
    fun leapDayIsMarkedOn28FebruaryInOtherYears() {
        val m = Milestones.info(d("2020-02-29"), d("2023-02-28"))
        assertTrue(m.anniversaryToday)
        assertEquals(3, m.nextAnniversaryYears)
    }

    @Test
    fun roundNumbers() {
        assertEquals(100L, Milestones.roundAtOrAfter(1))
        assertEquals(100L, Milestones.roundAtOrAfter(100))
        assertEquals(500L, Milestones.roundAtOrAfter(101))
        assertEquals(1000L, Milestones.roundAtOrAfter(501))
        assertEquals(2000L, Milestones.roundAtOrAfter(1001))

        val m = Milestones.info(d("2024-01-01"), d("2024-01-01").plusDays(1000))
        assertTrue(m.roundToday)
        assertEquals(1000L, m.nextRoundDays)
    }
}
