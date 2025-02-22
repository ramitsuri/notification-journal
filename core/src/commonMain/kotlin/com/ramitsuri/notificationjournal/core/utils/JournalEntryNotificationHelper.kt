package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlin.time.Duration

interface JournalEntryNotificationHelper {
    fun show(
        journalEntry: JournalEntry,
        showIn: Duration,
    )
}
