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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
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
            val view = LocalView.current
            LaunchedEffect(key1 = viewState) {
                val showToast =
                    viewState.status is Status.SuccessExit ||
                        viewState.status is Status.Success
                val finish = viewState.status is Status.SuccessExit
                if (showToast) {
                    Toast.makeText(
                        this@MainActivity,
                        viewState.status.exitText ?: getString(R.string.add_success),
                        Toast.LENGTH_SHORT,
                    ).show()
                    viewModel.addStatusAcknowledged()
                    view.performHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
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
            )
        }
        // From tile
        when (intent.extras?.getString(EXTRA_KEY)) {
            ADD -> {
                launchForInput { entry ->
                    viewModel.add(entry, exitOnDone = true)
                }
            }

            TEMPLATE -> {
                intent.extras?.getString(TEMPLATE_ID)?.let { templateId ->
                    viewModel.addFromTemplate(templateId)
                } ?: run {
                    Toast.makeText(
                        this,
                        getString(R.string.template_id_not_found),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }

            SHOW_ADDITIONAL_TEMPLATES -> {
                viewModel.loadTemplatesAndEntries()
            }

            else -> {
                // Do nothing
            }
        }
    }

    private fun launchForInput(onAddRequested: (String) -> Unit) {
        val launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.processResult(onAddRequested)
            }
        launcher.launchInputActivity()
    }

    companion object {
        const val EXTRA_KEY = "EXTRA_KEY"
        const val ADD = "ADD_JOURNAL_ENTRY"
        const val TEMPLATE = "TEMPLATE"
        const val TEMPLATE_ID = "TEMPLATE_ID"
        const val SHOW_ADDITIONAL_TEMPLATES = "SHOW_ADDITIONAL_TEMPLATES"
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
