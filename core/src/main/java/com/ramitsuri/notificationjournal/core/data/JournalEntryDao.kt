package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.model.JournalEntryTagUpdate
import com.ramitsuri.notificationjournal.core.model.JournalEntryTextUpdate
import com.ramitsuri.notificationjournal.core.model.JournalEntryTimeUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journalentry")
    fun getAllFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journalentry ORDER BY entry_time ASC")
    suspend fun getAll(): List<JournalEntry>

    @Query("SELECT * FROM journalentry WHERE id = :id")
    suspend fun get(id: Int): JournalEntry

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