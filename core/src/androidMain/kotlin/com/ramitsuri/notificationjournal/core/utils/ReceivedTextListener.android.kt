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
import com.ramitsuri.notificationjournal.core.ui.nav.Navigator
import com.ramitsuri.notificationjournal.core.ui.nav.Route
import java.net.URLEncoder

@Composable
actual fun ReceivedTextListener(navigator: Navigator) {
    val context = LocalContext.current
    val activity = (context.getActivity() as ComponentActivity)
    DisposableEffect(navigator) {
        val listener =
            Consumer<Intent> { intent ->
                val receivedTextProperties = intent.receivedText()
                if (receivedTextProperties.hasValues()) {
                    navigator.navigate(
                        Route.AddEntry.fromReceivedText(
                            text = URLEncoder.encode(receivedTextProperties.text, "UTF-8"),
                            tag = receivedTextProperties.tag,
                        ),
                    )
                }
                activity.intent = null
            }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}

private fun Context.getActivity(): Activity {
    if (this is Activity) return this
    return if (this is ContextWrapper) baseContext.getActivity() else getActivity()
}

private fun Intent?.receivedText(): ReceivedTextProperties? {
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
