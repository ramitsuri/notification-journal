package com.ramitsuri.notificationjournal.server

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.send
import kotlin.uuid.Uuid

class Client(
    private val session: DefaultWebSocketSession,
    val id: String = Uuid.random().toString()
) {
    suspend fun send(message: String) {
        session.send(message)
    }
}
