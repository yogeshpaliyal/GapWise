package com.yogeshpaliyal.gapwise.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    /** ARGB color, stored as Long so it survives JSON round-trips. */
    val color: Long,
    val category: String,
    /** Multi-day events (periods, trips, sick days) are logged with a start and an end. */
    val isMultiDay: Boolean = false,
    val pinned: Boolean = false,
    val hidden: Boolean = false,
    val paused: Boolean = false,
    val nudgesEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** When we last sent a nudge, so we nudge at most once per cycle. */
    val lastNudgedAt: Long = 0,
)

@Entity(
    tableName = "logs",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId")],
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val startAt: Long,
    /** Only used by multi-day events. Null while the event is still ongoing. */
    val endAt: Long? = null,
    val note: String? = null,
)

data class EventWithLogs(
    @Embedded val event: EventEntity,
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val logs: List<LogEntity>,
) {
    val sortedLogs: List<LogEntity> get() = logs.sortedBy { it.startAt }
    val lastLog: LogEntity? get() = logs.maxByOrNull { it.startAt }
    val ongoing: LogEntity? get() = if (event.isMultiDay) lastLog?.takeIf { it.endAt == null } else null

    /** The last time a tap changed this event: a log, or the end of a multi-day period. */
    val lastActionAt: Long? get() = lastLog?.let { maxOf(it.startAt, it.endAt ?: 0L) }

    /**
     * Milliseconds since the last tap, if it was within [REPEAT_WINDOW_MS]. A second tap that
     * soon is more likely a mistake than intent, so the app and widgets ask first.
     */
    fun recentActionAgo(now: Long = System.currentTimeMillis()): Long? =
        lastActionAt?.let { now - it }?.takeIf { it in 0 until REPEAT_WINDOW_MS }
}

/** A repeat tap within this window asks for confirmation. */
const val REPEAT_WINDOW_MS = 60_000L

/** "12 seconds ago" for the confirmation prompt. */
fun secondsAgoText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(1)
    return com.yogeshpaliyal.gapwise.L10n.p(com.yogeshpaliyal.gapwise.R.plurals.seconds_ago, s.toInt())
}
