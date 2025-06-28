package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.toSender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class RabbitMqHelper(
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private var connection: Connection? = null
    private val mutex: Mutex = Mutex()

    suspend fun send(
        getDataHostProperties: suspend () -> DataHostProperties,
        payload: Payload,
    ): Boolean {
        val dataHostProperties = getDataHostProperties()
        if (!dataHostProperties.isValid()) {
            log("Cannot send, invalid data host properties")
            return false
        }
        val channel = createSendChannel(dataHostProperties)
        if (channel == null) {
            log("Failed to create channel")
            return false
        }
        return withContext(ioDispatcher) {
            val actualPayload = payload.attachSender(dataHostProperties.toSender())
            channel.safelyPublish(
                dataHostProperties = dataHostProperties,
                message = json.encodeToString(actualPayload).toByteArray(),
            )
        }
    }

    fun receive(getDataHostProperties: suspend () -> DataHostProperties): Flow<Payload> =
        callbackFlow {
            val dataHostProperties = getDataHostProperties()
            if (!dataHostProperties.isValid()) {
                log("Cannot start receiving, invalid data host properties")
                close()
                return@callbackFlow
            }
            val rmqChannel = createReceiveChannel(dataHostProperties)
            if (rmqChannel == null) {
                log("Failed to create channel")
                close()
                return@callbackFlow
            }
            val token =
                try {
                    rmqChannel.basicConsume(
                        // queue =
                        dataHostProperties.deviceName,
                        // autoAck =
                        false,
                        // deliverCallback =
                        { _, delivery ->
                            val message = String(delivery.body, Charsets.UTF_8)
                            val payload = json.decodeFromString<Payload>(message)
                            if (payload.sender.id != dataHostProperties.deviceId) {
                                trySendBlocking(payload)
                                    .onSuccess {
                                        rmqChannel.basicAck(delivery.envelope.deliveryTag, false)
                                    }
                                    .onFailure {
                                        log("Failed to send to channel", it)
                                    }
                            }
                        },
                        // cancelCallback =
                        {
                            rmqChannel.close()
                            close()
                        },
                    )
                } catch (_: AlreadyClosedException) {
                    log("Connection already closed, reset connection")
                    this@RabbitMqHelper.close()
                    getConnection(dataHostProperties)
                    close()
                    null
                } catch (e: Exception) {
                    log("Failed to setup receiver", e)
                    close()
                    null
                }
            awaitClose {
                token?.let { runCatching { rmqChannel.basicCancel(it) } }
            }
        }

    private suspend fun createReceiveChannel(dataHostProperties: DataHostProperties): Channel? {
        return withContext(ioDispatcher) {
            getConnection(dataHostProperties)
                ?.safelyCreateChannel()
                ?.setupForReceiving(
                    queue = dataHostProperties.deviceName,
                    exchange = dataHostProperties.exchangeName,
                )
        }
    }

    suspend fun close() {
        mutex.withLock {
            withContext(ioDispatcher) {
                try {
                    connection?.addShutdownListener {
                        log("Connection shutdown: ${it.reason}")
                    }
                    connection?.close()
                } catch (e: Exception) {
                    log("Failed to close connection", e)
                } finally {
                    connection = null
                }
            }
        }
    }

    private suspend fun createSendChannel(dataHostProperties: DataHostProperties): Channel? {
        return withContext(ioDispatcher) {
            getConnection(dataHostProperties)
                ?.safelyCreateChannel()
                ?.apply {
                    confirmSelect()
                }
        }
    }

    private suspend fun getConnection(dataHostProperties: DataHostProperties): Connection? {
        return mutex.withLock {
            withContext(ioDispatcher) {
                connection ?: ConnectionFactory()
                    .apply {
                        host = dataHostProperties.dataHost
                        username = dataHostProperties.username
                        password = dataHostProperties.password
                        isAutomaticRecoveryEnabled = true
                        isTopologyRecoveryEnabled = true
                        connectionTimeout = 30_000 // 30 seconds
                    }.let {
                        try {
                            it.newConnection()
                        } catch (e: Exception) {
                            log("Failed to connect to RabbitMQ", e)
                            null
                        }
                    }
                    .also { connection = it }
            }
        }
    }

    private suspend fun Channel.safelyPublish(
        dataHostProperties: DataHostProperties,
        message: ByteArray,
        isRetry: Boolean = false,
    ): Boolean {
        return try {
            basicPublish(
                // exchange =
                dataHostProperties.exchangeName,
                // routingKey =
                "",
                // props =
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                // body =
                message,
            )
            waitForConfirmsOrDie(5.seconds.inWholeMilliseconds)
            true
        } catch (_: AlreadyClosedException) {
            log("Connection already closed, reset connection")
            this@RabbitMqHelper.close()
            getConnection(dataHostProperties)
            false
        } catch (e: Exception) {
            log("failed to publish message", e)
            if (isRetry) {
                false
            } else {
                safelyClose()
                safelyPublish(
                    dataHostProperties = dataHostProperties,
                    message = message,
                    isRetry = true,
                )
            }
        } finally {
            log("closing channel after send")
            safelyClose()
        }
    }

    private fun Channel.safelyClose() {
        try {
            close()
        } catch (e: Exception) {
            log("failed to close channel", e)
        }
    }

    private suspend fun Connection.safelyCreateChannel() =
        try {
            withContext(ioDispatcher) {
                createChannel()
            }
        } catch (e: Exception) {
            log("Failed to create channel", e)
            null
        }

    private fun Channel.setupForReceiving(
        queue: String,
        exchange: String,
    ) = try {
        queueDeclare(
            // queue =
            queue,
            // durable =
            true,
            // exclusive =
            false,
            // autoDelete =
            false,
            // arguments =
            null,
        )
        queueBind(
            // queue =
            queue,
            // exchange =
            exchange,
            // routingKey =
            "",
        )
        this
    } catch (e: Exception) {
        log("Failed to setup for receiving", e)
        null
    }

    private fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        Logger.i(TAG, throwable) { message }
    }

    companion object {
        private const val TAG = "RabbitMqHelper"
    }
}
