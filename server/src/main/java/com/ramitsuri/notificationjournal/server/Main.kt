package com.ramitsuri.notificationjournal.server

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.io.File
import java.util.Collections

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

@Suppress("unused")
fun Application.module() {
    install(WebSockets)

    val exchanges = getExchanges()
    if (exchanges.isEmpty()) {
        println("No exchanges found in exchanges.txt")
        return
    }

    routing {
        for (exchange in exchanges) {
            startWebSocket(exchange)
        }
    }
}

private fun Route.startWebSocket(exchange: String) {
    val clients = Collections.synchronizedSet<Client>(LinkedHashSet())
    webSocket("/$exchange") {
        val thisClient = Client(this)
        clients += thisClient
        try {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
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

private fun getExchanges() = File("server/exchanges.txt")
    .readLines()
    .firstOrNull()
    ?.split(",")
    ?: emptyList()

