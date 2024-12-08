package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TagGroup(
    @SerialName("tag")
    val tag: String,

    @SerialName("entries")
    val entries: List<JournalEntry>
) {
    val timeAfterLastEntry: LocalTime?
        get() = entries
            .lastOrNull()
            ?.let {
                it
                    .entryTime
                    .plus(1.seconds)
                    .toLocalDateTime(it.timeZone)
                    .time
            }
}
