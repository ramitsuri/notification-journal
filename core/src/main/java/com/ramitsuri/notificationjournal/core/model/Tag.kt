package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Tags", indices = [Index(value = ["value"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "order")
    val order: Int,

    @ColumnInfo(name = "value")
    val value: String,
) {
    companion object {
        val NO_TAG = Tag(id = Int.MIN_VALUE, order = Int.MIN_VALUE, value = "internal_no_tag_value")
    }
}