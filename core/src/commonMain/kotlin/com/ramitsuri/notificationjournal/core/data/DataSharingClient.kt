package com.ramitsuri.notificationjournal.core.data

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

interface DataSharingClient {
    suspend fun postJournalEntry(
        value: String,
        time: Instant,
        timeZoneId: TimeZone,
        tag: String?,
    ): Boolean

    suspend fun requestUpload()

    suspend fun postTemplate(id: Int, value: String, tag: String): Boolean
}
