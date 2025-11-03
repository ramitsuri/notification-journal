package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
actual fun ReceivedTextListener(
    navController: NavController,
    onTextReceived: (ReceivedTextProperties?) -> Unit,
) {
    // Not supported
}
