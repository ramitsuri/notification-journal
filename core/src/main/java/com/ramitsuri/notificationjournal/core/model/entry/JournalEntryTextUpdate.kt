package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo
import com.squareup.moshi.Json

data class JournalEntryTextUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @Json(name = "text")
    val text: String
)