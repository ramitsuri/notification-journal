package com.ramitsuri.notificationjournal.core.data

import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

interface WearDataSharingClient {
    suspend fun postJournalEntry(
        value: String,
        time: Instant,
        timeZoneId: TimeZone,
        tag: String?,
    ): Boolean

    suspend fun requestUpload()

    suspend fun postTemplate(template: JournalEntryTemplate): Boolean

    suspend fun clearTemplates(): Boolean

    suspend fun updateTile(): Boolean
}
