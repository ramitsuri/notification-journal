package com.ramitsuri.notificationjournal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.ui.nav.NavGraph
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import com.jthemedetecor.OsThemeDetector

fun main() = application {
    val factory = Factory()
    ServiceLocator.init(factory)
    ServiceLocator.onAppStart()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Journal",
    ) {
        val darkTheme = rememberTheme()
        NotificationJournalTheme(darkTheme = darkTheme) {
            NavGraph()
        }
    }
}

@Composable
private fun rememberTheme(): Boolean {
    var darkTheme by remember {
        mutableStateOf(currentSystemTheme == SystemTheme.DARK)
    }

    DisposableEffect(Unit) {
        val darkThemeListener: (Boolean) -> Unit = {
            darkTheme = it
        }

        val detector = OsThemeDetector.getDetector().apply {
            registerListener(darkThemeListener)
        }

        onDispose {
            detector.removeListener(darkThemeListener)
        }
    }

    return darkTheme
}