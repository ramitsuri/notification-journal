package com.ramitsuri.notificationjournal.ai.model

data class AddEntryIntent(
    val entryText: String? = null,
    val targetDate: String? = null,
    val targetTime: String? = null,
)
