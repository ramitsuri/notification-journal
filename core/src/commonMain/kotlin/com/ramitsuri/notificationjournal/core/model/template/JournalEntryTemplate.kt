package com.ramitsuri.notificationjournal.core.model.template

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "JournalEntryTemplate")
@Serializable
data class JournalEntryTemplate(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "text")
    @SerialName("text")
    val text: String,

    @ColumnInfo(name = "tag")
    @SerialName("tag")
    val tag: String,
)