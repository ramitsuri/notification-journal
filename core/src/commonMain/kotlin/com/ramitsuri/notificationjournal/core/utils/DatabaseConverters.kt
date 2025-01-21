package com.ramitsuri.notificationjournal.core.utils

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime

class DatabaseConverters {
    @TypeConverter
    fun toLocalDateTime(string: String): LocalDateTime {
        return LocalDateTime.parse(string)
    }

    @TypeConverter
    fun fromLocalDateTime(localDateTime: LocalDateTime): String {
        return localDateTime.toString()
    }
}
