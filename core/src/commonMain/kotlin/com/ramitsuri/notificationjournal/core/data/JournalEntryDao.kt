package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
abstract class JournalEntryDao {
    @Query("SELECT * FROM journalentry WHERE deleted = 0")
    abstract fun getAllFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journalentry WHERE reconciled = 0 AND deleted = 0")
    abstract fun getAllFlowNotReconciled(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journalentry WHERE reconciled = 0 AND deleted = 0 ORDER BY entry_time ASC")
    abstract suspend fun getAll(): List<JournalEntry>

    @Query("SELECT * FROM journalentry WHERE uploaded = 0")
    abstract suspend fun getForUpload(): List<JournalEntry>

    @Query("SELECT COUNT(*) FROM journalentry WHERE uploaded = 0")
    abstract fun getForUploadCountFlow(): Flow<Int>

    @Query("SELECT * FROM journalentry WHERE id = :id")
    abstract suspend fun get(id: String): JournalEntry?

    @Query(
        "SELECT * FROM journalentry WHERE text LIKE '%' || :query || '%' AND tag IN (:tags) " +
            "AND deleted = 0 ORDER BY entry_time DESC",
    )
    abstract suspend fun search(
        query: String,
        tags: List<String>,
    ): List<JournalEntry>

    @Query("SELECT * FROM journalentry WHERE text LIKE '%' || :query || '%' AND deleted = 0 ORDER BY entry_time DESC")
    abstract suspend fun search(query: String): List<JournalEntry>

    @Query("SELECT DISTINCT tag FROM journalentry")
    abstract fun getEntryTags(): Flow<List<String>>

    @Transaction
    open suspend fun insert(entry: JournalEntry) {
        insertInternal(listOf(entry))
    }

    @Transaction
    open suspend fun insert(entries: List<JournalEntry>) {
        insertInternal(entries)
    }

    @Transaction
    open suspend fun clearDaysAndInsert(
        days: List<String>,
        entries: List<JournalEntry>,
    ) {
        days.forEach { day ->
            clearForDateInternal(day)
        }
        insertInternal(entries)
    }

    @Transaction
    open suspend fun update(entries: List<JournalEntry>) {
        updateInternal(entries)
    }

    @Transaction
    open suspend fun updateUploaded(
        entries: List<JournalEntry>,
        uploaded: Boolean,
    ) {
        update(entries.map { it.copy(uploaded = uploaded) })
    }

    //region stats

    @Query(
        "SELECT entry_time from journalentry WHERE uploaded = :uploaded AND reconciled = :reconciled " +
            "ORDER BY entry_time ASC",
    )
    abstract suspend fun getEntryTimes(
        uploaded: Boolean,
        reconciled: Boolean,
    ): List<LocalDateTime>

    @Query("SELECT entry_time from journalentry")
    abstract suspend fun getEntryTimes(): List<LocalDateTime>

    @Query("SELECT COUNT(*) from journalentry WHERE uploaded = :uploaded AND reconciled = :reconciled")
    abstract suspend fun getEntryCount(
        uploaded: Boolean,
        reconciled: Boolean,
    ): Long

    @Query("SELECT COUNT(*) from journalentry")
    abstract suspend fun getEntryCount(): Long

    //endregion

    @Upsert
    abstract suspend fun upsert(journalEntry: JournalEntry)

    @Insert
    protected abstract suspend fun insertInternal(journalEntries: List<JournalEntry>)

    @Update
    protected abstract suspend fun updateInternal(journalEntries: List<JournalEntry>)

    @Query("UPDATE journalentry SET deleted = 1 WHERE entry_time LIKE :date||'%'")
    protected abstract suspend fun clearForDateInternal(date: String)
}
