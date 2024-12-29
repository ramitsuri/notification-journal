package com.ramitsuri.notificationjournal.core.data

import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.LocalDateTime

interface WearDataSharingClient {
    suspend fun postJournalEntry(
        value: String,
        time: LocalDateTime,
        tag: String?,
    ): Boolean

    suspend fun requestUpload()

    suspend fun postTemplate(template: JournalEntryTemplate): Boolean

    suspend fun clearTemplates(): Boolean

    suspend fun updateTile(): Boolean
}
