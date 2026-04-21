package com.ramitsuri.notificationjournal.server

import com.ramitsuri.notificationjournal.core.utils.Constants
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import java.util.Collections

@Suppress("unused")
fun Application.module() {
    install(WebSockets)

    routing {
        startWebSocket(Constants.DATA_HOST_EXCHANGE)
    }
}

private fun Route.startWebSocket(exchange: String) {
    val clients = Collections.synchronizedSet<Client>(LinkedHashSet())
    webSocket("/$exchange") {
        val thisClient = Client(this)
        clients += thisClient
        try {
            for (frame in incoming) {
                frame as? Frame.Binary ?: continue
                val receivedText = frame.readBytes()
                clients.forEach { client ->
                    if (client.id != thisClient.id) {
                        client.send(receivedText)
                    }
                }
            }
        } catch (e: Exception) {
            println(e.localizedMessage)
        } finally {
            println("Removing $thisClient!")
            clients -= thisClient
        }
    }
}
