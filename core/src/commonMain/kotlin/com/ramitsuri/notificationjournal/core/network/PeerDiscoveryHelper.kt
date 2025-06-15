package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class PeerDiscoveryHelper(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val dataSendHelper: DataSendHelper,
    private val dataReceiveHelper: DataReceiveHelper,
) {
    val connectedPeers = MutableStateFlow<List<Sender>>(listOf())
    private var delayMultiplier = 1

    fun start() {
        coroutineScope.launch(ioDispatcher) {
            while (true) {
                delay(PING_TIME * delayMultiplier)
                dataSendHelper
                    .sendPing()
                    .also { sent ->
                        if (sent) {
                            delayMultiplier = 1
                            Logger.i(TAG) { "Ping sent" }
                        } else {
                            delayMultiplier++
                            Logger.i(TAG) { "Unable to send ping, will retry in ${PING_TIME * delayMultiplier}ms" }
                        }
                    }
            }
        }
        coroutineScope.launch(ioDispatcher) {
            dataReceiveHelper
                .payloadFlow
                .filterIsInstance<Payload.PingResponse>()
                .collect { response ->
                    println("Received ping response from ${response.sender.name}")
                }
        }
    }

    companion object {
        private const val PING_TIME = 30_000L
        private const val TAG = "PeerDiscoveryHelper"
    }
}
