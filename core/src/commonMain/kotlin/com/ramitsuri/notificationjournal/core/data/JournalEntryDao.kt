package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
abstract class JournalEntryDao {
    @Query("SELECT * FROM journalentry WHERE uploaded = 0")
    abstract fun getAllFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journalentry WHERE uploaded = 0 ORDER BY entry_time ASC")
    abstract suspend fun getAll(): List<JournalEntry>

    @Query("SELECT * FROM journalentry WHERE id = :id")
    abstract suspend fun get(id: String): JournalEntry

    @Delete
    abstract suspend fun delete(journalEntries: List<JournalEntry>)

    @Transaction
    open suspend fun insert(entry: JournalEntry): JournalEntry {
        insertInternal(entry)
        return get(entry.id)
    }

    @Update
    abstract suspend fun update(journalEntry: JournalEntry)

    @Transaction
    open suspend fun updateUploaded(entries: List<JournalEntry>) {
        entries
            .forEach {
                update(it.copy(uploaded = true))
            }
    }

    @Insert
    protected abstract suspend fun insertInternal(journalEntry: JournalEntry)
}