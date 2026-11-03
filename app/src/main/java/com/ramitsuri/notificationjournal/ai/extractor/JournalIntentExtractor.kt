package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.JournalIntent

interface JournalIntentExtractor {
    suspend fun isAvailable(): Boolean

    suspend fun extractIntent(userInput: String): JournalIntent?

    fun release()
}
