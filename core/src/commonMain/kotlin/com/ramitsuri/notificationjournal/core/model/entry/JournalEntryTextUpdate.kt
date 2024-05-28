package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo

data class JournalEntryTextUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "text")
    val text: String
)