package com.ramitsuri.notificationjournal

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.ui.nav.NavGraph
import com.ramitsuri.notificationjournal.core.ui.theme.NotificationJournalTheme
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

fun main() =
    application {
        val factory = Factory()
        ServiceLocator.init(factory)
        ServiceLocator.onAppStart()
        var sizeIncreasedLastTime by remember { mutableStateOf(false) }

        val windowState =
            rememberWindowState(
                size = getWindowSize(),
                position = getWindowPosition(),
            )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Journal",
            state = windowState,
        ) {
            NotificationJournalTheme {
                NavGraph()
            }
            LaunchedEffect(windowState) {
                snapshotFlow { windowState.size }
                    .onEach(::setWindowSize)
                    .launchIn(this)

                snapshotFlow { windowState.position }
                    .filter { it.isSpecified }
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
    val positionX = getDpValue(Constants.PREF_WINDOW_POSITION_X)
    val positionY = getDpValue(Constants.PREF_WINDOW_POSITION_Y)
    return if (positionX != null && positionY != null) {
        WindowPosition(positionX, positionY)
    } else {
        WindowPosition.PlatformDefault
    }
}

private fun setWindowPosition(position: WindowPosition) {
    setDpValue(Constants.PREF_WINDOW_POSITION_X, position.x)
    setDpValue(Constants.PREF_WINDOW_POSITION_Y, position.y)
}

private fun getWindowSize(): DpSize {
    val height = getDpValue(Constants.PREF_WINDOW_SIZE_HEIGHT)
    val width = getDpValue(Constants.PREF_WINDOW_SIZE_WIDTH)
    return if (height != null && width != null) {
        DpSize(width = width, height = height)
    } else {
        DpSize(800.dp, 600.dp)
    }
}

private fun setWindowSize(size: DpSize) {
    setDpValue(Constants.PREF_WINDOW_SIZE_HEIGHT, size.height)
    setDpValue(Constants.PREF_WINDOW_SIZE_WIDTH, size.width)
}

private fun setDpValue(
    key: String,
    value: Dp,
) {
    ServiceLocator.keyValueStore.putInt(key, value.value.roundToInt())
}

private fun getDpValue(key: String): Dp? {
    val hasKey = ServiceLocator.keyValueStore.hasKey(key)
    return if (!hasKey) {
        null
    } else {
        ServiceLocator.keyValueStore.getInt(key, 0).dp
    }
}
