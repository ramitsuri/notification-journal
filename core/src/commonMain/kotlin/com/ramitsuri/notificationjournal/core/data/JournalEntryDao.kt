package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
abstract class JournalEntryDao {
    @Query("SELECT * FROM journalentry WHERE reconciled = 0 AND deleted = 0")
    abstract fun getAllFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journalentry WHERE reconciled = 0 AND deleted = 0 ORDER BY entry_time ASC")
    abstract suspend fun getAll(): List<JournalEntry>

    @Query("SELECT * FROM journalentry WHERE uploaded = 0")
    abstract suspend fun getForUpload(): List<JournalEntry>

    @Query("SELECT COUNT(*) FROM journalentry WHERE uploaded = 0")
    abstract fun getForUploadCountFlow(): Flow<Int>

    @Query("SELECT * FROM journalentry WHERE id = :id")
    abstract suspend fun get(id: String): JournalEntry

    @Transaction
    open suspend fun insert(entry: JournalEntry): JournalEntry {
        insertInternal(entry)
        return get(entry.id)
    }

    @Transaction
    open suspend fun update(entry: JournalEntry): JournalEntry {
        updateInternal(entry)
        return get(entry.id)
    }

    @Transaction
    open suspend fun updateUploaded(entries: List<JournalEntry>, uploaded: Boolean) {
        entries
            .forEach {
                update(it.copy(uploaded = uploaded))
            }
    }

    @Upsert
    abstract suspend fun upsert(journalEntry: JournalEntry)

    @Insert
    protected abstract suspend fun insertInternal(journalEntry: JournalEntry)

    @Update
    protected abstract suspend fun updateInternal(journalEntry: JournalEntry)
}