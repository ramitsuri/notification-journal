package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EntryConflictDao {
    @Query("SELECT * FROM entryconflict")
    abstract fun getAllFlow(): Flow<List<EntryConflict>>

    @Query("DELETE FROM entryconflict WHERE entry_id=:entryId")
    abstract suspend fun deleteForEntryId(entryId: String)

    @Insert
    abstract suspend fun insert(entryConflict: EntryConflict)
}