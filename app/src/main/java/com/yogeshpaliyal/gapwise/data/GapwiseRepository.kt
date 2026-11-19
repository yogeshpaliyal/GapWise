package com.yogeshpaliyal.gapwise.data

import android.content.Context
import com.yogeshpaliyal.gapwise.domain.DAY_MS
import com.yogeshpaliyal.gapwise.shortcuts.ShortcutPublisher
import com.yogeshpaliyal.gapwise.widget.GapwiseWidget
import kotlinx.coroutines.flow.Flow

/** What a tap did, so it can be undone. */
sealed interface LogResult {
    val eventId: Long

    data class Added(override val eventId: Long, val logId: Long) : LogResult
    data class Ended(override val eventId: Long, val logId: Long) : LogResult
}

class GapwiseRepository(
    private val context: Context,
    private val dao: GapwiseDao,
) {
    fun observeAll(): Flow<List<EventWithLogs>> = dao.observeAll()
    fun observe(id: Long): Flow<EventWithLogs?> = dao.observe(id)
    suspend fun getAll(): List<EventWithLogs> = dao.getAll()
    suspend fun get(id: Long): EventWithLogs? = dao.get(id)
    suspend fun hasEvents(): Boolean = dao.countEvents() > 0

    /**
     * One-tap log. For multi-day events the first tap starts a period and the next tap ends it.
     */
    suspend fun logNow(eventId: Long, at: Long = System.currentTimeMillis(), note: String? = null): LogResult? {
        val item = dao.get(eventId) ?: return null
        val open = item.ongoing
        val result = if (open != null) {
            dao.updateLog(open.copy(endAt = at.coerceAtLeast(open.startAt)))
            LogResult.Ended(eventId, open.id)
        } else {
            val id = dao.insertLog(LogEntity(eventId = eventId, startAt = at, note = note?.ifBlank { null }))
            LogResult.Added(eventId, id)
        }
        changed()
        return result
    }

    suspend fun undo(result: LogResult) {
        when (result) {
            is LogResult.Added -> dao.deleteLog(result.logId)
            is LogResult.Ended -> dao.getLog(result.logId)?.let { dao.updateLog(it.copy(endAt = null)) }
        }
        changed()
    }

    suspend fun addLog(eventId: Long, startAt: Long, endAt: Long?, note: String?) {
        dao.insertLog(LogEntity(eventId = eventId, startAt = startAt, endAt = endAt, note = note?.ifBlank { null }))
        changed()
    }

    suspend fun updateLog(log: LogEntity) {
        dao.updateLog(log.copy(note = log.note?.ifBlank { null }))
        changed()
    }

    suspend fun deleteLog(logId: Long) {
        dao.deleteLog(logId)
        changed()
    }

    suspend fun createEvent(event: EventEntity, firstLogAt: Long? = null): Long {
        val id = dao.insertEvent(event.copy(sortOrder = dao.maxSortOrder() + 1))
        if (firstLogAt != null) dao.insertLog(LogEntity(eventId = id, startAt = firstLogAt))
        changed()
        return id
    }

    suspend fun updateEvent(event: EventEntity) {
        dao.updateEvent(event)
        changed()
    }

    suspend fun deleteEvent(event: EventEntity) {
        dao.deleteEvent(event)
        changed()
    }

    suspend fun reorder(idsInOrder: List<Long>) {
        val all = dao.getAll().associateBy { it.event.id }
        val updated = idsInOrder.mapIndexedNotNull { i, id -> all[id]?.event?.copy(sortOrder = i) }
        dao.updateEvents(updated)
        changed()
    }

    suspend fun markNudged(eventId: Long, at: Long) {
        dao.get(eventId)?.let { dao.updateEvent(it.event.copy(lastNudgedAt = at)) }
    }

    /** Adds the chosen starter events, seeding an approximate "last time" for each. */
    suspend fun addSuggestions(chosen: List<Suggestion>) {
        val now = System.currentTimeMillis()
        var order = dao.maxSortOrder()
        chosen.forEachIndexed { i, s ->
            order += 1
            val id = dao.insertEvent(
                EventEntity(
                    name = s.name, emoji = s.emoji, category = s.category,
                    color = Palette.forIndex(order), isMultiDay = s.multiDay, sortOrder = order,
                ),
            )
            val start = now - (s.typicalDays / 2.0 * DAY_MS).toLong()
            val end = if (s.multiDay) start + 2 * DAY_MS else null
            dao.insertLog(LogEntity(eventId = id, startAt = start, endAt = end, note = "Approximate, from welcome tour"))
        }
        changed()
    }

    suspend fun exportJson(): String = BackupCodec.encode(dao.getAll())

    /** Imports are additive: existing events are kept. Returns how many events were added. */
    suspend fun importJson(text: String): Int {
        val file = BackupCodec.decode(text)
        var order = dao.maxSortOrder()
        file.events.forEach { b ->
            order += 1
            val id = dao.insertEvent(
                EventEntity(
                    name = b.name, emoji = b.emoji, color = b.color, category = b.category,
                    isMultiDay = b.isMultiDay, pinned = b.pinned, hidden = b.hidden, paused = b.paused,
                    nudgesEnabled = b.nudgesEnabled, sortOrder = order,
                ),
            )
            dao.insertLogs(b.logs.map { LogEntity(eventId = id, startAt = it.startAt, endAt = it.endAt, note = it.note) })
        }
        changed()
        return file.events.size
    }

    /** Keep the widget and launcher shortcuts in step with the data. */
    private suspend fun changed() {
        runCatching { GapwiseWidget.refresh(context) }
        runCatching { ShortcutPublisher.publish(context, dao.getAll()) }
    }
}
