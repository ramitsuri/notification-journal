package com.ramitsuri.notificationjournal.core.network

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Action
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

class DataSendHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val hostName: String,
    private val exchangeName: String,
    private val clientName: String,
    private val clientId: String,
    private val json: Json,
) : DataSendHelper {

    private var connection: Connection? = null
    private var channel: Channel? = null
    private val mutex: Mutex = Mutex()

    override suspend fun sendEntry(entry: JournalEntry, action: Action): Boolean {
        return Payload.Entries(
            data = listOf(entry),
            action = action,
            sender = Sender(name = clientName, id = clientId)
        ).send()
    }

    override suspend fun sendTags(tags: List<Tag>): Boolean {
        return Payload.Tags(
            data = tags,
            action = Action.UPDATE,
            sender = Sender(name = clientName, id = clientId)
        ).send()
    }

    override suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean {
        return Payload.Templates(
            data = templates,
            action = Action.UPDATE,
            sender = Sender(name = clientName, id = clientId)
        ).send()
    }

    private suspend fun Payload.send(): Boolean {
        mutex.withLock {
            createChannelIfNecessary()
            return withContext(ioDispatcher) {
                try {
                    val message = json.encodeToString(this@send).toByteArray()
                    channel?.basicPublish(
                        exchangeName,
                        "",
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message
                    )
                    channel?.waitForConfirmsOrDie(5.seconds.inWholeMilliseconds);
                    true
                } catch (e: Exception) {
                    log("failed to send message: $e")
                    false
                }
            }
        }
    }

    override suspend fun closeConnection() {
        mutex.withLock {
            runCatching {
                connection?.close()
                connection = null
                channel?.close()
                channel = null
            }
        }
    }

    private suspend fun createChannelIfNecessary() {
        withContext(ioDispatcher) {
            try {
                if (connection == null) {
                    connection = ConnectionFactory().apply {
                        host = hostName
                        isAutomaticRecoveryEnabled = true
                        isTopologyRecoveryEnabled = true
                    }.newConnection()
                    channel = connection?.createChannel()
                    channel?.confirmSelect()
                }
            } catch (e: Exception) {
                log("Failed to connect to RabbitMQ: $e")
                closeConnection()
            }
        }
    }

    private fun log(message: String) {
        println("$TAG: $message")
    }

    companion object {
        private const val TAG = "DataSendHelper"
    }
}
