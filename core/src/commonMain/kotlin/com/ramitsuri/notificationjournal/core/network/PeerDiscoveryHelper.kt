package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.Peer
import com.ramitsuri.notificationjournal.core.model.sync.Diagnostic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PeerDiscoveryHelper(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val dataSendHelper: DataSendHelper,
    private val dataReceiveHelper: DataReceiveHelper,
    private val clock: Clock = Clock.System,
) {
    private lateinit var job: Job
    private val _connectedPeers = MutableStateFlow<List<Peer>>(listOf())
    val connectedPeers = _connectedPeers.asStateFlow()

    fun start() {
        job =
            coroutineScope.launch(ioDispatcher) {
                launch {
                    dataReceiveHelper
                        .payloadFlow
                        .filterIsInstance<Diagnostic.PingRequest>()
                        .collect { request ->
                            if (clock.now() - request.time > 30.seconds) {
                                Logger.i(TAG) { "Ping request is too old" }
                                return@collect
                            }
                            Logger.i(TAG) { "Ping request received from ${request.sender.name}" }
                            dataSendHelper.sendPingResponse(clock.now())
                        }
                }
                launch {
                    sendPing()
                }
            }
    }

    fun stop() {
        job.cancel()
        _connectedPeers.update { emptyList() }
    }

    private suspend fun sendPing(delay: Duration = BASE_PING_REPEAT_SECONDS.seconds) {
        Logger.i(TAG) { "sendPing, waiting for $delay" }
        var pingSent = true
        delay(delay)
        dataSendHelper
            .sendPing(clock.now())
            .also { sent ->
                if (sent) {
                    Logger.i(TAG) { "Ping sent" }
                } else {
                    Logger.i(TAG) { "Ping not sent" }
                    pingSent = false
                }
            }
        if (pingSent) {
            var responseReceived = false
            Logger.i(TAG) { "Will wait for ping response" }
            dataReceiveHelper
                .payloadFlow
                .filterIsInstance<Diagnostic.PingResponse>()
                .timeout(RECEIVE_TIMEOUT_SECONDS.seconds)
                .catch {
                    if (it is TimeoutCancellationException) {
                        Logger.i(TAG) { "canceled from timeout" }
                        if (!responseReceived) {
                            _connectedPeers.update { emptyList() }
                            pingSent = false
                        }
                    }
                }
                .collect { response ->
                    if (clock.now() - response.time > 30.seconds) {
                        Logger.i(TAG) { "Ping request is too old" }
                        return@collect
                    }
                    responseReceived = true
                    Logger.i(TAG) { "response received" }
                    _connectedPeers.update(Peer.fromSender(response.sender, clock.now()))
                }
        }
        if (pingSent) {
            // Send with normal delay again
            sendPing()
        } else {
            sendPing(delay = delay * 2)
        }
    }

    private fun MutableStateFlow<List<Peer>>.update(newPeer: Peer) {
        // Remove any old peers with that Id and add the new one
        update { currentPeers ->
            val newPeers =
                currentPeers
                    .toMutableList()
                    .apply {
                        removeAll { it.id == newPeer.id }
                        add(newPeer)
                    }
            newPeers
        }
    }

    companion object {
        private const val BASE_PING_REPEAT_SECONDS = 30
        private const val RECEIVE_TIMEOUT_SECONDS = 30
        private const val TAG = "PeerDiscoveryHelper"
    }
}
