package com.ramitsuri.notificationjournal.core.model.sync

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Payload {
    val sender: Sender

    fun attachSender(sender: Sender): Payload {
        return when (this) {
            is Entity.Tags -> copy(sender = sender)
            is Entity.Entries -> copy(sender = sender)
            is Entity.ClearDaysAndInsertEntries -> copy(sender = sender)
            is Entity.Templates -> copy(sender = sender)

            is VerifyEntries.Request -> copy(sender = sender)
            is VerifyEntries.Response -> copy(sender = sender)
        }
    }
}

@Serializable
sealed interface Diagnostic : Payload {
    val time: Instant
}

sealed interface VerifyEntries : Diagnostic {
    val date: LocalDate
    val verification: Verification

    @Serializable
    data class Request(
        override val date: LocalDate,
        override val verification: Verification,
        override val time: Instant,
        override val sender: Sender = Sender(),
    ) : VerifyEntries

    @Serializable
    data class Response(
        override val date: LocalDate,
        override val verification: Verification,
        override val time: Instant,
        override val sender: Sender = Sender(),
    ) : VerifyEntries

    @Serializable
    data class Verification(
        val entries: List<JournalEntry>,
    ) {
        fun unmatchedEntries(other: Verification): List<JournalEntry> {
            if (other.entries.size > entries.size) {
                return other.entries.filter { otherEntry ->
                    entries.none { it.id == otherEntry.id }
                }
            }
            return entries.mapNotNull { entry ->
                val otherEntry = other.entries.find { it.id == entry.id }
                if (otherEntry == null) {
                    // Entry not found in other, so exists only locally, consider it as unmatched
                    entry
                } else if (otherEntry.tag != entry.tag ||
                    otherEntry.text != entry.text ||
                    otherEntry.entryTime != entry.entryTime
                ) {
                    otherEntry
                } else {
                    null
                }
            }
        }
    }
}

@Serializable
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
