package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.sync.Diagnostic
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Message
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.EncryptionHelper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class WebSocketHelper(
    private val httpClient: HttpClient,
    private val json: Json,
    private val encryptionHelper: EncryptionHelper,
    private val port: Int = Constants.DATA_HOST_PORT,
) {
    private val _messages = MutableSharedFlow<Payload>(extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    private val _isConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
        set(value) {
            field = value
            _isConnected.value = value != null
        }

    private val pendingAcks = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val pendingMutex = Mutex()

    private val processedIds = mutableSetOf<String>()
    private val idQueue = ArrayDeque<String>()
    private val dedupeMutex = Mutex()

    private var entityHandler: (suspend (Entity) -> Unit)? = null

    fun setEntityHandler(handler: suspend (Entity) -> Unit) {
        this.entityHandler = handler
    }

    suspend fun start(getDataHostProperties: suspend () -> DataHostProperties) {
        if (session != null) {
            Logger.withTag(TAG).d { "Already connected" }
            return
        }
        Logger.withTag(TAG).d { "Starting" }
        val dataHostProperties = getDataHostProperties()
        if (!dataHostProperties.isValid()) {
            Logger.withTag(TAG).w { "Cannot start websocket, invalid data host properties" }
            return
        }
        try {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = dataHostProperties.dataHost,
                port = port,
                path = "/${Constants.DATA_HOST_EXCHANGE}",
            ) {
                session = this
                Logger.withTag(TAG).i { "Connected to websocket: $session" }
                startReceiver()
            }
            session = null
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error while starting websocket" }
            session = null
        } finally {
            session = null
            failPendingAcks()
        }
    }

    suspend fun stop() {
        Logger.withTag(TAG).d { "Stopping websocket" }
        session?.close()
        session = null
        failPendingAcks()
    }

    suspend fun send(
        payload: Payload,
        timeout: Duration = 10.seconds,
    ): Boolean {
        val sess = session ?: return false
        val requireAck = payload is Entity
        val id = if (requireAck) Uuid.random().toString() else null

        val deferred =
            if (id != null) {
                CompletableDeferred<Boolean>().also {
                    pendingMutex.withLock { pendingAcks[id] = it }
                }
            } else {
                null
            }

        return try {
            val message = Message.Data(id = id, payload = payload)
            val encrypted = encryptionHelper.encrypt(json.encodeToString<Message>(message))
            sess.send(encrypted)

            deferred == null || withTimeoutOrNull(timeout) { deferred.await() } ?: false
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error sending message" }
            false
        } finally {
            if (id != null) {
                pendingMutex.withLock { pendingAcks.remove(id) }
            }
        }
    }

    private suspend fun failPendingAcks() {
        pendingMutex.withLock {
            pendingAcks.values.forEach { it.complete(false) }
            pendingAcks.clear()
        }
    }

    private suspend fun DefaultClientWebSocketSession.startReceiver() {
        try {
            for (incomingMessage in incoming) {
                incomingMessage as? Frame.Binary ?: continue
                val decrypted =
                    try {
                        encryptionHelper.decrypt(incomingMessage.readBytes())
                    } catch (e: Exception) {
                        Logger.withTag(TAG).e(e) { "Failed to decrypt message" }
                        continue
                    }

                val message =
                    try {
                        json.decodeFromString<Message>(decrypted)
                    } catch (e: Exception) {
                        try {
                            Message.Data(id = null, payload = json.decodeFromString<Payload>(decrypted))
                        } catch (_: Exception) {
                            Logger.withTag(TAG).e(e) { "Failed to decode message" }
                            continue
                        }
                    }

                when (message) {
                    is Message.Ack -> {
                        pendingMutex.withLock { pendingAcks.remove(message.id) }?.complete(true)
                    }

                    is Message.Data -> {
                        when (val payload = message.payload) {
                            is Diagnostic -> _messages.emit(payload)
                            is Entity -> handleEntityAndAck(message.id, payload)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Error in websocket receiver" }
        } finally {
            failPendingAcks()
        }
    }

    private suspend fun handleEntityAndAck(
        id: String?,
        entity: Entity,
    ) {
        if (id != null && isDuplicate(id)) {
            Logger.withTag(TAG).d { "Duplicate message $id received, re-acking" }
            sendAck(id)
            return
        }

        val handler = entityHandler
        if (handler == null) {
            Logger.withTag(TAG).w { "No entity handler registered, omitting ACK for $id" }
            return
        }

        try {
            handler(entity) // Suspend until DB writes finish
            if (id != null) {
                markProcessed(id)
                sendAck(id)
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Handler failed for $id, omitting ACK" }
        }
    }

    private suspend fun sendAck(id: String) {
        try {
            session?.let { sess ->
                val ackMessage: Message = Message.Ack(id = id)
                val encrypted = encryptionHelper.encrypt(json.encodeToString<Message>(ackMessage))
                sess.send(encrypted)
            }
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Failed to send ACK for $id" }
        }
    }

    private suspend fun isDuplicate(id: String): Boolean = dedupeMutex.withLock { id in processedIds }

    private suspend fun markProcessed(id: String) {
        dedupeMutex.withLock {
            if (processedIds.add(id)) {
                idQueue.addLast(id)
                if (idQueue.size > MAX_PROCESSED_IDS) {
                    processedIds.remove(idQueue.removeFirst())
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebSocketHelper"
        private const val MAX_PROCESSED_IDS = 500
    }
}
