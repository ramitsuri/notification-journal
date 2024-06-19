package com.ramitsuri.notificationjournal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.ui.nav.NavGraph
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() = application {
    val factory = Factory()
    ServiceLocator.init(factory)
    CoroutineScope(Dispatchers.IO).launch {
        DataReceiveHelper.getDefault().startListening {
            println("Received payload: $it")
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Journal",
    ) {
        NotificationJournalTheme {
            NavGraph(
                shutdown = { }
            )
        }
    }
}