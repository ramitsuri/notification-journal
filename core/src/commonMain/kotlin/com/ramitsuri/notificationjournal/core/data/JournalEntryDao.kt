package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntryTagUpdate
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntryTextUpdate
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntryTimeUpdate
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntryUploadedUpdate
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

    @Insert
    abstract suspend fun insert(journalEntry: JournalEntry)

    @Update(entity = JournalEntry::class)
    abstract suspend fun updateText(journalEntryUpdate: JournalEntryTextUpdate)

    @Update(entity = JournalEntry::class)
    abstract suspend fun updateTag(journalEntryUpdate: JournalEntryTagUpdate)

    @Update(entity = JournalEntry::class)
    abstract suspend fun updateEntryTime(journalEntryUpdate: JournalEntryTimeUpdate)

    @Transaction
    open suspend fun updateUploaded(entries: List<JournalEntry>) {
        entries
            .map {
                JournalEntryUploadedUpdate(id = it.id, uploaded = true)
            }
            .forEach {
                updateUploaded(it)
            }
    }

    @Update(entity = JournalEntry::class)
    protected abstract suspend fun updateUploaded(update: JournalEntryUploadedUpdate)
}