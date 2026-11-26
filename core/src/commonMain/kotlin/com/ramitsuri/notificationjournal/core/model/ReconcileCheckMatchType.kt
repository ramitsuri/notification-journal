package com.ramitsuri.notificationjournal.core.model

/** Matching rule evaluated against a DayGroup's entries for a ReconcileCheck's tag. */
enum class ReconcileCheckMatchType {
    ANY_ENTRY,
    STARTS_WITH,
    CONTAINS_ALL_WORDS,
}
