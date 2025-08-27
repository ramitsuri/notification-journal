package com.ramitsuri.notificationjournal.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun NotificationJournalTheme(content: @Composable () -> Unit) {
    val dynamicColorScheme = dynamicColorScheme(LocalContext.current)
    MaterialTheme(
        colorScheme = dynamicColorScheme ?: colorScheme,
        typography = Typography,
        content = content,
    )
}
