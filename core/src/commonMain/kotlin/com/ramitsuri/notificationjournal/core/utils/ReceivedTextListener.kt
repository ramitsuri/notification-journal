package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Composable
expect fun ReceivedTextListener(
    navController: NavController,
    onTextReceived: (ReceivedTextProperties?) -> Unit,
)

data class ReceivedTextProperties(
    val text: String?,
    val tag: String? = null,
)

@OptIn(ExperimentalContracts::class)
fun ReceivedTextProperties?.hasValues(): Boolean {
    contract { returns(true) implies (this@hasValues != null) }
    return this != null && !text.isNullOrEmpty()
}
