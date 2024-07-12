package com.ramitsuri.notificationjournal.core.model.entry

import com.ramitsuri.notificationjournal.core.model.sync.Sender

data class EntriesVerification(
    val sentEntries: List<JournalEntry> = listOf(),
    val receivedEntries: List<JournalEntry> = listOf(),
    val verifiedBy: Sender? = null,
) {
    val isComplete: Boolean
        get() = sentEntries.isNotEmpty() && receivedEntries.isNotEmpty() && verifiedBy != null

    fun getMismatchedEntries(): List<JournalEntry> {
        return receivedEntries.filter { receivedEntry ->
            val sentEntry = sentEntries.firstOrNull { it.id == receivedEntry.id }
            sentEntry != receivedEntry
        }
    }
}

data class EntriesVerificationResponse(
    val sender: Sender? = null,
    val entries: List<JournalEntryVerification> = listOf(),
) {
    companion object {
        fun EntriesVerification.toResponse(): EntriesVerificationResponse {
            val entries = receivedEntries.map { receivedEntry ->
                val sentEntry = sentEntries.firstOrNull { it.id == receivedEntry.id }
                JournalEntryVerification(
                    matchStatus = sentEntry == receivedEntry,
                    entry = receivedEntry
                )
            }
            return EntriesVerificationResponse(
                sender = verifiedBy,
                entries = entries,
            )
        }
    }
}

data class JournalEntryVerification(
    val matchStatus: Boolean? = null,
    val entry: JournalEntry,
)