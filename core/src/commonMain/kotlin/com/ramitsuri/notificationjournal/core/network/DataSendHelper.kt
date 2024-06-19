package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Action
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate

interface DataSendHelper {

    suspend fun sendEntry(entry: JournalEntry, action: Action): Boolean

    suspend fun sendTags(tags: List<Tag>): Boolean

    suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean

    suspend fun closeConnection()
}