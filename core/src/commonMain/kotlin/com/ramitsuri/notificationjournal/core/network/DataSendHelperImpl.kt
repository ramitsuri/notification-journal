package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

internal class DataSendHelperImpl(
    private val getDataHostProperties: suspend () -> DataHostProperties,
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

    override suspend fun sendVerifyEntriesRequest(
        date: LocalDate,
        verification: VerifyEntries.Verification,
        time: Instant,
    ): Boolean {
        return VerifyEntries.Request(
            date = date,
            verification = verification,
            time = time,
        ).send()
    }

    override suspend fun sendVerifyEntriesResponse(
        date: LocalDate,
        verification: VerifyEntries.Verification,
        time: Instant,
    ): Boolean {
        return VerifyEntries.Response(
            date = date,
            verification = verification,
            time = time,
        ).send()
    }

    private suspend fun Payload.send(): Boolean {
        log("Sending payload: ${this::class.qualifiedName?.split(".")?.takeLast(2)?.joinToString(".")}")
        return false
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
