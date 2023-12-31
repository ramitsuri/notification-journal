package com.ramitsuri.notificationjournal.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.ramitsuri.notificationjournal.MainApplication

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (applicationContext as MainApplication).getViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewState = viewModel.state.collectAsState().value
            LaunchedEffect(key1 = viewState) {
                if (viewState.shouldExit) {
                    finish()
                }
            }
            WearApp(
                viewState = viewState,
                onAddRequested = viewModel::add,
                onTemplateAddRequested = viewModel::addFromTemplate,
                onTransferRequested = viewModel::transferLocallySaved,
                onUploadRequested = viewModel::triggerUpload
            )
        }
        // From tile
        when (intent.extras?.getString(EXTRA_KEY)) {
            ADD -> {
                launchForInput { entry ->
                    viewModel.add(entry)
                    finish()
                }
            }

            UPLOAD -> {
                viewModel.triggerUpload()
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

    companion object {
        const val EXTRA_KEY = "EXTRA_KEY"
        const val ADD = "ADD_JOURNAL_ENTRY"
        const val UPLOAD = "UPLOAD"
        const val OPEN_APP = "OPEN_APP"
    }
}

fun ActivityResult?.processResult(onSuccess: (String) -> Unit) {
    this?.data?.let { data ->
        val result = data.extras?.getString("result_text")
        if (result != null) {
            onSuccess(result)
        }
    }
}

fun ActivityResultLauncher<Intent>.launchInputActivity() {
    // RemoteInputHelper stopped working after One UI 5 update
    val intent = Intent("com.google.android.wearable.action.LAUNCH_KEYBOARD")
    launch(intent)
}