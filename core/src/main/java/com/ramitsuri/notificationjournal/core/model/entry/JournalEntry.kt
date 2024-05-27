package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ramitsuri.notificationjournal.core.utils.formatForDisplay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: Int,

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

    @ColumnInfo(name = "entry_time_override")
    @SerialName("entryTimeOverride")
    val entryTimeOverride: Instant? = null,

    @ColumnInfo(name = "uploaded", defaultValue = "0")
    @SerialName("uploaded")
    val uploaded: Boolean = false,

    @ColumnInfo(name = "auto_tagged", defaultValue = "0")
    @SerialName("auto_tagged")
    val autoTagged: Boolean = false,
) {
    fun formattedTime(am: String, pm: String): String =
        formatForDisplay(
            toFormat = entryTimeOverride ?: entryTime,
            timeZone = timeZone,
            amString = am,
            pmString = pm,
        )

}