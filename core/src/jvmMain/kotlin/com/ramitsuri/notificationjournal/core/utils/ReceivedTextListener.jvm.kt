package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.ui.nav.Navigator

@Composable
actual fun ReceivedTextListener(
    navigator: Navigator,
    onTextReceived: (ReceivedTextProperties?) -> Unit,
) {
    // Not supported
}
