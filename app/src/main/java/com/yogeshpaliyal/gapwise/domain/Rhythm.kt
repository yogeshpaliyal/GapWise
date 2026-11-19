package com.yogeshpaliyal.gapwise.domain

import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.data.LogEntity
import kotlin.math.roundToLong

const val DAY_MS = 24L * 60 * 60 * 1000
const val HOUR_MS = 60L * 60 * 1000

/** One gap between two consecutive logs. */
data class Gap(
    val endedAt: Long,
    val days: Double,
    /** A one-off long gap (a trip, an illness) that is ignored when learning the rhythm. */
    val outlier: Boolean,
)

data class Rhythm(
    val logCount: Int,
    val lastAt: Long?,
    val gaps: List<Gap>,
    /** Typical number of days between logs. Null until there is enough history. */
    val usualDays: Double?,
    /** When we expect the next log, at roughly the usual time of day. */
    val nextExpectedAt: Long?,
    /** Multi-day only: typical length in days. */
    val usualLengthDays: Double?,
    /** Multi-day only: start of the currently open period. */
    val ongoingSince: Long?,
) {
    val hasRhythm: Boolean get() = usualDays != null

    /** 0 right after logging, 1 when it's usually due. Can go past 1. */
    fun progress(now: Long): Float {
        val last = lastAt ?: return 0f
        val usual = usualDays ?: return 0f
        return ((now - last).toDouble() / (usual * DAY_MS)).toFloat().coerceIn(0f, 1.5f)
    }
}

object RhythmCalculator {
    /** Minimum logs before we try to guess a rhythm. */
    private const val MIN_LOGS = 3

    /** Only the most recent gaps count, so the rhythm follows how you live now. */
    private const val RECENT_GAPS = 8

    /** A gap this many times longer than the median is treated as a one-off. */
    private const val OUTLIER_FACTOR = 2.5

    fun compute(item: EventWithLogs): Rhythm = compute(item.logs, item.event.isMultiDay)

    fun compute(logs: List<LogEntity>, multiDay: Boolean): Rhythm {
        val sorted = logs.sortedBy { it.startAt }
        val rawGaps = sorted.zipWithNext { a, b -> (b.startAt - a.startAt).toDouble() / DAY_MS to b.startAt }
        val median = median(rawGaps.map { it.first })
        val gaps = rawGaps.map { (days, at) ->
            val outlier = median != null && rawGaps.size >= 3 && days > median * OUTLIER_FACTOR
            Gap(endedAt = at, days = days, outlier = outlier)
        }

        val usable = gaps.filterNot { it.outlier }.map { it.days }.takeLast(RECENT_GAPS)
        val usual = if (sorted.size >= MIN_LOGS && usable.size >= 2) median(usable) else null

        val last = sorted.lastOrNull()
        val ongoing = if (multiDay) last?.takeIf { it.endAt == null } else null

        val next = if (usual != null && last != null && ongoing == null) {
            val base = last.startAt + (usual * DAY_MS).roundToLong()
            alignToUsualTimeOfDay(base, sorted)
        } else {
            null
        }

        val lengths = if (multiDay) {
            sorted.mapNotNull { l -> l.endAt?.let { (it - l.startAt).toDouble() / DAY_MS + 1.0 } }
        } else {
            emptyList()
        }

        return Rhythm(
            logCount = sorted.size,
            lastAt = last?.startAt,
            gaps = gaps,
            usualDays = usual,
            nextExpectedAt = next,
            usualLengthDays = if (lengths.size >= 2) median(lengths) else null,
            ongoingSince = ongoing?.startAt,
        )
    }

    /** Move the expected moment to the time of day you usually log it. */
    private fun alignToUsualTimeOfDay(expected: Long, logs: List<LogEntity>): Long {
        val zone = java.time.ZoneId.systemDefault()
        val minutes = logs.takeLast(RECENT_GAPS).map {
            val t = java.time.Instant.ofEpochMilli(it.startAt).atZone(zone).toLocalTime()
            (t.hour * 60 + t.minute).toDouble()
        }
        val usualMinute = median(minutes)?.toLong() ?: return expected
        val date = java.time.Instant.ofEpochMilli(expected).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).plusMinutes(usualMinute).toInstant().toEpochMilli()
    }

    fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
    }
}
