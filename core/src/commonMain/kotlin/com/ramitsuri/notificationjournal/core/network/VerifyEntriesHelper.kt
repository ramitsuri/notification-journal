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
                            val verification = repository.getVerificationForDate(request.date)
                            if (verification == null) {
                                log { "Unable to compute verification for ${request.date}" }
                                return@collect
                            }
                            dataSendHelper.sendVerifyEntriesResponse(
                                verification = verification,
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
        val verification = repository.getVerificationForDate(date)
        if (verification == null) {
            log { "Unable to compute verification for date $date" }
            return null
        }
        val sent =
            dataSendHelper.sendVerifyEntriesRequest(
                verification = verification,
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
        val unmatchedEntries = verification.unmatchedEntries(response.verification)
        if (unmatchedEntries.isNotEmpty()) {
            log {
                "Verify entries response doesn't match for date $date\n" +
                    "Unmatched entries: ${
                        unmatchedEntries.joinToString(
                            prefix = "\n  ",
                            separator = "\n  ",
                        ) { it.tag + ": " + it.text.take(25) }
                    }"
            }
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
