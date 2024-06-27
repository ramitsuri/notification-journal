package com.ramitsuri.notificationjournal.core.network

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class DataReceiveHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val hostName: String,
    private val exchangeName: String,
    private val deviceName: String,
    private val deviceId: String,
    private val username: String,
    private val password: String,
    private val json: Json,
) : DataReceiveHelper {

    private var connection: Connection? = null
    private var channel: Channel? = null
    private val mutex: Mutex = Mutex()

    override suspend fun startListening(onPayloadReceived: (Payload) -> Unit) {
        val deliverCallback = DeliverCallback { _: String?, delivery: Delivery ->
            val message = String(delivery.body, Charsets.UTF_8)
            val payload = json.decodeFromString<Payload>(message)
            if (payload.sender.id == deviceId) {
                return@DeliverCallback
            }
            onPayloadReceived(payload)
        }
        withContext(ioDispatcher) {
            mutex.withLock {
                createChannelIfNecessary()
                try {
                    channel?.basicConsume(deviceName, true, deliverCallback) { _ -> }
                } catch (e: Exception) {
                    log("Failed to setup receiver: ${e.message}")
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
        try {
            if (connection == null) {
                connection = ConnectionFactory().apply {
                    host = hostName
                    username = this@DataReceiveHelperImpl.username
                    password = this@DataReceiveHelperImpl.password
                    isAutomaticRecoveryEnabled = true
                    isTopologyRecoveryEnabled = true
                }.newConnection()
                channel = connection?.createChannel()
                channel?.queueDeclare(deviceName, true, false, false, null)
                channel?.queueBind(deviceName, exchangeName, "")
            }
        } catch (e: Exception) {
            log("Failed to connect to RabbitMQ: $e")
            closeConnection()
        }
    }

    private fun log(message: String) {
        println("$TAG: $message")
    }

    companion object {
        private const val TAG = "DataReceiveHelper"
    }
}