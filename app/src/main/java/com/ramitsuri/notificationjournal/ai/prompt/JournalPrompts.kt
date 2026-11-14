package com.ramitsuri.notificationjournal.ai.prompt

import java.time.LocalDate

object JournalPrompts {
    fun getAddEntryPrompt(
        userInput: String,
        today: LocalDate = LocalDate.now(),
        customRules: String? = null,
    ): String {
        val yesterday = today.minusDays(1)
        val formattedCustomRules = formatCustomRules(customRules)
        val customRulesSection =
            if (formattedCustomRules.isNotBlank()) {
                "\n\nAdditional Rules:\n$formattedCustomRules"
            } else {
                ""
            }
        val sanitizedInput = userInput.replace("\"", "'")

        val prompt =
            """
            Extract journal entry details from the user input and return ONLY valid JSON.

            Context:
            - Today: $today
            - Yesterday: $yesterday

            Schema:
            {
              "entryText": "string",
              "targetDate": "YYYY-MM-DD" | null,
              "targetTime": "HH:mm" | null
            }

            Rules:
            1. entryText: Keep only the core thought or activity. Strip command prefixes ("add a new entry that", "log that", "note that", "write down").
            2. Literal time placeholder: If the input literally contains the words "at time" (e.g., "dinner at time"), write "{{time}}" in entryText ("dinner at {{time}}") and set targetTime to null.
            3. targetDate: Convert relative dates using Context (e.g., "today" -> "$today", "yesterday" -> "$yesterday"). Use null if no date is mentioned.
            4. targetTime: Convert mentioned clock times to 24-hour "HH:mm" (e.g., "8pm" -> "20:00", "7:30 am" -> "07:30"). Use null if no clock time is mentioned.$customRulesSection

            Examples:

            Input: "add a new entry that I slept well"
            Output: {"entryText": "I slept well", "targetDate": null, "targetTime": null}

            Input: "log that I had dinner at 8pm yesterday"
            Output: {"entryText": "I had dinner at 8pm", "targetDate": "$yesterday", "targetTime": "20:00"}

            Input: "note that I had dinner at time"
            Output: {"entryText": "I had dinner at {{time}}", "targetDate": null, "targetTime": null}

            Input: "feeling super energized after my morning run"
            Output: {"entryText": "feeling super energized after my morning run", "targetDate": null, "targetTime": null}

            Input: "$sanitizedInput"
            Output:
            """.trimIndent()
        return prompt
    }

    private fun formatCustomRules(customRules: String?): String {
        if (customRules.isNullOrBlank()) return ""
        return customRules
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                if (line.startsWith("-") || line.startsWith("*")) line else "- $line"
            }.joinToString("\n")
    }
}
