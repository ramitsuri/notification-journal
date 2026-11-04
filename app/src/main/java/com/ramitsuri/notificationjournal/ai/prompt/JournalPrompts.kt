package com.ramitsuri.notificationjournal.ai.prompt

import java.time.LocalDate

object JournalPrompts {
    fun getStructuredOutputPrompt(
        userInput: String,
        today: String = LocalDate.now().toString(),
    ): String =
        """
        You are a personal journal assistant. Today's date is $today.
        Analyze the user's input: "$userInput"

        Rules:
        - action: "ADD_ENTRY" to log a new entry, "QUERY_ENTRIES" to search past entries, else "CONVERSE".
        - entryText: extract ONLY the actual journal content without command phrases like "add a new entry that", "log that", "write down that", etc.
        - tag: null (do not set a tag; leave as null so entries remain untagged for review).
        - targetDate: "YYYY-MM-DD" if a specific date is mentioned (e.g., yesterday). For ADD_ENTRY without a date, use "$today".
        - conversationalReply: a short confirmation to the user (e.g., "Saved to your journal: ...").
        """.trimIndent()

    fun getJsonPrompt(
        userInput: String,
        today: String = LocalDate.now().toString(),
    ): String =
        """
        You are a personal journal assistant. Today's date is $today.
        Analyze the user's input: "$userInput"

        Rules:
        - action: "ADD_ENTRY" to log a new entry, "QUERY_ENTRIES" to search past entries, else "CONVERSE".
        - entryText: extract ONLY the actual journal content without command phrases like "add a new entry that", "log that", "write down that", etc.
        - tag: null (do not set a tag; leave as null so entries remain untagged for review).
        - targetDate: "YYYY-MM-DD" if a specific date is mentioned (e.g., yesterday). For ADD_ENTRY without a date, use "$today".
        - conversationalReply: a short confirmation to the user (e.g., "Saved to your journal: ...").

        Respond with ONLY a single raw JSON object (no markdown, no code blocks):
        {
          "action": "ADD_ENTRY",
          "entryText": "...",
          "tag": null,
          "targetDate": "$today",
          "searchKeyword": null,
          "conversationalReply": "Saved to your journal."
        }
        """.trimIndent()
}
