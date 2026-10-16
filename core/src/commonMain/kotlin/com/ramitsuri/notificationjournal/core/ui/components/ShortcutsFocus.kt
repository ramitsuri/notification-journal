package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Returns a [FocusRequester] for the node that handles a screen's keyboard shortcuts and keeps
 * requesting focus for it, because key events are only delivered to the focused node and its
 * ancestors.
 *
 * Requesting focus only once when the screen is first shown isn't enough. Focus is lost when:
 * - The focused node is removed (for example a focused entry is deleted or moved, or the day pager
 *   swaps the page content). Compose then clears focus entirely, so nothing receives key events.
 * - Something else in the window takes focus (for example a date in the list pane is clicked).
 * - A dialog or another window takes focus.
 *
 * So focus is requested whenever the screen is resumed, and whenever any of [keys] change.
 *
 * @param enabled Pass false when another screen on screen at the same time should own the shortcuts
 *   (for example the list pane in a list-detail layout).
 */
@Composable
fun rememberShortcutsFocusRequester(
    vararg keys: Any?,
    enabled: Boolean = true,
): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(focusRequester, lifecycleOwner, enabled, *keys) {
        if (!enabled) {
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
        }
    }
    return focusRequester
}
