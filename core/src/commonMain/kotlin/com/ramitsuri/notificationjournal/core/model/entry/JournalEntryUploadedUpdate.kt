package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo

data class JournalEntryUploadedUpdate(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "uploaded")
    val uploaded: Boolean
)
