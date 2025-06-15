package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.datetime.LocalDate

internal class DataSendHelperImpl(
    private val getDataHostProperties: suspend () -> DataHostProperties,
    private val rabbitMqHelper: RabbitMqHelper,
) : DataSendHelper {
    override suspend fun sendEntries(entries: List<JournalEntry>): Boolean {
        return Payload.Entries(
            data = entries,
        ).send()
    }

    override suspend fun sendTags(tags: List<Tag>): Boolean {
        return Payload.Tags(
            data = tags,
        ).send()
    }

    override suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean {
        return Payload.Templates(
            data = templates,
        ).send()
    }

    override suspend fun sendClearDaysAndInsertEntries(
        days: List<LocalDate>,
        entries: List<JournalEntry>,
    ): Boolean {
        return Payload.ClearDaysAndInsertEntries(
            days = days,
            entries = entries,
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
