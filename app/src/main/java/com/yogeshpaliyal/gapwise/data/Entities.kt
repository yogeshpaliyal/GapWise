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
}
