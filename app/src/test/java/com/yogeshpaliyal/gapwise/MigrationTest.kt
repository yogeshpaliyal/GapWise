package com.yogeshpaliyal.gapwise

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogeshpaliyal.gapwise.data.GapwiseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Opens a database as 0.3.x left it, so an upgrade keeps everyone's events and logs. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @Test
    fun version1To2_keepsDataAndAddsMilestoneFlag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migration-test.db")
        file.parentFile?.mkdirs()
        file.delete()

        // The version 1 schema, exactly as Room created it.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `color` INTEGER NOT NULL, `category` TEXT NOT NULL, `isMultiDay` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `hidden` INTEGER NOT NULL, `paused` INTEGER NOT NULL, `nudgesEnabled` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastNudgedAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER, `note` TEXT, FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_eventId` ON `logs` (`eventId`)")
            db.execSQL("INSERT INTO events (name, emoji, color, category, isMultiDay, pinned, hidden, paused, nudgesEnabled, sortOrder, createdAt, lastNudgedAt) VALUES ('Haircut', '💇', 0, 'Self-care', 0, 1, 0, 0, 1, 1, 0, 0)")
            db.execSQL("INSERT INTO logs (eventId, startAt, endAt, note) VALUES (1, 1000, NULL, 'Short')")
            db.version = 1
        }

        val room = Room.databaseBuilder(context, GapwiseDatabase::class.java, file.name)
            .addMigrations(GapwiseDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val all = runBlocking { room.dao().getAll() }
            assertEquals(1, all.size)
            val item = all.single()
            assertEquals("Haircut", item.event.name)
            assertFalse(item.event.isMilestone)
            assertEquals(listOf(1000L), item.logs.map { it.startAt })
            assertEquals("Short", item.logs.single().note)
        } finally {
            room.close()
            file.delete()
        }
    }
}
