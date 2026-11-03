package com.ramitsuri.notificationjournal.ai.prompt

import java.time.LocalDate

object JournalPrompts {
    fun getStructuredOutputPrompt(
        userInput: String,
        today: String = LocalDate.now().toString(),
    ): String =
        """
        You are a personal journal assistant.
        Today's date is $today.
        Extract the user's intent. If adding an entry, entryText must ONLY contain the journal entry content, stripping out command words like 'add a new entry that', 'log that', etc.
        User input: "$userInput"
        """.trimIndent()

    fun getJsonPrompt(
        userInput: String,
        today: String = LocalDate.now().toString(),
    ): String =
        """
        You are a personal journal assistant. Today's date is $today.
        Analyze the user's input: "$userInput"

        If the user wants to add an entry:
        - action: "ADD_ENTRY"
        - entryText: extract ONLY the actual journal content without command phrases like "add a new entry that", "log that", "write down that", etc.
        - tag: "Personal"
        - conversationalReply: "Saved to your journal."

        If searching entries:
        - action: "QUERY_ENTRIES"
        - targetDate: "YYYY-MM-DD" if a date is mentioned, else null
        - searchKeyword: the topic or null
        - conversationalReply: "Searching your journal..."

        Respond with ONLY a single raw JSON object (no markdown, no code blocks):
        {
          "action": "ADD_ENTRY",
          "entryText": "...",
          "tag": "Personal",
          "targetDate": null,
          "searchKeyword": null,
          "conversationalReply": "..."
        }
        """.trimIndent()
}
