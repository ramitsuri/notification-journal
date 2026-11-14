package com.ramitsuri.notificationjournal.ai.extractor

import co.touchlab.kermit.Logger
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.ramitsuri.notificationjournal.ai.model.AddEntryIntent
import com.ramitsuri.notificationjournal.ai.model.ExtractionResult
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.PrefManager

private const val TAG = "JournalAI"

class NanoAddEntryIntentExtractor(
    private val prefManager: PrefManager? = null,
    private val fallbackExtractor: AddEntryIntentExtractor = RuleBasedAddEntryIntentExtractor(),
) : AddEntryIntentExtractor {
    private val generativeModel: GenerativeModel = Generation.getClient()

    override suspend fun isAvailable(): Boolean =
        try {
            generativeModel.checkStatus() == FeatureStatus.AVAILABLE
        } catch (e: Exception) {
            Logger.withTag(TAG).w(e) { "checkStatus failed in isAvailable" }
            false
        }

    override suspend fun extractIntent(userInput: String): ExtractionResult<AddEntryIntent>? {
        val customRules = prefManager?.getCustomPromptRules()

        var statusCheckError: Exception? = null
        val status =
            try {
                generativeModel.checkStatus()
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Error checking Gemini Nano status" }
                statusCheckError = e
                FeatureStatus.UNAVAILABLE
            }

        Logger.withTag(TAG).d { "Gemini Nano status: $status (AVAILABLE=${FeatureStatus.AVAILABLE})" }

        if (status == FeatureStatus.DOWNLOADABLE) {
            Logger.withTag(TAG).i { "Gemini Nano is DOWNLOADABLE. Initiating model download..." }
            try {
                generativeModel.download().collect { downloadStatus ->
                    Logger.withTag(TAG).d { "Download progress: $downloadStatus" }
                }
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Failed to initiate model download" }
            }
        }

        var fallbackReason = "Unknown"
        var fallbackThrowable: Throwable? = null
        var fallbackRawResponse: String? = null

        if (status == FeatureStatus.AVAILABLE) {
            try {
                Logger.withTag(TAG).d { "Attempting Gemini Nano extraction..." }
                val prompt = JournalPrompts.getAddEntryPrompt(userInput, customRules = customRules)

                val request =
                    GenerateContentRequest
                        .Builder(TextPart(prompt))
                        .apply {
                            maxOutputTokens = 1024
                            temperature = 0.0f
                        }.build()

                val textResponse = generativeModel.generateContent(request)
                val responseText = textResponse.candidates.firstOrNull()?.text
                Logger.withTag(TAG).d { "Gemini Nano text response: $responseText" }
                if (!responseText.isNullOrBlank()) {
                    val parsed = IntentJsonParser.parseJson(responseText, userInput)
                    if (parsed != null) {
                        Logger.withTag(TAG).d { "Gemini Nano parsing succeeded: $parsed" }
                        return ExtractionResult(
                            intent = parsed,
                            engine = "Gemini Nano",
                        )
                    } else {
                        Logger.withTag(TAG).w { "Could not parse JSON from: $responseText" }
                        fallbackReason = "Could not parse JSON from Gemini Nano response"
                        fallbackRawResponse = responseText
                    }
                } else {
                    fallbackReason = "Gemini Nano returned empty or blank response"
                }
            } catch (e: Exception) {
                Logger.withTag(TAG).w(e) { "Gemini Nano generation failed" }
                fallbackReason = "Gemini Nano generation failed: ${e.message}"
                fallbackThrowable = e
            }
        } else {
            Logger.withTag(TAG).w { "Gemini Nano status ($status) is not AVAILABLE. Using fallback extractor." }
            fallbackReason =
                if (statusCheckError != null) {
                    "Error checking Gemini Nano status: ${statusCheckError.message}"
                } else {
                    "Gemini Nano status ($status) is not AVAILABLE"
                }
            fallbackThrowable = statusCheckError
        }

        // Fallback to rule-based extractor
        val logMessage =
            buildString {
                append("Reason: ").append(fallbackReason)
                append("\nInput: \"").append(userInput).append("\"")
                if (fallbackRawResponse != null) {
                    append("\nRaw Response: ").append(fallbackRawResponse)
                }
            }
        Logger.withTag(Constants.TAG_AI_FALLBACK).w(fallbackThrowable) { logMessage }

        Logger.withTag(TAG).d { "Using RuleBasedAddEntryIntentExtractor for: $userInput" }
        return fallbackExtractor.extractIntent(userInput)
    }

    override fun release() {
        try {
            generativeModel.close()
        } catch (e: Exception) {
            Logger.withTag(TAG).w(e) { "Error closing generative model" }
        }
        fallbackExtractor.release()
    }
}
