package com.ramitsuri.notificationjournal.core.model.entry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ramitsuri.notificationjournal.core.utils.formatForDisplay
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

@Entity
@JsonClass(generateAdapter = true)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @Json(name = "id")
    val id: Int,

    @ColumnInfo(name = "entry_time")
    @Json(name = "entryTime")
    val entryTime: Instant,

    @ColumnInfo(name = "time_zone")
    @Json(name = "timeZone")
    val timeZone: TimeZone,

    @ColumnInfo(name = "text")
    @Json(name = "text")
    val text: String,

    @ColumnInfo(name = "tag")
    @Json(name = "tag")
    val tag: String? = null,

    @ColumnInfo(name = "entry_time_override")
    @Json(name = "entryTimeOverride")
    val entryTimeOverride: Instant? = null,

    @ColumnInfo(name = "uploaded", defaultValue = "0")
    @Json(name = "uploaded")
    val uploaded: Boolean = false,

    @ColumnInfo(name = "auto_tagged", defaultValue = "0")
    @Json(name = "auto_tagged")
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