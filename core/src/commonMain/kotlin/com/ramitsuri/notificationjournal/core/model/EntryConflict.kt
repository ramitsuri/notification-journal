package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity
data class EntryConflict(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "entry_id")
    @SerialName("entry_id")
    val entryId: String,
    @ColumnInfo(name = "entry_time")
    @SerialName("entry_time")
    val entryTime: LocalDateTime,
    @ColumnInfo(name = "text")
    @SerialName("text")
    val text: String,
    @ColumnInfo(name = "tag")
    @SerialName("tag")
    val tag: String,
    @ColumnInfo(name = "sender_name")
    @SerialName("sender_name")
    val senderName: String,
)
