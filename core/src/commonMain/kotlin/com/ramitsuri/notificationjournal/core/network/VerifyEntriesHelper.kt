package com.ramitsuri.notificationjournal.core.network

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
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
                                log { "Request is too old for ${request.date}" }
                                return@collect
                            }
                            log { "Request for ${request.date} from ${request.sender.name}" }
                            val hash = repository.getHashForDate(request.date)
                            if (hash == null) {
                                log { "Unable to compute hash for $date" }
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
        log { "Verifying for date $date" }
        val hash = repository.getHashForDate(date)
        if (hash == null) {
            log { "Unable to compute hash for date $date" }
            return null
        }
        val sent =
            dataSendHelper.sendVerifyEntriesRequest(
                hash = hash,
                time = clock.now(),
                date = date,
            )
        if (!sent) {
            log { "Verify entries request unable to send for date $date" }
            return null
        }
        val response =
            dataReceiveHelper
                .payloadFlow
                .filterIsInstance<VerifyEntries.Response>()
                .filter { it.date == date }
                .timeout(REQUEST_RESPONSE_STALE_SECONDS.seconds)
                .catch {
                    if (it is TimeoutCancellationException) {
                        log { "canceled from timeout for date $date" }
                    }
                }
                .firstOrNull()
        if (response == null) {
            log { "Verify entries response not received for date $date" }
            return null
        }
        if (clock.now() - response.time > REQUEST_RESPONSE_STALE_SECONDS.seconds) {
            log { "Verify entries response is too old for date $date" }
            return null
        }
        if (response.hash != hash) {
            log { "Verify entries response hash doesn't match for date $date" }
            return null
        }
        log { "Matches with ${response.sender.name} for date $date" }
        return response.sender.name
    }

    private fun log(message: () -> String) {
        Logger.i(TAG, message = message)
    }

    companion object {
        private const val TAG = "VerifyEntriesHelper"
        private const val REQUEST_RESPONSE_STALE_SECONDS = 30
    }
}
