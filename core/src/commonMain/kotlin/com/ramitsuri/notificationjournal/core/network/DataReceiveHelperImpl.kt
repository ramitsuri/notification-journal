package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class DataReceiveHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val getDataHostProperties: suspend () -> DataHostProperties,
    private val json: Json,
) : DataReceiveHelper {
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val mutex: Mutex = Mutex()

    override suspend fun startListening(onPayloadReceived: (Payload) -> Unit) {
        log("Start receiving")
        val dataHostProperties = getDataHostProperties()
        if (!dataHostProperties.isValid()) {
            log("Cannot start receiving, invalid data host properties")
            return
        }
        val deliverCallback =
            DeliverCallback { _: String?, delivery: Delivery ->
                val message = String(delivery.body, Charsets.UTF_8)
                val payload = json.decodeFromString<Payload>(message)
                if (payload.sender.id == dataHostProperties.deviceId) {
                    log("Ignoring own message")
                    return@DeliverCallback
                }
                onPayloadReceived(payload)
            }
        withContext(ioDispatcher) {
            mutex.withLock {
                createChannelIfNecessary(dataHostProperties)
                try {
                    channel?.let {
                        it.basicConsume(dataHostProperties.deviceName, true, deliverCallback) { _ -> }
                    } ?: log("Channel not available")
                } catch (e: Exception) {
                    log("Failed to setup receiver", e)
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

    private suspend fun createChannelIfNecessary(dataHostProperties: DataHostProperties) {
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
                channel?.queueDeclare(dataHostProperties.deviceName, true, false, false, null)
                channel?.queueBind(dataHostProperties.deviceName, dataHostProperties.exchangeName, "")
            }
        } catch (e: Exception) {
            log("Failed to connect to RabbitMQ", e)
            closeConnection()
        }
    }

    private fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        Logger.i(TAG, throwable) { message }
    }

    companion object {
        private const val TAG = "DataReceiveHelper"
    }
}
