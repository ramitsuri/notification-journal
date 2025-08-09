package com.ramitsuri.notificationjournal

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ramitsuri.notificationjournal.core.di.DiFactory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.WindowSize
import com.ramitsuri.notificationjournal.core.ui.nav.NavGraph
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.awt.Desktop
import java.awt.desktop.AppForegroundEvent
import java.awt.desktop.AppForegroundListener
import com.ramitsuri.notificationjournal.core.model.WindowPosition as AppWindowPosition

fun main() =
    application {
        val factory = DiFactory()
        ServiceLocator.init(factory)
        // This is not called automatically when the app first starts
        ServiceLocator.onAppStart()
        var sizeIncreasedLastTime by remember { mutableStateOf(false) }

        val windowState =
            rememberWindowState(
                size = getWindowSize(),
                position = getWindowPosition(),
            )

        LaunchedEffect(Unit) {
            Desktop.getDesktop().addAppEventListener(
                object : AppForegroundListener {
                    override fun appRaisedToForeground(e: AppForegroundEvent?) {
                        ServiceLocator.onAppStart()
                    }

                    override fun appMovedToBackground(e: AppForegroundEvent?) {
                        ServiceLocator.onAppStop()
                    }
                },
            )
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Journal",
            state = windowState,
        ) {
            LaunchedEffect(window.rootPane) {
                with(window.rootPane) {
                    putClientProperty("apple.awt.transparentTitleBar", true)
                    putClientProperty("apple.awt.fullWindowContent", true)
                }
            }
            NotificationJournalTheme {
                NavGraph()
            }
            LaunchedEffect(windowState) {
                snapshotFlow { windowState.size }
                    .distinctUntilChanged()
                    .debounce(300L)
                    .onEach(::setWindowSize)
                    .launchIn(this)

                snapshotFlow { windowState.position }
                    .filter { it.isSpecified }
                    .distinctUntilChanged()
                    .debounce(300L)
                    .onEach(::setWindowPosition)
                    .launchIn(this)
            }
            LifecycleResumeEffect(Unit) {
                // Due to a bug where dialogs don't open in the center of the window but resizing the
                // window fixes it
                if (sizeIncreasedLastTime) {
                    windowState.size -= DpSize(1.dp, 1.dp)
                    sizeIncreasedLastTime = false
                } else {
                    windowState.size += DpSize(1.dp, 1.dp)
                    sizeIncreasedLastTime = true
                }
                onPauseOrDispose {}
            }
        }
    }

private fun getWindowPosition(): WindowPosition {
    return runBlocking {
        ServiceLocator
            .prefManager
            .getWindowPosition()
            ?.let {
                WindowPosition(x = it.x.dp, y = it.y.dp)
            }
            ?: defaultWindowPosition
    }
}

private suspend fun setWindowPosition(position: WindowPosition) {
    ServiceLocator.prefManager.setWindowPosition(
        AppWindowPosition(
            x = position.x.value,
            y = position.y.value,
        ),
    )
}

private fun getWindowSize(): DpSize {
    return runBlocking {
        ServiceLocator
            .prefManager
            .getWindowSize()
            ?.let {
                DpSize(
                    width = it.width.dp, height = it.height.dp,
                )
            }
            ?: defaultWindowSize
    }
}

private suspend fun setWindowSize(size: DpSize) {
    ServiceLocator.prefManager.setWindowSize(
        WindowSize(
            height = size.height.value,
            width = size.width.value,
        ),
    )
}

private val defaultWindowSize = DpSize(height = 800.dp, width = 600.dp)
private val defaultWindowPosition = WindowPosition.PlatformDefault
