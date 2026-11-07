package com.ramitsuri.notificationjournal.core.utils

import androidx.room.TypeConverter
import co.touchlab.kermit.Severity
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant

class DatabaseConverters {
    @TypeConverter
    fun toLocalDateTime(string: String): LocalDateTime = LocalDateTime.parse(string)

    @TypeConverter
    fun fromLocalDateTime(localDateTime: LocalDateTime): String = localDateTime.toString()

    @TypeConverter
    fun toVoiceNoteStatus(value: String): VoiceNoteStatus =
        runCatching { VoiceNoteStatus.valueOf(value) }.getOrDefault(VoiceNoteStatus.PROCESSING)

    @TypeConverter
    fun fromVoiceNoteStatus(status: VoiceNoteStatus): String = status.name

    @TypeConverter
    fun toInstant(string: String): Instant = Instant.parse(string)

    @TypeConverter
    fun fromInstant(instant: Instant): String = instant.toString()

    @TypeConverter
    fun toSeverity(value: String): Severity = runCatching { Severity.valueOf(value) }.getOrDefault(Severity.Info)

    @TypeConverter
    fun fromSeverity(severity: Severity): String = severity.name
}
