package com.ramitsuri.notificationjournal.core.model.entry

import com.ramitsuri.notificationjournal.core.model.sync.Sender

data class EntriesVerification(
    val sentEntries: List<JournalEntry> = listOf(),
    val receivedEntries: List<JournalEntry> = listOf(),
    val verifiedBy: Sender? = null,
) {
    val isComplete: Boolean
        get() = sentEntries.isNotEmpty() && receivedEntries.isNotEmpty()

    fun partitionByMatching(): Pair<List<JournalEntry>, List<JournalEntry>> {
        val matching = mutableListOf<JournalEntry>()
        val notMatching = mutableListOf<JournalEntry>()
        sentEntries.forEach { sent ->
            val received = receivedEntries.firstOrNull { it.id == sent.id } ?: return@forEach
            if (received == sent) {
                matching.add(sent)
            } else {
                notMatching.add(sent)
            }
        }
        return Pair(matching, notMatching)
    }
}
