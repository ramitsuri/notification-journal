package com.ramitsuri.notificationjournal.core.data.dictionary

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class DictionaryDao {
    @Query("SELECT * FROM DictionaryItem")
    abstract suspend fun getItems(): List<DictionaryItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(item: DictionaryItem)
}
