package com.ramitsuri.notificationjournal.core.data.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "DictionaryItem",
    indices = [Index(value = ["word"], unique = true, name = "index_dictionary_word")],
)
data class DictionaryItem(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "word")
    val word: String,
)
