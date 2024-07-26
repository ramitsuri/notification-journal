package com.ramitsuri.notificationjournal.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        (applicationContext as MainApplication).getViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewState = viewModel.state.collectAsState().value
            LaunchedEffect(key1 = viewState) {
                val showToast = viewState.addStatus == AddStatus.SUCCESS_EXIT ||
                        viewState.addStatus == AddStatus.SUCCESS
                val finish = viewState.addStatus == AddStatus.SUCCESS_EXIT
                if (showToast) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.add_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.addStatusAcknowledged()
                }
                if (finish) {
                    finish()
                }
            }
            WearApp(
                viewState = viewState,
                onAddRequested = viewModel::add,
                onTemplateAddRequested = viewModel::addFromTemplate,
                onTransferRequested = viewModel::transferLocallySaved,
                onUploadRequested = viewModel::triggerUpload,
                onLoadThingsRequested = viewModel::loadTemplatesAndEntries,
            )
        }
        // From tile
        when (intent.extras?.getString(EXTRA_KEY)) {
            ADD -> {
                launchForInput { entry ->
                    viewModel.add(entry, exitOnDone = true)
                }
            }

            UPLOAD -> {
                viewModel.triggerUpload()
            }

            TEMPLATE -> {
                intent.extras?.getString(TEMPLATE_ID)?.let { templateId ->
                    viewModel.addFromTemplate(templateId)
                } ?: run {
                    Toast.makeText(
                        this,
                        getString(R.string.template_id_not_found),
                        Toast.LENGTH_LONG
                    ).show()
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

    companion object {
        const val EXTRA_KEY = "EXTRA_KEY"
        const val ADD = "ADD_JOURNAL_ENTRY"
        const val UPLOAD = "UPLOAD"
        const val TEMPLATE = "TEMPLATE"
        const val TEMPLATE_ID = "TEMPLATE_ID"
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