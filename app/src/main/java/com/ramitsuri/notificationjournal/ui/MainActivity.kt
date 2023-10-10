package com.ramitsuri.notificationjournal.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.ui.theme.NotificationJournalTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
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
        showNotification()
        setContent {
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