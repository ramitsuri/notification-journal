package com.ramitsuri.notificationjournal.core.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class ReceivedTextProperties(
    val text: String?,
    val tag: String? = null,
)

@OptIn(ExperimentalContracts::class)
fun ReceivedTextProperties?.hasValues(): Boolean {
    contract { returns(true) implies (this@hasValues != null) }
    return this != null && !text.isNullOrEmpty()
}
