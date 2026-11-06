package com.ramitsuri.notificationjournal.di

import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.PlatformConfig
import com.ramitsuri.notificationjournal.core.utils.JournalEntryNotificationHelper
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object WearBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun providePlatformConfig(): PlatformConfig =
        PlatformConfig(
            isDesktop = false,
            allowJournalImport = false,
            actsAsServer = false,
            allowJournalEntryNotify = false,
            ipAddress = null,
        )

    @Provides
    fun provideJournalEntryNotificationHelper(): JournalEntryNotificationHelper? = null
}
