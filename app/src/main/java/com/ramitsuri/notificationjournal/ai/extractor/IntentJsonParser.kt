package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.AddEntryIntent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object IntentJsonParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parseJson(
        raw: String,
        originalInput: String,
    ): AddEntryIntent? =
        try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if ((start != -1) && (end != -1) && (end > start)) {
                val jsonStr = raw.substring(start, end + 1)
                val jsonElement = json.parseToJsonElement(jsonStr)
                if (jsonElement is JsonObject) {
                    val rawText =
                        jsonElement["entryText"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.takeIf { it.isNotBlank() && it != "null" }
                    val entryText = rawText ?: originalInput

                    val targetDate =
                        jsonElement["targetDate"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.takeIf { it.isNotBlank() && it != "null" }
                    val targetTime =
                        jsonElement["targetTime"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.takeIf { it.isNotBlank() && it != "null" }

                    AddEntryIntent(
                        entryText = entryText,
                        targetDate = targetDate,
                        targetTime = targetTime,
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
}
