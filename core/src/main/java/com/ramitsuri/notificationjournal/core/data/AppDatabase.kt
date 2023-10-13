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
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom1To2
import com.ramitsuri.notificationjournal.core.utils.DatabaseConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

@Database(
    entities = [
        JournalEntry::class
    ],
    version = 2,
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
                INSTANCE = Room
                    .databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "app_database"
                    )
                    .addMigrations(MigrationFrom1To2())
                    .build()
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
    suspend fun updateText(journalEntryUpdate: JournalEntryTextUpdate)

    @Update(entity = JournalEntry::class)
    suspend fun updateTag(journalEntryUpdate: JournalEntryTagUpdate)

    @Update(entity = JournalEntry::class)
    suspend fun updateEntryTime(journalEntryUpdate: JournalEntryTimeUpdate)
}

@Entity
@JsonClass(generateAdapter = true)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @Json(name = "id")
    val id: Int,

    @ColumnInfo(name = "entry_time")
    @Json(name = "entryTime")
    val entryTime: Instant,

    @ColumnInfo(name = "time_zone")
    @Json(name = "timeZone")
    val timeZone: ZoneId,

    @ColumnInfo(name = "text")
    @Json(name = "text")
    val text: String,

    @ColumnInfo(name = "tag")
    @Json(name = "tag")
    val tag: String? = null,

    @ColumnInfo(name = "entry_time_override")
    @Json(name = "entryTimeOverride")
    val entryTimeOverride: Instant? = null,
)

data class JournalEntryTextUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @Json(name = "text")
    val text: String
)

data class JournalEntryTagUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "tag")
    val tag: String?
)

data class JournalEntryTimeUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "entry_time_override")
    val entryTimeOverride: Instant?,
)