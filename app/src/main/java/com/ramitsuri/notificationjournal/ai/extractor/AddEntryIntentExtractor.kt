package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.AddEntryIntent
import com.ramitsuri.notificationjournal.ai.model.ExtractionResult

interface AddEntryIntentExtractor {
    suspend fun isAvailable(): Boolean

    suspend fun extractIntent(userInput: String): ExtractionResult<AddEntryIntent>?

    fun release()
}
