package com.ramitsuri.notificationjournal.core.utils

import androidx.room.TypeConverter
import co.touchlab.kermit.Severity
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import kotlinx.datetime.DayOfWeek
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

    @TypeConverter
    fun toDaysOfWeek(value: String): Set<DayOfWeek> =
        value
            .split(",")
            .mapNotNull { token ->
                token
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }
            }.toSet()

    @TypeConverter
    fun fromDaysOfWeek(daysOfWeek: Set<DayOfWeek>): String =
        DayOfWeek.entries
            .filter { it in daysOfWeek }
            .joinToString(",") { it.name }

    @TypeConverter
    fun toReconcileCheckMatchType(value: String): ReconcileCheckMatchType =
        runCatching { ReconcileCheckMatchType.valueOf(value) }.getOrDefault(ReconcileCheckMatchType.ANY_ENTRY)

    @TypeConverter
    fun fromReconcileCheckMatchType(matchType: ReconcileCheckMatchType): String = matchType.name
}
