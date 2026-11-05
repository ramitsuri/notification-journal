package com.ramitsuri.notificationjournal.core.utils

import androidx.room.TypeConverter
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
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

    @TypeConverter
    fun toVoiceNoteStatus(value: String): VoiceNoteStatus =
        runCatching { VoiceNoteStatus.valueOf(value) }.getOrDefault(VoiceNoteStatus.PROCESSING)

    @TypeConverter
    fun fromVoiceNoteStatus(status: VoiceNoteStatus): String = status.name
}
