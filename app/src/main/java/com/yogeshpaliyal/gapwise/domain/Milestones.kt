package com.yogeshpaliyal.gapwise.domain

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** How long since a milestone's date, and what's coming up. All in calendar days, local time. */
data class MilestoneInfo(
    val since: LocalDate,
    val years: Int,
    /** Months past the last whole year. */
    val months: Int,
    /** Days past the last whole month. */
    val days: Int,
    val totalDays: Long,
    /** The next anniversary, which may be today. */
    val nextAnniversary: LocalDate,
    /** How many years the next anniversary marks. */
    val nextAnniversaryYears: Int,
    val daysToAnniversary: Long,
    /** The next round number of days (100, 500, 1,000, 2,000…), which may be today. */
    val nextRoundDays: Long,
    val nextRoundDate: LocalDate,
    val daysToRound: Long,
) {
    val anniversaryToday: Boolean get() = daysToAnniversary == 0L
    val roundToday: Boolean get() = daysToRound == 0L
}

object Milestones {
    /** 100, 500, then every 1,000 days. */
    fun roundAtOrAfter(days: Long): Long = when {
        days <= 100 -> 100
        days <= 500 -> 500
        else -> ((days + 999) / 1000) * 1000
    }

    /** The anniversary in [year]. A 29 February date is marked on 28 February in other years. */
    fun anniversaryIn(since: LocalDate, year: Int): LocalDate =
        since.withYear(year) // withYear already clamps 29 Feb to 28 Feb

    fun info(since: LocalDate, today: LocalDate): MilestoneInfo {
        val p = if (today.isBefore(since)) Period.ZERO else Period.between(since, today)
        val total = ChronoUnit.DAYS.between(since, today).coerceAtLeast(0)

        var years = (today.year - since.year).coerceAtLeast(1)
        var next = anniversaryIn(since, since.year + years)
        if (next.isBefore(today)) {
            years += 1
            next = anniversaryIn(since, since.year + years)
        }

        val round = roundAtOrAfter(total.coerceAtLeast(1))
        return MilestoneInfo(
            since = since,
            years = p.years,
            months = p.months,
            days = p.days,
            totalDays = total,
            nextAnniversary = next,
            nextAnniversaryYears = years,
            daysToAnniversary = ChronoUnit.DAYS.between(today, next),
            nextRoundDays = round,
            nextRoundDate = since.plusDays(round),
            daysToRound = round - total,
        )
    }
}
