package com.ramitsuri.notificationjournal.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.ui.theme.NotificationJournalTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (applicationContext as MainApplication).getViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    getAllRequested = viewModel::getAll,
                    uploadRequested = viewModel::upload,
                    reverseSortOrderRequested = viewModel::reverseSortOrder,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAll()
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
    val activity = getActivity()
    val intent =
        activity?.packageManager?.getLaunchIntentForPackage(activity.packageName)
    activity?.finishAffinity()
    activity?.startActivity(intent)
    exitProcess(0)
}

enum class JournalMenuItem(@StringRes val textResId: Int) {
    UPLOAD(R.string.button_text_upload_all),
    REFRESH(R.string.button_text_refresh),
    SET_SERVER(R.string.button_text_set_server),
    SERVER_SET(R.string.button_text_server_set),
    RESTART(R.string.button_text_restart),
    REVERSE_SORT(R.string.button_text_sort_order)
}