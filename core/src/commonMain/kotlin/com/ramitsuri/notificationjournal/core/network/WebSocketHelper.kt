package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class WebSocketHelper(
    private val httpClient: HttpClient,
    private val json: Json,
    private val encryptionHelper: EncryptionHelper,
) {
    private val _messages = MutableSharedFlow<Payload>()
    val messages = _messages.asSharedFlow()

    private val _isConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
        set(value) {
            field = value
            _isConnected.value = value != null
        }

    suspend fun start(getDataHostProperties: suspend () -> DataHostProperties) {
        if (session != null) {
            log("Already connected")
            return
        }
        log("Starting")
        val dataHostProperties = getDataHostProperties()
        if (!dataHostProperties.isValid()) {
            log("Cannot send, invalid data host properties")
            return
        }
        try {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = dataHostProperties.dataHost,
                port = Constants.DATA_HOST_PORT,
                path = "/${Constants.DATA_HOST_EXCHANGE}",
            ) {
                session = this
                log("Connected to websocket: $session")
                startReceiver()
            }
            session = null
        } catch (e: Exception) {
            log("Error while starting websocket", e)
            session = null
        }
    }

    suspend fun stop() {
        log("Stopping")
        session?.close()
        session = null
    }

    suspend fun send(payload: Payload): Boolean {
        return try {
            session?.let { sess ->
                val jsonString = json.encodeToString(payload)
                val encrypted = encryptionHelper.encrypt(jsonString)
                sess.send(encrypted)
                true
            } ?: false
        } catch (e: Exception) {
            log("Error while sending", e)
            false
        }
    }

    private suspend fun DefaultClientWebSocketSession.startReceiver() {
        try {
            for (message in incoming) {
                message as? Frame.Binary ?: continue
                val decrypted = encryptionHelper.decrypt(message.readBytes())
                val payload = json.decodeFromString<Payload>(decrypted)
                _messages.emit(payload)
            }
        } catch (e: Exception) {
            log("Error while receiving", e)
        }
    }

    private fun log(
        message: String,
        throwable: Throwable? = null,
    ) {
        Logger.i("WebSocketHelper") {
            val exceptionMessage = throwable?.localizedMessage
            if (exceptionMessage.isNullOrEmpty()) {
                message
            } else {
                "$message , $exceptionMessage"
            }
        }
    }
}
