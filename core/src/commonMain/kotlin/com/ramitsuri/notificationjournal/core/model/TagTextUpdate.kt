package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo

data class TagTextUpdate(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "value")
    val value: String,
)
