package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo
import com.squareup.moshi.Json

data class JournalEntryUploadedUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @Json(name = "uploaded")
    val uploaded: Boolean
)
