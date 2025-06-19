package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class TestDataSendHelper : DataSendHelper {
    var sendSuccessful = true
    val entriesSent = mutableListOf<JournalEntry>()

    override suspend fun sendEntries(entries: List<JournalEntry>): Boolean {
        if (sendSuccessful) {
            entriesSent.addAll(entries)
        } else {
            entriesSent.removeAll { true }
        }
        return sendSuccessful
    }

    override suspend fun sendTags(tags: List<Tag>): Boolean {
        return sendSuccessful
    }

    override suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean {
        return sendSuccessful
    }

    override suspend fun sendClearDaysAndInsertEntries(
        days: List<LocalDate>,
        entries: List<JournalEntry>,
    ): Boolean {
        return sendSuccessful
    }

    override suspend fun sendPing(time: Instant): Boolean {
        return sendSuccessful
    }

    override suspend fun sendPingResponse(time: Instant): Boolean {
        return sendSuccessful
    }

    override suspend fun sendVerifyEntriesRequest(
        date: LocalDate,
        hash: String,
        time: Instant,
    ): Boolean {
        return sendSuccessful
    }

    override suspend fun sendVerifyEntriesResponse(
        date: LocalDate,
        hash: String,
        time: Instant,
    ): Boolean {
        return sendSuccessful
    }
}
