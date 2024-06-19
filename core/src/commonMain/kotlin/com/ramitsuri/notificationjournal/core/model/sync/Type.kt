package com.ramitsuri.notificationjournal.core.model.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Type {
    @SerialName("unknown")
    UNKNOWN,

    @SerialName("journal_entry")
    JOURNAL_ENTRY,

    @SerialName("tags")
    TAGS,

    @SerialName("templates")
    TEMPLATES,
    ;
}
