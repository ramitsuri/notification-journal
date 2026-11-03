package com.ramitsuri.notificationjournal.ai.extractor

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import java.time.LocalDate

class NanoJournalIntentExtractor : JournalIntentExtractor {
    private val generativeModel: GenerativeModel = Generation.getClient()

    override suspend fun isAvailable(): Boolean =
        try {
            generativeModel.checkStatus() == FeatureStatus.AVAILABLE
        } catch (e: Exception) {
            false
        }

    override suspend fun extractIntent(userInput: String): JournalIntent? {
        val today = LocalDate.now().toString()
        val prompt =
            """
            You are a personal journal assistant.
            Today's date is $today.
            Analyze the user's input and extract their structured intent:
            "$userInput"
            """.trimIndent()

        val baseRequest = GenerateContentRequest.Builder(TextPart(prompt)).build()
        val typedRequest =
            generateTypedContentRequest(
                generateContentRequest = baseRequest,
                outputClass = JournalIntent::class,
            )

        val response = generativeModel.generateContent(typedRequest)
        return response.candidates.firstOrNull()?.response
    }

    override fun release() {
        generativeModel.close()
    }
}
