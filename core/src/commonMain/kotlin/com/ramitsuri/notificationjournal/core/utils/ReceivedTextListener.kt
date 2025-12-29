package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.ui.nav.Navigator
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Composable
expect fun ReceivedTextListener(navigator: Navigator)

data class ReceivedTextProperties(
    val text: String?,
    val tag: String? = null,
)

@OptIn(ExperimentalContracts::class)
fun ReceivedTextProperties?.hasValues(): Boolean {
    contract { returns(true) implies (this@hasValues != null) }
    return this != null && !text.isNullOrEmpty()
}
