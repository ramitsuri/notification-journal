package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo

data class JournalEntryTagUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "tag")
    val tag: String?
)