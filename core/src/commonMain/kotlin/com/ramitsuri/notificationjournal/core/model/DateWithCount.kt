package com.ramitsuri.notificationjournal.core.model

import kotlinx.datetime.LocalDate

data class DateWithCount(
    val date: LocalDate,
    val conflictCount: Int,
    val untaggedCount: Int,
    val verification: Verification = Verification.NotVerified,
) {
    sealed interface Verification {
        data object InProgress : Verification

        data class Verified(val with: String) : Verification

        data object NotVerified : Verification
    }
}
