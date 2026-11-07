package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.model.toSender
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal class DataSendHelperImpl(
    private val getDataHostProperties: suspend () -> DataHostProperties,
    private val webSocketHelper: WebSocketHelper,
) : DataSendHelper {
    override suspend fun sendEntries(entries: List<JournalEntry>): Boolean =
        Entity
            .Entries(
                data = entries,
            ).send()

    override suspend fun sendTags(tags: List<Tag>): Boolean =
        Entity
            .Tags(
                data = tags,
            ).send()

    override suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean =
        Entity
            .Templates(
                data = templates,
            ).send()

    override suspend fun sendClearDaysAndInsertEntries(
        days: List<LocalDate>,
        entries: List<JournalEntry>,
    ): Boolean =
        Entity
            .ClearDaysAndInsertEntries(
                days = days,
                entries = entries,
            ).send()

    override suspend fun sendVerifyEntriesRequest(
        date: LocalDate,
        verification: VerifyEntries.Verification,
        time: Instant,
    ): Boolean =
        VerifyEntries
            .Request(
                date = date,
                verification = verification,
                time = time,
            ).send()

    override suspend fun sendVerifyEntriesResponse(
        date: LocalDate,
        verification: VerifyEntries.Verification,
        time: Instant,
    ): Boolean =
        VerifyEntries
            .Response(
                date = date,
                verification = verification,
                time = time,
            ).send()

    private suspend fun Payload.send(): Boolean {
        val dataHostProperties = getDataHostProperties()
        val actualPayload = this.attachSender(dataHostProperties.toSender())
        val payloadName =
            this::class
                .qualifiedName
                ?.split(".")
                ?.takeLast(2)
                ?.joinToString(".")
        Logger.withTag(TAG).d { "Sending payload: $payloadName" }
        val sent =
            webSocketHelper.send(
                payload = actualPayload,
            )
        if (!sent) {
            Logger.withTag(TAG).w { "Failed to send payload: $payloadName" }
        }
        return sent
    }

    companion object {
        private const val TAG = "DataSendHelper"
    }
}
