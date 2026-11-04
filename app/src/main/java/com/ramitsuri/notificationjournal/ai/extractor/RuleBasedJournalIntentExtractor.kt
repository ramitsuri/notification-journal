package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.ExtractionResult
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import java.time.LocalDate

/**
 * A deterministic, rule-based intent extractor using pattern matching.
 * Used on emulators without AICore/Gemini Nano and as a reliable offline fallback.
 */
class RuleBasedJournalIntentExtractor : JournalIntentExtractor {
    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractIntent(userInput: String): ExtractionResult {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        val intent =
            when {
                lower.contains("find") || lower.contains("what did") || lower.contains("search") -> {
                    val keyword =
                        if (!lower.contains("yesterday")) {
                            trimmed.replace(Regex("(?i)^(find|search|show me|look for)\\s*"), "").trim()
                        } else {
                            null
                        }
                    JournalIntent(
                        action = "QUERY_ENTRIES",
                        targetDate = if (lower.contains("yesterday")) LocalDate.now().minusDays(1).toString() else null,
                        searchKeyword = keyword.takeIf { !it.isNullOrBlank() },
                        conversationalReply = "Searching your journal...",
                    )
                }
                else -> {
                    val cleanedText = trimmed.replace(ADD_PREFIX_REGEX, "").trim()
                    val entryText = if (cleanedText.isNotBlank()) cleanedText else trimmed
                    JournalIntent(
                        action = "ADD_ENTRY",
                        entryText = entryText,
                        tag = null,
                        conversationalReply = "Added new journal entry.",
                    )
                }
            }

        return ExtractionResult(
            intent = intent,
            engine = "Rule-based fallback",
        )
    }

    override fun release() {}

    companion object {
        private val ADD_PREFIX_REGEX =
            Regex(
                "(?i)^(please\\s+)?((add|log|record|journal|write\\s+down|create|make|note)(\\s+(a|an))?(\\s+new)?(\\s+(journal\\s+)?(entry|note))?|(new\\s+(journal\\s+)?(entry|note)))(\\s+(that|saying|about|for))?\\s*",
            )
    }
}
