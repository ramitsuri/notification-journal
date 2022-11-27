package com.ramitsuri.notificationjournal.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ramitsuri.notificationjournal.utils.DatabaseConverters
import java.time.Instant
import java.time.ZoneId

@Database(
    entities = [
        JournalEntry::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
            }
            return INSTANCE as AppDatabase
        }
    }
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journalentry")
    suspend fun getAll(): List<JournalEntry>

    @Query("DELETE FROM journalentry")
    fun deleteAll()

    @Delete
    fun delete(journalEntries: List<JournalEntry>)

    @Insert
    suspend fun insert(journalEntry: JournalEntry)
}

@Entity
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "entry_time") val entryTime: Instant,
    @ColumnInfo(name = "time_zone") val timeZone: ZoneId,
    @ColumnInfo(name = "text") val text: String
)