package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.ExtractionResult

interface JournalIntentExtractor {
    suspend fun isAvailable(): Boolean

    suspend fun extractIntent(userInput: String): ExtractionResult?

    fun release()
}
