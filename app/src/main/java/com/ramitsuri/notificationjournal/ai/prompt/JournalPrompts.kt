package com.ramitsuri.notificationjournal.ai.prompt

import java.time.LocalDate

object JournalPrompts {
    fun getStructuredOutputPrompt(
        userInput: String,
        today: String = LocalDate.now().toString(),
        customRules: String? = null,
    ): String =
        buildPrompt(
            userInput = userInput,
            today = today,
            customRules = customRules,
            isJsonMode = false,
        )

    fun getJsonPrompt(
        userInput: String,
        today: String = LocalDate.now().toString(),
        customRules: String? = null,
    ): String =
        buildPrompt(
            userInput = userInput,
            today = today,
            customRules = customRules,
            isJsonMode = true,
        )

    private fun buildPrompt(
        userInput: String,
        today: String,
        customRules: String?,
        isJsonMode: Boolean,
    ): String {
        val formattedCustomRules = formatCustomRules(customRules)
        val customRulesSection =
            if (formattedCustomRules.isNotBlank()) {
                "\n\nAdditional User Custom Rules:\n$formattedCustomRules"
            } else {
                ""
            }

        val basePrompt =
            """
            You are a personal journal assistant specialized in adding journal entries. Today's date is $today.
            Analyze the user's input: "$userInput"

            Rules:
            - entryText: extract ONLY the actual journal content without command phrases like "add a new entry that", "log that", "write down that", "note that", "journal that", "record that", etc. If the user spoke a thought, reflection, activity, or note, that is the entryText.
            - When the user says "dinner at time" or says "at time" after an activity (e.g., "dinner at time"), parse it as "{{time}}" in entryText (e.g., "dinner at {{time}}").
            - tag: null (do not set a tag; leave as null so entries remain untagged for review).
            - targetDate: "YYYY-MM-DD" if a specific date is mentioned (e.g., yesterday). Otherwise "$today".
            - targetTime: "HH:mm" in 24-hour format if a specific time of day is mentioned in the input (e.g., "8pm" -> "20:00", "7:30 am" -> "07:30", "at 9" -> "09:00" or "21:00"). null if no specific time of day is mentioned.
            - conversationalReply: a short confirmation to the user (e.g., "Saved to your journal: ...").$customRulesSection
            """.trimIndent()

        return if (isJsonMode) {
            """
            $basePrompt

            Respond with ONLY a single raw JSON object (no markdown, no code blocks):
            {
              "entryText": "...",
              "tag": null,
              "targetDate": "$today",
              "targetTime": null,
              "conversationalReply": "Saved to your journal."
            }
            """.trimIndent()
        } else {
            basePrompt
        }
    }

    private fun formatCustomRules(customRules: String?): String {
        if (customRules.isNullOrBlank()) return ""
        return customRules
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                if (line.startsWith("-") || line.startsWith("*")) {
                    line
                } else {
                    "- $line"
                }
            }.joinToString("\n")
    }
}
