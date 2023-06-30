package com.ramitsuri.notificationjournal.presentation

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.core.utils.Constants

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (applicationContext as MainApplication).getViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewState = viewModel.state.collectAsState().value
            WearApp(
                viewState = viewState,
                onAddRequested = viewModel::add,
                onSyncRequested = viewModel::sync,
            )
        }
        when (intent.extras?.getString(EXTRA_KEY)) {
            ADD -> {
                launchForInput { entry ->
                    viewModel.add(entry)
                    finish()
                }
            }

            else -> {
                // Do nothing
            }
        }
    }

    private fun launchForInput(onAddRequested: (String) -> Unit) {
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.processResult(onAddRequested)
        }
        launcher.launchInputActivity()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    companion object {
        const val EXTRA_KEY = "ADD_JOURNAL_ENTRY_KEY"
        const val ADD = "ADD_JOURNAL_ENTRY"
    }
}

fun ActivityResult?.processResult(onSuccess: (String) -> Unit) {
    this?.data?.let { data ->
        val results: Bundle = RemoteInput.getResultsFromIntent(data)
        val result = results.getCharSequence(Constants.REMOTE_INPUT_JOURNAL_KEY)?.toString()
        if (result != null) {
            onSuccess(result)
        }
    }
}

fun ActivityResultLauncher<Intent>.launchInputActivity() {
    val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent();
    val remoteInputs: List<RemoteInput> = listOf(
        RemoteInput.Builder(Constants.REMOTE_INPUT_JOURNAL_KEY)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }.build()
    )
    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
    launch(intent)
}