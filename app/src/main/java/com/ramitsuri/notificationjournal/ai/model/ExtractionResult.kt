package com.ramitsuri.notificationjournal.ai.model

data class ExtractionResult(
    val intent: JournalIntent,
    val engine: String,
)
