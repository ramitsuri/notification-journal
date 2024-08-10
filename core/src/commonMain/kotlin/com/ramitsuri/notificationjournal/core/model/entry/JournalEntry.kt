package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
data class JournalEntry(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "entry_time")
    @SerialName("entryTime")
    val entryTime: Instant,

    @ColumnInfo(name = "time_zone")
    @SerialName("timeZone")
    val timeZone: TimeZone,

    @ColumnInfo(name = "text")
    @SerialName("text")
    val text: String,

    @ColumnInfo(name = "tag")
    @SerialName("tag")
    val tag: String? = null,

    @ColumnInfo(name = "uploaded", defaultValue = "0")
    @SerialName("uploaded")
    val uploaded: Boolean = false,

    @ColumnInfo(name = "auto_tagged", defaultValue = "0")
    @SerialName("auto_tagged")
    val autoTagged: Boolean = false,

    @ColumnInfo(name = "deleted", defaultValue = "0")
    @SerialName("deleted")
    val deleted: Boolean = false,

    @ColumnInfo(name = "reconciled", defaultValue = "0")
    @SerialName("reconciled")
    val reconciled: Boolean = false,
) {
    fun localDateTime() = entryTime.toLocalDateTime(timeZone)
}