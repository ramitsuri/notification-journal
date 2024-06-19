package com.ramitsuri.notificationjournal.core.model.sync

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Payload {
    abstract val action: Action

    abstract val sender: Sender

    @Serializable
    @SerialName("tags")
    data class Tags(
        val data: List<Tag>,
        override val action: Action,
        override val sender: Sender
    ) : Payload()

    @Serializable
    @SerialName("entries")
    data class Entries(
        val data: List<JournalEntry>,
        override val action: Action,
        override val sender: Sender
    ) : Payload()

    @Serializable
    @SerialName("templates")
    data class Templates(
        val data: List<JournalEntryTemplate>,
        override val action: Action,
        override val sender: Sender
    ) : Payload()
}
