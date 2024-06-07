package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo

data class TagOrderUpdate(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "order")
    val order: Int,
)
