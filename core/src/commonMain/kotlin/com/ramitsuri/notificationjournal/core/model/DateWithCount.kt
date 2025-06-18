package com.ramitsuri.notificationjournal.core.model

import kotlinx.datetime.LocalDate

data class DateWithCount(
    val date: LocalDate,
    val conflictCount: Int,
    val untaggedCount: Int,
    val verifiedWith: String? = null,
)
