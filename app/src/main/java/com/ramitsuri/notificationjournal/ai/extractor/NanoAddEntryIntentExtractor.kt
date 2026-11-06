package com.ramitsuri.notificationjournal.ai.extractor

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Feature
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.ramitsuri.notificationjournal.ai.model.AddEntryIntent
import com.ramitsuri.notificationjournal.ai.model.ExtractionResult
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import org.json.JSONObject
import java.time.LocalDate

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
            Log.w(TAG, "checkStatus failed in isAvailable", e)
            false
        }

    private suspend fun isStructuredOutputSupported(): Boolean =
        try {
            val features = generativeModel.getAvailableFeatures()
            Log.d(TAG, "AICore available features: $features")
            features.contains(Feature.STRUCTURED_OUTPUT)
        } catch (e: Exception) {
            Log.w(TAG, "getAvailableFeatures failed", e)
            false
        }

    override suspend fun extractIntent(userInput: String): ExtractionResult<AddEntryIntent>? {
        val today = LocalDate.now().toString()
        val customRules = prefManager?.getCustomPromptRules()

        val status =
            try {
                generativeModel.checkStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Gemini Nano status", e)
                FeatureStatus.UNAVAILABLE
            }

        Log.d(TAG, "Gemini Nano status: $status (AVAILABLE=${FeatureStatus.AVAILABLE})")

        if (status == FeatureStatus.DOWNLOADABLE) {
            Log.i(TAG, "Gemini Nano is DOWNLOADABLE. Initiating model download...")
            try {
                generativeModel.download().collect { downloadStatus ->
                    Log.d(TAG, "Download progress: $downloadStatus")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate model download", e)
            }
        }

        if (status == FeatureStatus.AVAILABLE) {
            // 1. Try structured output if supported by AICore
            if (isStructuredOutputSupported()) {
                try {
                    Log.d(TAG, "Attempting Structured Output extraction...")
                    val prompt = JournalPrompts.getAddEntryStructuredOutputPrompt(userInput, today, customRules)

                    val baseRequest =
                        GenerateContentRequest
                            .Builder(TextPart(prompt))
                            .apply {
                                maxOutputTokens = 1024
                                temperature = 0.0f
                            }.build()
                    val typedRequest =
                        generateTypedContentRequest(
                            generateContentRequest = baseRequest,
                            outputClass = AddEntryIntent::class,
                        )

                    val response = generativeModel.generateContent(typedRequest)
                    val typedResponse = response.candidates.firstOrNull()?.response
                    if (typedResponse != null) {
                        val normalizedIntent =
                            typedResponse.copy(
                                entryText = typedResponse.entryText?.ifBlank { userInput } ?: userInput,
                            )
                        Log.d(TAG, "Structured output succeeded: $normalizedIntent")
                        return ExtractionResult(
                            intent = normalizedIntent,
                            engine = "Gemini Nano (Structured Output)",
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Structured output failed; falling back to JSON prompt", e)
                }
            } else {
                Log.d(TAG, "Structured output not supported on this model build. Using JSON prompt...")
            }

            // 2. Unstructured JSON prompt with Gemini Nano
            try {
                Log.d(TAG, "Attempting JSON prompt extraction...")
                val jsonPrompt = JournalPrompts.getAddEntryJsonPrompt(userInput, today, customRules)

                val jsonRequest =
                    GenerateContentRequest
                        .Builder(TextPart(jsonPrompt))
                        .apply {
                            maxOutputTokens = 1024
                            temperature = 0.0f
                        }.build()

                val textResponse = generativeModel.generateContent(jsonRequest)
                val responseText = textResponse.candidates.firstOrNull()?.text
                Log.d(TAG, "Gemini Nano text response: $responseText")
                if (!responseText.isNullOrBlank()) {
                    val parsed = parseJson(responseText, userInput)
                    if (parsed != null) {
                        Log.d(TAG, "JSON prompt parsing succeeded: $parsed")
                        return ExtractionResult(
                            intent = parsed,
                            engine = "Gemini Nano (Prompted)",
                        )
                    } else {
                        Log.w(TAG, "Could not parse JSON from: $responseText")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "JSON prompt generation failed", e)
            }
        } else {
            Log.w(TAG, "Gemini Nano status ($status) is not AVAILABLE. Using fallback extractor.")
        }

        // 3. Fallback to rule-based extractor
        Log.d(TAG, "Using RuleBasedAddEntryIntentExtractor for: $userInput")
        return fallbackExtractor.extractIntent(userInput)
    }

    private fun parseJson(
        raw: String,
        originalInput: String,
    ): AddEntryIntent? =
        try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if ((start != -1) && (end != -1) && (end > start)) {
                val jsonStr = raw.substring(start, end + 1)
                val jsonObject = JSONObject(jsonStr)
                val rawText = jsonObject.optString("entryText").takeIf { (it.isNotBlank()) && (it != "null") }
                val entryText = rawText ?: originalInput
                AddEntryIntent(
                    entryText = entryText,
                    tag = jsonObject.optString("tag").takeIf { (it.isNotBlank()) && (it != "null") },
                    targetDate = jsonObject.optString("targetDate").takeIf { (it.isNotBlank()) && (it != "null") },
                    targetTime = jsonObject.optString("targetTime").takeIf { (it.isNotBlank()) && (it != "null") },
                    conversationalReply =
                        jsonObject.optString("conversationalReply").takeIf {
                            (it.isNotBlank()) && (it != "null")
                        },
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

    override fun release() {
        try {
            generativeModel.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing generative model", e)
        }
        fallbackExtractor.release()
    }
}
