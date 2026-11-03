package com.ramitsuri.notificationjournal.ai.extractor

import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import java.time.LocalDate

class FakeJournalIntentExtractor : JournalIntentExtractor {
    override suspend fun isAvailable(): Boolean = true

    override suspend fun extractIntent(userInput: String): JournalIntent {
        val lower = userInput.lowercase()
        return when {
            lower.contains("find") || lower.contains("what did") || lower.contains("search") -> {
                JournalIntent(
                    action = "QUERY_ENTRIES",
                    targetDate = if (lower.contains("yesterday")) LocalDate.now().minusDays(1).toString() else null,
                    searchKeyword =
                        if (!lower.contains(
                                "yesterday",
                            )
                        ) {
                            userInput.replace(Regex("(?i)find|search"), "").trim()
                        } else {
                            null
                        },
                    conversationalReply = "Searching your journal...",
                )
            }
            else -> {
                JournalIntent(
                    action = "ADD_ENTRY",
                    entryText = userInput.replace(Regex("(?i)^(log|journal|write down)\\s*"), "").trim(),
                    tag = "Personal",
                    conversationalReply = "Added new journal entry.",
                )
            }
        }
    }

    override fun release() {}
}
