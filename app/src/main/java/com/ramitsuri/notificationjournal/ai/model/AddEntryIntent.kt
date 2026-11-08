package com.ramitsuri.notificationjournal.ai.model

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

@Generable("Represents user intent when adding a journal entry")
data class AddEntryIntent(
    @Guide(description = "The narrative text content of the journal entry.")
    val entryText: String? = null,
    @Guide(
        description =
            "Tag or category. Do not set a tag for new entries (leave as null) so they remain untagged for user review.",
    )
    val tag: String? = null,
    @Guide(
        description =
            "Target date in ISO format YYYY-MM-DD if a specific date is mentioned (e.g., 'yesterday', 'October 24th'). null if no specific date is mentioned.",
    )
    val targetDate: String? = null,
    @Guide(
        description =
            "Target time in 24-hour format HH:mm (e.g., '18:30', '08:00') if a specific time of day is mentioned. null if no specific time of day is mentioned.",
    )
    val targetTime: String? = null,
    @Guide(description = "A friendly conversational reply confirming the action to the user.")
    val conversationalReply: String? = null,
)
