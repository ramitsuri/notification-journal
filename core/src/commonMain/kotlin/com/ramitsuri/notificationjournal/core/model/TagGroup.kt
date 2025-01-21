package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TagGroup(
    @SerialName("tag")
    val tag: String,
    @SerialName("entries")
    val entries: List<JournalEntry>,
) {
    val timeAfterLastEntry: LocalTime?
        get() =
            entries
                .lastOrNull()
                ?.entryTime
                ?.plus(1.seconds)
                ?.time
}
