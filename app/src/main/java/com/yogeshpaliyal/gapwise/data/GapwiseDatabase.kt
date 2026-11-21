package com.yogeshpaliyal.gapwise.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface GapwiseDao {
    @Transaction
    @Query("SELECT * FROM events ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<EventWithLogs>>

    @Transaction
    @Query("SELECT * FROM events ORDER BY sortOrder, id")
    suspend fun getAll(): List<EventWithLogs>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    fun observe(id: Long): Flow<EventWithLogs?>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventWithLogs?

    @Query("SELECT COUNT(*) FROM events")
    suspend fun countEvents(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM events")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Update
    suspend fun updateEvents(events: List<EventEntity>)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Insert
    suspend fun insertLog(log: LogEntity): Long

    @Insert
    suspend fun insertLogs(logs: List<LogEntity>)

    @Update
    suspend fun updateLog(log: LogEntity)

    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLog(id: Long): LogEntity?
}

@Database(entities = [EventEntity::class, LogEntity::class], version = 2, exportSchema = true)
abstract class GapwiseDatabase : RoomDatabase() {
    abstract fun dao(): GapwiseDao

    companion object {
        fun create(context: Context): GapwiseDatabase =
            Room.databaseBuilder(context, GapwiseDatabase::class.java, "gapwise.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** 2: milestones, events counted from a fixed date. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN isMilestone INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
