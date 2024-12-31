package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "Tags", indices = [Index(value = ["value"], unique = true)])
data class Tag(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "order")
    @SerialName("order")
    val order: Int,

    @ColumnInfo(name = "value")
    @SerialName("value")
    val value: String,
) {
    companion object {
        val NO_TAG = Tag(
            id = "internal_no_tag",
            order = Int.MIN_VALUE,
            value = "internal_no_tag_value"
        )

        fun isNoTag(tag: String) = tag == NO_TAG.value
    }
}