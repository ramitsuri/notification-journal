package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo

data class TagTextUpdate(
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "value")
    val value: String,
)
