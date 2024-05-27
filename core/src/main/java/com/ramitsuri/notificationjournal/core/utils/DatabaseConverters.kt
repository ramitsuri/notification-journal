package com.ramitsuri.notificationjournal.core.utils

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class DatabaseConverters {
    @TypeConverter
    fun toInstant(millis: Long): Instant {
        return Instant.fromEpochMilliseconds(millis)
    }

    @TypeConverter
    fun fromInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }

    @TypeConverter
    fun toZoneId(zoneIdString: String): TimeZone {
        return TimeZone.of(zoneIdString)
    }

    @TypeConverter
    fun fromZoneId(zoneId: TimeZone): String {
        return zoneId.id
    }
}