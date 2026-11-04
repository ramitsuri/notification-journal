package com.ramitsuri.notificationjournal.ai.model

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

@Generable("Represents user intent when adding or querying journal entries")
data class JournalIntent(
    @Guide(
        description =
            "Action to take. ADD_ENTRY to write a new entry, " +
                "QUERY_ENTRIES to search past entries, or CONVERSE for general chit-chat",
        enumValues = ["ADD_ENTRY", "QUERY_ENTRIES", "CONVERSE"],
    )
    val action: String = "CONVERSE",
    @Guide(description = "The narrative text content of the journal entry if action is ADD_ENTRY")
    val entryText: String? = null,
    @Guide(
        description =
            "Tag or category. Do not set a tag for new entries (leave as null) so they remain untagged for user review.",
    )
    val tag: String? = null,
    @Guide(
        description =
            "Target date in ISO format YYYY-MM-DD (e.g., 'yesterday', 'October 24th', or today's date).",
    )
    val targetDate: String? = null,
    @Guide(description = "Search keyword or topic if action is QUERY_ENTRIES")
    val searchKeyword: String? = null,
    @Guide(description = "A friendly conversational reply confirming the action to the user.")
    val conversationalReply: String? = null,
)
