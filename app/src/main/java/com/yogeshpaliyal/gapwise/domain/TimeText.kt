package com.yogeshpaliyal.gapwise.domain

import android.text.format.DateFormat
import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** Human-friendly time strings. Deliberately calm: nothing is "late", things are just "past usual". */
object TimeText {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    // Built per call so they follow the current language.
    private val dateFmt get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    private val shortDateFmt
        get() = Locale.getDefault().let { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(it, "dMMM"), it) }
    private val timeFmt get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun daysBetween(fromMs: Long, toMs: Long): Long =
        ChronoUnit.DAYS.between(localDate(fromMs), localDate(toMs))

    /** Big number + unit for tiles, e.g. ("12", "days ago"). */
    fun elapsed(fromMs: Long?, now: Long = System.currentTimeMillis()): Pair<String, String> {
        if (fromMs == null) return "—" to L10n.s(R.string.time_not_logged)
        val diff = (now - fromMs).coerceAtLeast(0)
        val days = daysBetween(fromMs, now)
        return when {
            diff < HOUR_MS -> L10n.s(R.string.time_now) to L10n.s(R.string.time_just_logged)
            days == 0L -> L10n.s(R.string.time_hours_short, diff / HOUR_MS) to L10n.s(R.string.time_ago_today)
            days < 60 -> "$days" to L10n.p(R.plurals.time_days_ago_unit, days.toInt())
            days < 730 -> (days / 30.4).roundToInt().let { "$it" to L10n.p(R.plurals.time_months_ago_unit, it) }
            else -> String.format(Locale.getDefault(), "%.1f", days / 365.25) to L10n.s(R.string.time_years_ago_unit)
        }
    }

    fun elapsedInline(fromMs: Long?, now: Long = System.currentTimeMillis()): String {
        val (n, u) = elapsed(fromMs, now)
        return if (fromMs == null) u else L10n.s(R.string.time_inline, n, u)
    }

    fun dueText(nextMs: Long?, now: Long = System.currentTimeMillis()): String? {
        nextMs ?: return null
        val d = daysBetween(now, nextMs)
        return when {
            d == 0L -> L10n.s(R.string.due_today)
            d == 1L -> L10n.s(R.string.due_tomorrow)
            d in 2..13 -> L10n.p(R.plurals.due_in_days, d.toInt())
            d >= 14 -> L10n.s(R.string.due_around_date, shortDate(nextMs))
            else -> L10n.p(R.plurals.due_past_usual, (-d).toInt())
        }
    }

    fun usualText(days: Double): String {
        val d = days.roundToInt()
        return when {
            d <= 1 -> L10n.s(R.string.usual_every_day)
            d < 60 -> L10n.p(R.plurals.usual_every_days, d)
            else -> L10n.p(R.plurals.usual_every_months, (days / 30.4).roundToInt())
        }
    }

    fun milestone(sinceMs: Long, now: Long = System.currentTimeMillis()): MilestoneInfo =
        Milestones.info(localDate(sinceMs), localDate(now))

    /** Big number + unit for a milestone tile: whole years, else months, else days. */
    fun milestoneElapsed(m: MilestoneInfo): Pair<String, String> = when {
        m.years >= 1 -> "${m.years}" to L10n.p(R.plurals.ms_years_unit, m.years)
        m.years * 12 + m.months >= 1 -> "${m.months}" to L10n.p(R.plurals.ms_months_unit, m.months)
        else -> "${m.totalDays}" to L10n.p(R.plurals.ms_days_unit, m.totalDays.toInt())
    }

    /** "4 years, 2 months, 9 days". */
    fun milestoneBreakdown(m: MilestoneInfo): String = L10n.s(
        R.string.ms_breakdown,
        L10n.p(R.plurals.ms_years, m.years),
        L10n.p(R.plurals.ms_months, m.months),
        L10n.p(R.plurals.ms_days, m.days),
    )

    fun count(n: Long): String = java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(n)

    /** "in 23 days", "tomorrow", "today". */
    fun inDays(d: Long): String = when (d) {
        0L -> L10n.s(R.string.ms_today)
        1L -> L10n.s(R.string.ms_tomorrow)
        else -> L10n.p(R.plurals.ms_in_days, d.toInt(), count(d))
    }

    /** The line under a milestone: a celebration today, else the next anniversary. */
    fun milestoneLine(m: MilestoneInfo): String = when {
        m.anniversaryToday -> L10n.p(R.plurals.ms_years_today, m.nextAnniversaryYears)
        m.roundToday -> L10n.s(R.string.ms_days_today, count(m.nextRoundDays))
        else -> L10n.s(R.string.ms_next_in, L10n.p(R.plurals.ms_years, m.nextAnniversaryYears), inDays(m.daysToAnniversary))
    }

    fun date(d: LocalDate): String = dateFmt.format(d)
    fun date(ms: Long): String = dateFmt.format(localDate(ms))
    fun shortDate(ms: Long): String = shortDateFmt.format(localDate(ms))
    fun time(ms: Long): String = timeFmt.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun dateTime(ms: Long): String = L10n.s(R.string.date_time, date(ms), time(ms))
}
