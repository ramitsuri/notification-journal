package com.ramitsuri.notificationjournal.core.data

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
import androidx.room.Update
import com.ramitsuri.notificationjournal.core.utils.DatabaseConverters
import kotlinx.coroutines.flow.Flow
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

        private fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
            }
            return INSTANCE as AppDatabase
        }

        fun getDao(context: Context) = getInstance(context).journalEntryDao()
    }
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journalentry")
    fun getAllFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journalentry ORDER BY entry_time ASC")
    suspend fun getAll(): List<JournalEntry>

    @Query("DELETE FROM journalentry")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(journalEntries: List<JournalEntry>)

    @Insert
    suspend fun insert(journalEntry: JournalEntry)

    @Update(entity = JournalEntry::class)
    suspend fun update(journalEntryUpdate: JournalEntryUpdate)
}

@Entity
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "entry_time") val entryTime: Instant,
    @ColumnInfo(name = "time_zone") val timeZone: ZoneId,
    @ColumnInfo(name = "text") val text: String
)

data class JournalEntryUpdate(
    val id: Int,
    val text: String
)