package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Diagnostic
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

internal class DataSendHelperImpl(
    private val getDataHostProperties: suspend () -> DataHostProperties,
    private val rabbitMqHelper: RabbitMqHelper,
) : DataSendHelper {
    override suspend fun sendEntries(entries: List<JournalEntry>): Boolean {
        return Entity.Entries(
            data = entries,
        ).send()
    }

    override suspend fun sendTags(tags: List<Tag>): Boolean {
        return Entity.Tags(
            data = tags,
        ).send()
    }

    override suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean {
        return Entity.Templates(
            data = templates,
        ).send()
    }

    override suspend fun sendClearDaysAndInsertEntries(
        days: List<LocalDate>,
        entries: List<JournalEntry>,
    ): Boolean {
        return Entity.ClearDaysAndInsertEntries(
            days = days,
            entries = entries,
        ).send()
    }

    override suspend fun sendPing(time: Instant): Boolean {
        return Diagnostic.PingRequest(time).send()
    }

    override suspend fun sendPingResponse(time: Instant): Boolean {
        return Diagnostic.PingResponse(time).send()
    }

    override suspend fun sendVerifyEntriesRequest(
        date: LocalDate,
        hash: String,
        time: Instant,
    ): Boolean {
        return VerifyEntries.Request(
            date = date,
            hash = hash,
            time = time,
        ).send()
    }

    override suspend fun sendVerifyEntriesResponse(
        date: LocalDate,
        hash: String,
        time: Instant,
    ): Boolean {
        return VerifyEntries.Response(
            date = date,
            hash = hash,
            time = time,
        ).send()
    }

    private suspend fun Payload.send(): Boolean {
        log("Sending payload")
        return rabbitMqHelper.send(
            getDataHostProperties = getDataHostProperties,
            payload = this,
        )
    }

    private fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        Logger.i(TAG, throwable) { message }
    }

    companion object {
        private const val TAG = "DataSendHelper"
    }
}
