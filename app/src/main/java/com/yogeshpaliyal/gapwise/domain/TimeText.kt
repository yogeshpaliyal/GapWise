package com.yogeshpaliyal.gapwise.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Human-friendly time strings. Deliberately calm: nothing is "late", things are just "past usual". */
object TimeText {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM")
    private val timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun daysBetween(fromMs: Long, toMs: Long): Long =
        ChronoUnit.DAYS.between(localDate(fromMs), localDate(toMs))

    /** Big number + unit for tiles, e.g. ("12", "days ago"). */
    fun elapsed(fromMs: Long?, now: Long = System.currentTimeMillis()): Pair<String, String> {
        if (fromMs == null) return "—" to "not logged yet"
        val diff = (now - fromMs).coerceAtLeast(0)
        val days = daysBetween(fromMs, now)
        return when {
            diff < HOUR_MS -> "Now" to "just logged"
            days == 0L -> "${diff / HOUR_MS}h" to "ago, today"
            days == 1L -> "1" to "day ago"
            days < 60 -> "$days" to "days ago"
            days < 730 -> "${(days / 30.4).roundToInt()}" to "months ago"
            else -> "%.1f".format(days / 365.25) to "years ago"
        }
    }

    fun elapsedInline(fromMs: Long?, now: Long = System.currentTimeMillis()): String {
        val (n, u) = elapsed(fromMs, now)
        return if (fromMs == null) u else "$n $u"
    }

    fun dueText(nextMs: Long?, now: Long = System.currentTimeMillis()): String? {
        nextMs ?: return null
        val d = daysBetween(now, nextMs)
        return when {
            d == 0L -> "Usually around today"
            d == 1L -> "Usually due tomorrow"
            d in 2..13 -> "Usually due in $d days"
            d >= 14 -> "Next around ${shortDateFmt.format(localDate(nextMs))}"
            d == -1L -> "1 day past usual"
            else -> "${-d} days past usual"
        }
    }

    fun usualText(days: Double): String {
        val d = days.roundToInt()
        return when {
            d <= 1 -> "Usually every day"
            d < 14 -> "Usually every $d days"
            d < 60 -> "Usually every $d days"
            else -> "Usually every ${(days / 30.4).roundToInt()} months"
        }
    }

    fun date(ms: Long): String = dateFmt.format(localDate(ms))
    fun shortDate(ms: Long): String = shortDateFmt.format(localDate(ms))
    fun time(ms: Long): String = timeFmt.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun dateTime(ms: Long): String = "${date(ms)}, ${time(ms)}"
}
