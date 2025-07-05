package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

class WebSocketHelper(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private val _messages = MutableSharedFlow<Payload>()
    val messages = _messages.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null

    suspend fun start(getDataHostProperties: suspend () -> DataHostProperties) {
        stop()
        val dataHostProperties = getDataHostProperties()
        if (!dataHostProperties.isValid()) {
            log("Cannot send, invalid data host properties")
            return
        }
        try {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = dataHostProperties.dataHost,
                port = dataHostProperties.port,
                path = "/${dataHostProperties.exchangeName}",
            ) {
                session = this
                log("Connected to websocket: $session")
                startReceiver()
            }
        } catch (e: Exception) {
            log("Error while starting websocket", e)
        }
    }

    suspend fun stop() {
        session?.close()
        session = null
    }

    suspend fun send(payload: Payload): Boolean {
        return try {
            session?.let {
                it.send(json.encodeToString(payload))
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
                message as? Frame.Text ?: continue
                val payload = json.decodeFromString<Payload>(message.readText())
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
        Logger.i("WebSocketHelper") { "$message, ${throwable?.localizedMessage}" }
    }
}
