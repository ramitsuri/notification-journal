package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagGroup(
    @SerialName("tag")
    val tag: String,

    @SerialName("entries")
    val entries: List<JournalEntry>
)
