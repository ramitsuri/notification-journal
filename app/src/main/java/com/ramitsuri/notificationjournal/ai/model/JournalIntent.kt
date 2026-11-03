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
    @Guide(description = "Optional tag or category (e.g. Work, Personal, Idea) if action is ADD_ENTRY")
    val tag: String? = null,
    @Guide(
        description =
            "Target date in ISO format YYYY-MM-DD if mentioned " +
                "(e.g., 'yesterday', 'October 24th', 'last Monday')",
    )
    val targetDate: String? = null,
    @Guide(description = "Search keyword or topic if action is QUERY_ENTRIES")
    val searchKeyword: String? = null,
    @Guide(description = "Conversational reply to show to the user")
    val conversationalReply: String? = null,
)
