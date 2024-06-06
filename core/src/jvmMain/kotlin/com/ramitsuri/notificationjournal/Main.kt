package com.ramitsuri.notificationjournal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.ui.nav.NavGraph
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme

fun main() = application {
    val factory = Factory()
    ServiceLocator.init(factory)

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