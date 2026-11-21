package com.ramitsuri.notificationjournal.core.sync

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.IoDispatcher
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.SyncRequest
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.network.VerifyEntriesHelper
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SingleIn(AppScope::class)
@Inject
class AppLifecycleCoordinator(
    private val coroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val webSocketHelper: WebSocketHelper,
    private val verifyEntriesHelper: VerifyEntriesHelper,
    private val dataReceiveHelper: DataReceiveHelper,
    private val dataSendHelper: DataSendHelper,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
    private val prefManager: PrefManager,
) {
    private var appStopJob: Job? = null
    private val uploadMutex = Mutex()

    init {
        dataReceiveHelper.setEntityHandler(::handleEntity)
        setupReconnectSync()
        setupSyncRequestListener()
    }

    fun onAppStart() {
        Logger.i(TAG) { "app start" }
        appStopJob?.cancel()
        resetReceiveHelper()
        verifyEntriesHelper.start()
    }

    fun onAppStop() {
        Logger.i(TAG) { "app stop" }
        appStopJob =
            coroutineScope.launch {
                Logger.i(TAG) { "Will wait for 2 minutes before stopping jobs" }
                delay(2.minutes)
                Logger.i(TAG) { "Stopping jobs" }
                launch {
                    webSocketHelper.stop()
                }
                verifyEntriesHelper.stop()
            }
    }

    fun resetReceiveHelper(resetWebsocket: Boolean = false) {
        coroutineScope.launch {
            if (resetWebsocket || !webSocketHelper.isConnected.value) {
                Logger.i(TAG) { "Resetting websocket" }
                webSocketHelper.stop()
                launch {
                    webSocketHelper.start(prefManager.getDataHostProperties()::first)
                }
            }
        }
    }

    private suspend fun handleEntity(entity: Entity) {
        Logger.withTag(TAG).i { "Handling received entity: ${entity::class.simpleName}" }
        when (entity) {
            is Entity.Entries -> repository.handlePayload(entity)
            is Entity.Tags -> tagsDao.clearAndInsert(entity.data)
            is Entity.Templates -> templatesDao.clearAndInsert(entity.data)
            is Entity.ClearDaysAndInsertEntries -> {
                repository.clearDaysAndInsert(
                    days = entity.days,
                    entries = entity.entries,
                    uploadEntries = false,
                )
            }
        }
    }

    private fun setupReconnectSync() {
        coroutineScope.launch {
            webSocketHelper.isConnected
                .filter { it }
                .debounce(1.seconds)
                .collect {
                    try {
                        dataSendHelper.sendSyncRequest()
                        triggerUploadAll()
                    } catch (e: Exception) {
                        Logger.withTag(TAG).e(e) { "Failed during reconnect sync" }
                    }
                }
        }
    }

    private fun setupSyncRequestListener() {
        coroutineScope.launch {
            dataReceiveHelper.payloadFlow
                .filterIsInstance<SyncRequest>()
                .collect { request ->
                    try {
                        val localProperties = prefManager.getDataHostProperties().first()
                        if (request.sender.id != localProperties.deviceId) {
                            Logger.withTag(TAG).i {
                                "Received SyncRequest from ${request.sender.name}, triggering uploadAll"
                            }
                            coroutineScope.launch {
                                triggerUploadAll()
                            }
                        }
                    } catch (e: Exception) {
                        Logger.withTag(TAG).e(e) { "Failed handling SyncRequest" }
                    }
                }
        }
    }

    private suspend fun triggerUploadAll() {
        if (uploadMutex.tryLock()) {
            try {
                repository.uploadAll()
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Failed to upload on sync request" }
            } finally {
                uploadMutex.unlock()
            }
        }
    }

    companion object {
        private const val TAG = "AppLifecycleCoordinator"
    }
}
