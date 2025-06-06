package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

internal class DataSendHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val getDataHostProperties: suspend () -> DataHostProperties,
    private val json: Json,
) : DataSendHelper {
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val mutex: Mutex = Mutex()

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

    private suspend fun Payload.send(isRetry: Boolean = false): Boolean {
        log("Sending payload")
        val dataHostProperties = getDataHostProperties()
        if (!dataHostProperties.isValid()) {
            log("Cannot send, invalid data host properties")
            return false
        }
        val payloadWithSender =
            this.attachSender(Sender(name = dataHostProperties.deviceName, id = dataHostProperties.deviceId))
        return withContext(ioDispatcher) {
            mutex.withLock {
                createChannelIfNecessary(dataHostProperties)
                try {
                    val message = json.encodeToString(payloadWithSender).toByteArray()
                    channel?.let {
                        it.basicPublish(
                            dataHostProperties.exchangeName,
                            "",
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            message,
                        )
                        it.waitForConfirmsOrDie(5.seconds.inWholeMilliseconds)
                        true
                    } ?: run {
                        log("Channel not available")
                        false
                    }
                } catch (e: Exception) {
                    log("failed to send message", e)
                    if (isRetry) {
                        return@withContext false
                    } else {
                        return@withContext send(isRetry = true)
                    }
                }
            }
        }
    }

    override suspend fun closeConnection() {
        log("Closing connection")
        mutex.withLock {
            closeConnectionInternal()
        }
    }

    private fun closeConnectionInternal() {
        runCatching {
            connection?.close()
            connection = null
            channel?.close()
            channel = null
            log("Connection closed")
        }
    }

    private fun createChannelIfNecessary(dataHostProperties: DataHostProperties) {
        try {
            if (connection == null) {
                connection =
                    ConnectionFactory().apply {
                        host = dataHostProperties.dataHost
                        username = dataHostProperties.username
                        password = dataHostProperties.password
                        isAutomaticRecoveryEnabled = true
                        isTopologyRecoveryEnabled = true
                    }.newConnection()
                channel = connection?.createChannel()
                channel?.confirmSelect()
            }
        } catch (e: Exception) {
            log("Failed to connect to RabbitMQ", e)
            closeConnectionInternal()
        }
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
