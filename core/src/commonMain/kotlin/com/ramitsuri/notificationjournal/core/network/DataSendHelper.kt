package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.LocalDate

interface DataSendHelper {
    suspend fun sendEntries(entries: List<JournalEntry>): Boolean

    suspend fun sendTags(tags: List<Tag>): Boolean

    suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean

    suspend fun sendClearDaysAndInsertEntries(
        days: List<LocalDate>,
        entries: List<JournalEntry>,
    ): Boolean

    suspend fun closeConnection()
}
