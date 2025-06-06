package com.ramitsuri.notificationjournal.core.model.sync

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Payload {
    abstract val sender: Sender

    @Serializable
    @SerialName("tags")
    data class Tags(
        val data: List<Tag>,
        override val sender: Sender = Sender(),
    ) : Payload()

    @Serializable
    @SerialName("entries")
    data class Entries(
        val data: List<JournalEntry>,
        override val sender: Sender = Sender(),
    ) : Payload()

    @Serializable
    @SerialName("clear_days_and_insert_entries")
    data class ClearDaysAndInsertEntries(
        val days: List<LocalDate>,
        val entries: List<JournalEntry>,
        override val sender: Sender = Sender(),
    ) : Payload()

    @Serializable
    @SerialName("templates")
    data class Templates(
        val data: List<JournalEntryTemplate>,
        override val sender: Sender = Sender(),
    ) : Payload()

    fun attachSender(sender: Sender): Payload {
        return when (this) {
            is Tags -> copy(sender = sender)
            is Entries -> copy(sender = sender)
            is ClearDaysAndInsertEntries -> copy(sender = sender)
            is Templates -> copy(sender = sender)
        }
    }
}
