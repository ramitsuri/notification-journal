package com.ramitsuri.notificationjournal.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
            WearApp(
                viewState = viewState,
                onAddRequested = viewModel::add,
                onSyncRequested = viewModel::sync,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}