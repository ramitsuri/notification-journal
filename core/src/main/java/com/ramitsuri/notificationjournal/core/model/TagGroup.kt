package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagGroup(
    @Json(name = "tag")
    val tag: String,

    @Json(name = "entries")
    val entries: List<JournalEntry>
)
