package com.ramitsuri.notificationjournal.ai.model

data class ExtractionResult<T>(
    val intent: T,
    val engine: String,
)
