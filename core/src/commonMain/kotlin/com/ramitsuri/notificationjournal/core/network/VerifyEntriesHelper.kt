package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.seconds

class VerifyEntriesHelper(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val dataSendHelper: DataSendHelper,
    private val dataReceiveHelper: DataReceiveHelper,
    private val repository: JournalRepository,
    private val clock: Clock = Clock.System,
) {
    private lateinit var job: Job

    fun start() {
        job =
            coroutineScope.launch(ioDispatcher) {
                launch {
                    dataReceiveHelper
                        .payloadFlow
                        .filterIsInstance<VerifyEntries.Request>()
                        .collect { request ->
                            if (clock.now() - request.time > REQUEST_RESPONSE_STALE_SECONDS.seconds) {
                                Logger.i(TAG) { "Verify entries request is too old" }
                                return@collect
                            }
                            Logger.i(TAG) { "Verify entries request received from ${request.sender.name}" }
                            val hash = repository.getHashForDate(request.date)
                            if (hash == null) {
                                Logger.i(TAG) { "Unable to compute hash for date" }
                                return@collect
                            }
                            dataSendHelper.sendVerifyEntriesResponse(
                                hash = hash,
                                time = clock.now(),
                                date = request.date,
                            )
                        }
                }
            }
    }

    fun stop() {
        job.cancel()
    }

    // Returns name of the peer with which entries hash matches otherwise null
    suspend fun requestVerifyEntries(date: LocalDate): String? {
        val hash = repository.getHashForDate(date)
        if (hash == null) {
            Logger.i(TAG) { "Unable to compute hash for date" }
            return null
        }
        val sent = dataSendHelper.sendVerifyEntriesRequest(
            hash = hash,
            time = clock.now(),
            date = date,
        )
        if (!sent) {
            Logger.i(TAG) { "Verify entries request unable to send" }
            return null
        }
        val response =
            dataReceiveHelper
                .payloadFlow
                .filterIsInstance<VerifyEntries.Response>()
                .firstOrNull()
        if (response == null) {
            Logger.i(TAG) { "Verify entries response not received" }
            return null
        }
        if (clock.now() - response.time > REQUEST_RESPONSE_STALE_SECONDS.seconds) {
            Logger.i(TAG) { "Verify entries response is too old" }
            return null
        }
        if (response.hash != hash) {
            Logger.i(TAG) { "Verify entries response hash doesn't match" }
            return null
        }
        return response.sender.name
    }

    companion object {
        private const val TAG = "VerifyEntriesHelper"
        private const val REQUEST_RESPONSE_STALE_SECONDS = 30
    }
}
