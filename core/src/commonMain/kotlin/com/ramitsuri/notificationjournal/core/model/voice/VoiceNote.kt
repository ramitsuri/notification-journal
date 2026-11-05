package com.ramitsuri.notificationjournal.core.model.voice

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class VoiceNoteStatus {
    PROCESSING,
    FAILED,
    SUCCESS,
}

@Serializable
@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "filePath")
    @SerialName("filePath")
    val filePath: String,
    @ColumnInfo(name = "recordedAt")
    @SerialName("recordedAt")
    val recordedAt: LocalDateTime,
    @ColumnInfo(name = "segmentIndex")
    @SerialName("segmentIndex")
    val segmentIndex: Int = 0,
    @ColumnInfo(name = "status")
    @SerialName("status")
    val status: VoiceNoteStatus = VoiceNoteStatus.PROCESSING,
    @ColumnInfo(name = "errorMessage")
    @SerialName("errorMessage")
    val errorMessage: String? = null,
)
