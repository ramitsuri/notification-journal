package com.ramitsuri.notificationjournal.core.model.entry

import com.ramitsuri.notificationjournal.core.model.sync.Sender

data class EntriesVerification(
    val sentEntries: List<JournalEntry> = listOf(),
    val receivedEntries: List<JournalEntry> = listOf(),
    val verifiedBy: Sender? = null,
) {
    val isComplete: Boolean
        get() = sentEntries.isNotEmpty() && receivedEntries.isNotEmpty()

    fun getMismatchedEntries(): List<JournalEntry> {
        return receivedEntries.filter { receivedEntry ->
            val sentEntry = sentEntries.firstOrNull { it.id == receivedEntry.id }
            sentEntry != receivedEntry
        }
    }
}
