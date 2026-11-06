package com.ramitsuri.notificationjournal.core.di

data class PlatformConfig(
    val isDesktop: Boolean,
    val allowJournalImport: Boolean,
    val actsAsServer: Boolean = false,
    val allowJournalEntryNotify: Boolean = false,
    val ipAddress: String? = null,
)
