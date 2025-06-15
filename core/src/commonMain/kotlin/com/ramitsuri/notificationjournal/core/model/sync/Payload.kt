package com.ramitsuri.notificationjournal.core.model.sync

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity.ClearDaysAndInsertEntries
import com.ramitsuri.notificationjournal.core.model.sync.Entity.Entries
import com.ramitsuri.notificationjournal.core.model.sync.Entity.Tags
import com.ramitsuri.notificationjournal.core.model.sync.Entity.Templates
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed interface Payload {
    val sender: Sender

    fun attachSender(sender: Sender): Entity {
        return when (this) {
            is Tags -> copy(sender = sender)
            is Entries -> copy(sender = sender)
            is ClearDaysAndInsertEntries -> copy(sender = sender)
            is Templates -> copy(sender = sender)
        }
    }
}

@Serializable
sealed interface Diagnostic : Payload

@Serializable
sealed interface Entity : Payload {
sealed interface Entity : Payload {

    @Serializable
    @SerialName("tags")
    data class Tags(
        val data: List<Tag>,
        override val sender: Sender = Sender(),
    ) : Entity

    @Serializable
    @SerialName("entries")
    data class Entries(
        val data: List<JournalEntry>,
        override val sender: Sender = Sender(),
    ) : Entity

    @Serializable
    @SerialName("clear_days_and_insert_entries")
    data class ClearDaysAndInsertEntries(
        val days: List<LocalDate>,
        val entries: List<JournalEntry>,
        override val sender: Sender = Sender(),
    ) : Entity

    @Serializable
    @SerialName("templates")
    data class Templates(
        val data: List<JournalEntryTemplate>,
        override val sender: Sender = Sender(),
    ) : Entity
}
