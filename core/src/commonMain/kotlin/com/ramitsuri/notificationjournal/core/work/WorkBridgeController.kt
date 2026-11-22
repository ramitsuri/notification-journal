package com.ramitsuri.notificationjournal.core.work

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.EncryptionHelper
import com.ramitsuri.notificationjournal.core.utils.Key
import com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
import com.ramitsuri.notificationjournal.core.utils.WorkProfileManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WorkBridgeController(
    private val repository: JournalRepository,
    private val workProfileManager: WorkProfileManager,
    private val keyValueStore: KeyValueStoreV2,
    private val json: Json,
    private val httpClient: HttpClient =
        HttpClient {
            install(WebSockets) {
                pingIntervalMillis = 20_000
            }
        },
    private val retryDelay: Duration = 30.seconds,
    private val maxRetries: Int = 3,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onStopRequested: () -> Unit = {},
) {
    sealed interface State {
        data object Idle : State

        data class Connecting(
            val attempt: Int,
            val maxAttempts: Int,
        ) : State

        data object Connected : State

        data object Stopped : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var isRunning: Boolean = false

    suspend fun start() =
        withContext(ioDispatcher) {
            if (isRunning) {
                Logger.withTag(TAG).d { "WorkBridgeController already started" }
                return@withContext
            }
            isRunning = true
            try {
                runConnectionLoop()
            } finally {
                isRunning = false
                _state.value = State.Stopped
            }
        }

    private suspend fun runConnectionLoop() =
        coroutineScope {
            val profile = workProfileManager.getProfile()
            if (profile == null || !profile.isValid()) {
                Logger.withTag(TAG).w { "Cannot start work bridge, no valid work profile" }
                _state.value = State.Stopped
                onStopRequested()
                return@coroutineScope
            }

            val workEncryptionHelper =
                EncryptionHelper(
                    keyValueStore = keyValueStore,
                    passwordKey = Key.WORK_PASSWORD,
                    saltKey = Key.WORK_SALT,
                )

            var attempts = 0
            var wasConnected = false

            while (currentCoroutineContext().isActive) {
                attempts++
                _state.value = State.Connecting(attempts, maxRetries)

                val socketHelper =
                    WebSocketHelper(
                        httpClient = httpClient,
                        json = json,
                        encryptionHelper = workEncryptionHelper,
                        port = profile.port,
                    )

                socketHelper.setEntityHandler { entity ->
                    // Strictly one-way: only ingest entries via dedicated work ingestion
                    if (entity is Entity.Entries) {
                        repository.handleWorkPayload(entity)
                    }
                }

                val observerJob =
                    launch {
                        socketHelper.isConnected.collect { connected ->
                            if (connected) {
                                _state.value = State.Connected
                                wasConnected = true
                            } else if (wasConnected) {
                                _state.value = State.Connecting(1, maxRetries)
                            }
                        }
                    }

                try {
                    socketHelper.start(host = profile.host, port = profile.port)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.withTag(TAG).e(e) { "Work bridge socket session ended with error" }
                } finally {
                    observerJob.cancel()
                }

                currentCoroutineContext().ensureActive()

                if (wasConnected) {
                    attempts = 0
                    wasConnected = false
                }

                if (attempts >= maxRetries) {
                    Logger.withTag(TAG).i { "Work bridge reached max retries ($maxRetries), requesting stop" }
                    _state.value = State.Stopped
                    onStopRequested()
                    break
                }

                _state.value = State.Connecting(attempts + 1, maxRetries)
                delay(retryDelay)
            }
        }

    companion object {
        private const val TAG = "WorkBridgeController"
    }
}
