package com.ramitsuri.notificationjournal.core.sync

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.IoDispatcher
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.minutes

@SingleIn(AppScope::class)
@Inject
class AppLifecycleCoordinator(
    private val coroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val webSocketHelper: WebSocketHelper,
    private val verifyEntriesHelper: VerifyEntriesHelper,
    private val dataReceiveHelper: DataReceiveHelper,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
    private val prefManager: PrefManager,
) {
    private var receiveJob: Job? = null
    private var appStopJob: Job? = null

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
                    receiveJob?.cancel()
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
            startReceiving()
        }
    }

    private suspend fun startReceiving() =
        supervisorScope {
            Logger.i(TAG) { "Start receiving" }
            receiveJob?.cancel()
            receiveJob =
                launch {
                    dataReceiveHelper.payloadFlow.filterIsInstance<Entity>().collect { entity ->
                        when (entity) {
                            is Entity.Entries -> {
                                coroutineScope.launch { repository.handlePayload(entity) }
                            }

                            is Entity.Tags -> {
                                coroutineScope.launch { tagsDao.clearAndInsert(entity.data) }
                            }

                            is Entity.Templates -> {
                                coroutineScope.launch { templatesDao.clearAndInsert(entity.data) }
                            }

                            is Entity.ClearDaysAndInsertEntries -> {
                                coroutineScope.launch {
                                    repository.clearDaysAndInsert(
                                        days = entity.days,
                                        entries = entity.entries,
                                        uploadEntries = false,
                                    )
                                }
                            }
                        }
                    }
                }
        }

    companion object {
        private const val TAG = "AppLifecycleCoordinator"
    }
}
