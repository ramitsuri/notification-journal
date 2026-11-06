package com.ramitsuri.notificationjournal.core.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.ramitsuri.notificationjournal.core.log.InMemoryLogWriter
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.sync.AppLifecycleCoordinator
import com.ramitsuri.notificationjournal.core.ui.nav.NavViewModelFactories
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import kotlinx.coroutines.CoroutineScope

interface CoreGraph : NavViewModelFactories {
    val repository: JournalRepository
    val prefManager: PrefManager
    val lifecycleCoordinator: AppLifecycleCoordinator
    val coroutineScope: CoroutineScope
    val notificationHandler: NotificationHandler
    val platformConfig: PlatformConfig
    val isDesktop: Boolean get() = platformConfig.isDesktop
    val inMemoryLogWriter: InMemoryLogWriter

    fun initLogger() {
        Logger.setLogWriters(inMemoryLogWriter, platformLogWriter())
    }
}
