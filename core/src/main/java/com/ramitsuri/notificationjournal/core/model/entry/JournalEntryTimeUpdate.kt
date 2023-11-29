package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo
import java.time.Instant

data class JournalEntryTimeUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "entry_time_override")
    val entryTimeOverride: Instant?,
)