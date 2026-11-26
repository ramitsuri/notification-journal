package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ramitsuri.notificationjournal.core.model.ReconcileCheck
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReconcileCheckDao {
    @Query("SELECT * FROM reconcile_checks ORDER BY tag ASC, id ASC")
    abstract fun getAllFlow(): Flow<List<ReconcileCheck>>

    @Query("SELECT * FROM reconcile_checks WHERE enabled = 1 ORDER BY tag ASC, id ASC")
    abstract suspend fun getEnabled(): List<ReconcileCheck>

    @Upsert
    abstract suspend fun upsert(check: ReconcileCheck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(check: ReconcileCheck)

    @Update
    abstract suspend fun update(check: ReconcileCheck)

    @Delete
    abstract suspend fun delete(check: ReconcileCheck)
}
