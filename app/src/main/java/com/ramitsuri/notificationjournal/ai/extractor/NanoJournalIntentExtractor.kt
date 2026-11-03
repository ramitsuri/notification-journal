package com.ramitsuri.notificationjournal.ai.extractor

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Feature
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import org.json.JSONObject
import java.time.LocalDate

class NanoJournalIntentExtractor(
    private val fallbackExtractor: JournalIntentExtractor = FakeJournalIntentExtractor(),
) : JournalIntentExtractor {
    private val generativeModel: GenerativeModel = Generation.getClient()

    override suspend fun isAvailable(): Boolean =
        try {
            generativeModel.checkStatus() == FeatureStatus.AVAILABLE
        } catch (_: Exception) {
            false
        }

    private suspend fun isStructuredOutputSupported(): Boolean =
        try {
            generativeModel.getAvailableFeatures().contains(Feature.STRUCTURED_OUTPUT)
        } catch (_: Exception) {
            false
        }

    override suspend fun extractIntent(userInput: String): JournalIntent? {
        val today = LocalDate.now().toString()

        // 1. Try structured output if supported by AICore
        if (isStructuredOutputSupported()) {
            try {
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
                val typedResponse = response.candidates.firstOrNull()?.response
                if (typedResponse != null) {
                    return typedResponse
                }
            } catch (_: Exception) {
                // Structured output failed or unsupported on this AICore build; fallback to JSON prompt
            }
        }

        // 2. Unstructured JSON prompt with Gemini Nano
        try {
            val jsonPrompt =
                """
                You are a personal journal assistant. Today's date is $today.
                Analyze the user's input: "$userInput"

                Respond with ONLY a single raw JSON object (no markdown, no backticks, no other text) with the following structure:
                {
                  "action": "ADD_ENTRY" or "QUERY_ENTRIES" or "CONVERSE",
                  "entryText": "narrative entry text or null",
                  "tag": "Personal or null",
                  "targetDate": "YYYY-MM-DD or null",
                  "searchKeyword": "search keyword or null",
                  "conversationalReply": "short reply to user"
                }
                """.trimIndent()

            val textResponse = generativeModel.generateContent(jsonPrompt)
            val responseText = textResponse.candidates.firstOrNull()?.text
            if (!responseText.isNullOrBlank()) {
                val parsed = parseJson(responseText)
                if (parsed != null) {
                    return parsed
                }
            }
        } catch (_: Exception) {
            // Gemini Nano text generation failed; fallback to rule-based parser
        }

        // 3. Fallback to rule-based extractor
        return fallbackExtractor.extractIntent(userInput)
    }

    private fun parseJson(raw: String): JournalIntent? =
        try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if ((start != -1) && (end != -1) && (end > start)) {
                val jsonStr = raw.substring(start, end + 1)
                val jsonObject = JSONObject(jsonStr)
                JournalIntent(
                    action = jsonObject.optString("action", "CONVERSE"),
                    entryText = jsonObject.optString("entryText").takeIf { it.isNotBlank() && it != "null" },
                    tag = jsonObject.optString("tag").takeIf { it.isNotBlank() && it != "null" },
                    targetDate = jsonObject.optString("targetDate").takeIf { it.isNotBlank() && it != "null" },
                    searchKeyword = jsonObject.optString("searchKeyword").takeIf { it.isNotBlank() && it != "null" },
                    conversationalReply =
                        jsonObject.optString("conversationalReply").takeIf {
                            it.isNotBlank() && it != "null"
                        },
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

    override fun release() {
        generativeModel.close()
        fallbackExtractor.release()
    }
}
