package com.yogeshpaliyal.gapwise.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = "gapwise",
    val version: Int = 1,
    val exportedAt: Long,
    val events: List<BackupEvent>,
)

@Serializable
data class BackupEvent(
    val name: String,
    val emoji: String,
    val color: Long,
    val category: String,
    val isMultiDay: Boolean = false,
    val pinned: Boolean = false,
    val hidden: Boolean = false,
    val paused: Boolean = false,
    val nudgesEnabled: Boolean = true,
    val logs: List<BackupLog> = emptyList(),
)

@Serializable
data class BackupLog(
    val startAt: Long,
    val endAt: Long? = null,
    val note: String? = null,
)

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(items: List<EventWithLogs>): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            exportedAt = System.currentTimeMillis(),
            events = items.map { item ->
                val e = item.event
                BackupEvent(
                    name = e.name, emoji = e.emoji, color = e.color, category = e.category,
                    isMultiDay = e.isMultiDay, pinned = e.pinned, hidden = e.hidden,
                    paused = e.paused, nudgesEnabled = e.nudgesEnabled,
                    logs = item.sortedLogs.map { BackupLog(it.startAt, it.endAt, it.note) },
                )
            },
        ),
    )

    fun decode(text: String): BackupFile = json.decodeFromString(BackupFile.serializer(), text)
}
