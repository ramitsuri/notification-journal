package com.ramitsuri.notificationjournal

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.ramitsuri.notificationjournal.ui.journalentry.AppUi
import com.ramitsuri.notificationjournal.ui.journalentry.JournalEntryViewModel
import com.ramitsuri.notificationjournal.ui.theme.NotificationJournalTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JournalEntryViewModel by viewModels {
        (applicationContext as MainApplication).getViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val receivedText =
            if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            } else {
                null
            }
        viewModel.setReceivedText(receivedText)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        showNotification()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }

            NotificationJournalTheme {
                val viewState = viewModel.state.collectAsState().value
                AppUi(
                    state = viewState,
                    onAddRequested = viewModel::add,
                    onEditRequested = viewModel::edit,
                    onDeleteRequested = viewModel::delete,
                    onErrorAcknowledged = viewModel::onErrorAcknowledged,
                    setApiUrlRequested = viewModel::setApiUrl,
                    uploadRequested = viewModel::upload,
                    reverseSortOrderRequested = viewModel::reverseSortOrder,
                    resetReceivedText = viewModel::resetReceivedText
                )
            }
        }
    }

    private fun showNotification() {
        (application as MainApplication).showJournalNotification()
    }
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:
 * activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;
 * drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:
 * activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;
 * drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun Context.shutdown() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

enum class JournalMenuItem(@StringRes val textResId: Int) {
    UPLOAD(R.string.button_text_upload_all),
    SET_SERVER(R.string.button_text_set_server),
    SERVER_SET(R.string.button_text_server_set),
    RESTART(R.string.button_text_restart),
    REVERSE_SORT(R.string.button_text_sort_order)
}