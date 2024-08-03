package com.ramitsuri.notificationjournal.core.model.template

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "JournalEntryTemplate")
@Serializable
data class JournalEntryTemplate(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "text")
    @SerialName("text")
    val text: String,

    @ColumnInfo(name = "display_text")
    @SerialName("display_text")
    val displayText: String,

    @ColumnInfo(name = "short_display_text")
    @SerialName("short_display_text")
    val shortDisplayText: String,

    @ColumnInfo(name = "tag")
    @SerialName("tag")
    val tag: String,

    @Ignore
    val replacesExistingValues: Boolean = true
) {
    constructor(
        id: String,
        text: String,
        displayText: String,
        shortDisplayText: String,
        tag: String
    ) : this(
        id = id,
        text = text,
        displayText = displayText,
        shortDisplayText = shortDisplayText,
        tag = tag,
        replacesExistingValues = true,
    )
}
