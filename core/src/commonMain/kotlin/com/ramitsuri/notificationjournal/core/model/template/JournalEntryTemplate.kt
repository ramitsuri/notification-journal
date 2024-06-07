package com.ramitsuri.notificationjournal.core.model.template

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "JournalEntryTemplate")
data class JournalEntryTemplate(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "tag")
    val tag: String,
)