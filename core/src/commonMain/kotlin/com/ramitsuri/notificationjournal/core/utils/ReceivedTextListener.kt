package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
expect fun ReceivedTextListener(
    navController: NavController,
    onTextReceived: (String?) -> Unit,
)
