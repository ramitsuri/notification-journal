package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EntryConflictDao {
    @Query("SELECT * FROM entryconflict")
    abstract fun getFlow(): Flow<List<EntryConflict>>

    @Query("SELECT COUNT(DISTINCT entry_id) FROM entryconflict WHERE entry_id IN (:entryIds)")
    abstract suspend fun getCount(entryIds: List<String>): Int

    @Query("DELETE FROM entryconflict WHERE entry_id=:entryId")
    abstract suspend fun deleteForEntryId(entryId: String)

    @Insert
    abstract suspend fun insert(entryConflict: EntryConflict)

    @Query("DELETE FROM entryconflict")
    abstract suspend fun deleteAll()
}
