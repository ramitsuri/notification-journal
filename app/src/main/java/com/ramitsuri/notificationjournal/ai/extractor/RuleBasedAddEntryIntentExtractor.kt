package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.AddEntryIntent
import com.ramitsuri.notificationjournal.ai.model.ExtractionResult
import java.time.LocalDate

/**
 * A deterministic, rule-based intent extractor using pattern matching for adding entries.
 * Used on emulators without AICore/Gemini Nano and as a reliable offline fallback.
 */
class RuleBasedAddEntryIntentExtractor : AddEntryIntentExtractor {
    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractIntent(userInput: String): ExtractionResult<AddEntryIntent> {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        var cleanedText = trimmed.replace(ADD_PREFIX_REGEX, "").trim()
        if (cleanedText.isBlank()) cleanedText = trimmed

        val targetDate = if (lower.contains("yesterday")) LocalDate.now().minusDays(1).toString() else null
        val targetTime = extractTargetTime(cleanedText)
        cleanedText = cleanedText.replace(AT_TIME_REGEX, "at {{time}}")

        val intent =
            AddEntryIntent(
                entryText = cleanedText,
                tag = null,
                targetDate = targetDate,
                targetTime = targetTime,
                conversationalReply = "Added new journal entry.",
            )

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

        private val AT_TIME_REGEX = Regex("(?i)\\bat time\\b")

        private val TIME_REGEX =
            Regex("(?i)\\bat\\s+(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\b")

        fun extractTargetTime(input: String): String? {
            val match = TIME_REGEX.find(input) ?: return null
            val timeStr = match.groupValues[1]
            return parseTo24Hour(timeStr)
        }

        private fun parseTo24Hour(input: String): String? {
            val trimmed = input.trim()
            val colonMatch = Regex("""^(\d{1,2}):(\d{2})\s*(am|pm)?$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
            if (colonMatch != null) {
                var hour = colonMatch.groupValues[1].toInt()
                val minute = colonMatch.groupValues[2].toInt()
                val amPm = colonMatch.groupValues[3].lowercase()
                if ((amPm == "pm") && (hour < 12)) hour += 12
                if ((amPm == "am") && (hour == 12)) hour = 0
                return String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
            }
            val hourOnlyMatch = Regex("""^(\d{1,2})\s*(am|pm)$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
            if (hourOnlyMatch != null) {
                var hour = hourOnlyMatch.groupValues[1].toInt()
                val amPm = hourOnlyMatch.groupValues[2].lowercase()
                if ((amPm == "pm") && (hour < 12)) hour += 12
                if ((amPm == "am") && (hour == 12)) hour = 0
                return String.format(java.util.Locale.US, "%02d:00", hour)
            }
            return null
        }
    }
}
