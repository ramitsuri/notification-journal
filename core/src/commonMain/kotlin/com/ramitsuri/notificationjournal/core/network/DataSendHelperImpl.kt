package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

internal class DataSendHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val hostName: String,
    private val exchangeName: String,
    private val deviceName: String,
    private val deviceId: String,
    private val username: String,
    private val password: String,
    private val json: Json,
) : DataSendHelper {
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val mutex: Mutex = Mutex()

    override suspend fun sendEntry(
        entries: List<JournalEntry>,
        replacesLocal: Boolean,
    ): Boolean {
        return Payload.Entries(
            data = entries,
            sender = getSender(),
            replacesLocal = replacesLocal,
        ).send()
    }

    override suspend fun sendTags(tags: List<Tag>): Boolean {
        return Payload.Tags(
            data = tags,
            sender = getSender(),
        ).send()
    }

    override suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean {
        return Payload.Templates(
            data = templates,
            sender = getSender(),
        ).send()
    }

    private suspend fun Payload.send(): Boolean {
        log("Sending payload")
        return withContext(ioDispatcher) {
            mutex.withLock {
                createChannelIfNecessary()
                try {
                    val message = json.encodeToString(this@send).toByteArray()
                    channel?.let {
                        it.basicPublish(
                            exchangeName,
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
                    false
                }
            }
        }
    }

    override suspend fun closeConnection() {
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
        }
    }

    private fun getSender() = Sender(name = deviceName, id = deviceId)

    private fun createChannelIfNecessary() {
        try {
            if (connection == null) {
                connection =
                    ConnectionFactory().apply {
                        host = hostName
                        username = this@DataSendHelperImpl.username
                        password = this@DataSendHelperImpl.password
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
