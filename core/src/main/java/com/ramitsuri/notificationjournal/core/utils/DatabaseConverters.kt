package com.ramitsuri.notificationjournal.core.utils

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId

class DatabaseConverters {
    @TypeConverter
    fun toInstant(millis: Long): Instant {
        return Instant.ofEpochMilli(millis)
    }

    @TypeConverter
    fun fromInstant(instant: Instant): Long {
        return instant.toEpochMilli()
    }

    @TypeConverter
    fun toZoneId(zoneIdString: String): ZoneId {
        return ZoneId.of(zoneIdString)
    }

    @TypeConverter
    fun fromZoneId(zoneId: ZoneId): String {
        return zoneId.id
    }
}