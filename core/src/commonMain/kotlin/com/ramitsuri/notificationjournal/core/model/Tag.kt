package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Tags", indices = [Index(value = ["value"], unique = true)])
data class Tag(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "order")
    val order: Int,

    @ColumnInfo(name = "value")
    val value: String,
) {
    companion object {
        val NO_TAG = Tag(
            id = "internal_no_tag",
            order = Int.MIN_VALUE,
            value = "internal_no_tag_value"
        )
    }
}