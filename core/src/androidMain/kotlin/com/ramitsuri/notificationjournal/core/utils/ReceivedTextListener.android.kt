package com.ramitsuri.notificationjournal.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavController

@Composable
actual fun ReceivedTextListener(
    navController: NavController,
    onTextReceived: (ReceivedTextProperties?) -> Unit,
) {
    val context = LocalContext.current
    val activity = (context.getActivity() as ComponentActivity)
    DisposableEffect(navController) {
        val listener =
            Consumer<Intent> { intent ->
                onTextReceived(intent.receivedText())
            }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}

fun Context.getActivity(): Activity {
    if (this is Activity) return this
    return if (this is ContextWrapper) baseContext.getActivity() else getActivity()
}

fun Intent?.receivedText(): ReceivedTextProperties? {
    if (this == null) {
        return null
    }
    return if (action == Intent.ACTION_SEND && type == "text/plain") {
        ReceivedTextProperties(
            text = getStringExtra(Intent.EXTRA_TEXT),
        )
    } else if (action == "com.ramitsuri.notificationjournal.intent.SHARE" && type == "text/plain") {
        ReceivedTextProperties(
            text = getStringExtra("com.ramitsuri.notificationjournal.intent.TEXT"),
            tag = getStringExtra("com.ramitsuri.notificationjournal.intent.TAG"),
        )
    } else {
        null
    }
}
