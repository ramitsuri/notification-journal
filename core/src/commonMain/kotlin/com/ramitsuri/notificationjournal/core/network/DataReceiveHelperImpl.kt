package com.ramitsuri.notificationjournal.core.network

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DataReceiveHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val hostName: String,
    private val exchangeName: String,
    private val clientName: String,
    private val clientId: String,
    private val json: Json,
) : DataReceiveHelper {

    private var connection: Connection? = null
    private var channel: Channel? = null
    private val mutex: Mutex = Mutex()

    override suspend fun startListening(onPayloadReceived: (Payload) -> Unit) {
        val deliverCallback = DeliverCallback { _: String?, delivery: Delivery ->
            val message = String(delivery.body, Charsets.UTF_8)
            val payload = json.decodeFromString<Payload>(message)
            if (payload.sender.id == clientId) {
                return@DeliverCallback
            }
            onPayloadReceived(payload)
        }
        mutex.withLock {
            createChannelIfNecessary()
            try {
                channel?.basicConsume(clientName, true, deliverCallback) { _ -> }
            } catch (e: Exception) {
                log("Failed to setup receiver: ${e.message}")
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
                    channel?.queueDeclare(clientName, true, false, false, null)
                    channel?.queueBind(clientName, exchangeName, "")
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
        private const val TAG = "DataReceiveHelper"
    }
}