package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ramitsuri.notificationjournal.core.log.LogData
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LogDao {
    @Query("SELECT * FROM logs ORDER BY time DESC, id DESC")
    abstract fun getLogsFlow(): Flow<List<LogData>>

    @Query("SELECT DISTINCT tag FROM logs ORDER BY tag ASC")
    abstract fun getTagsFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(logData: LogData)

    @Query("DELETE FROM logs")
    abstract suspend fun deleteAll()

    @Query(
        "DELETE FROM logs WHERE id NOT IN (" +
            "SELECT id FROM logs ORDER BY " +
            "CASE " +
            "WHEN severity IN ('Error', 'Assert', 'Warn') THEN 1 " +
            "WHEN severity = 'Info' THEN 2 " +
            "ELSE 3 " +
            "END ASC, " +
            "time DESC, id DESC " +
            "LIMIT :maxLogs)",
    )
    abstract suspend fun pruneOldLogs(maxLogs: Int)
}
