package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrCodeScanner(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
)

@Composable
expect fun QrCodeImage(
    modifier: Modifier = Modifier,
    content: String,
)
