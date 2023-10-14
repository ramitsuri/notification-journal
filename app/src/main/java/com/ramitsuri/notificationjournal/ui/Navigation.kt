package com.ramitsuri.notificationjournal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ramitsuri.notificationjournal.ui.journalentry.JournalEntryScreen
import com.ramitsuri.notificationjournal.ui.journalentry.JournalEntryViewModel

object Destinations {
    const val JOURNAL_ENTRY = "journal_entry"
    const val TAGS = "tags"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    receivedText: String?,
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.JOURNAL_ENTRY,
        modifier = modifier
    ) {
        composable(Destinations.JOURNAL_ENTRY) {
            val viewModel: JournalEntryViewModel =
                viewModel(factory = JournalEntryViewModel.factory(receivedText))
            val viewState = viewModel.state.collectAsStateWithLifecycle().value
            JournalEntryScreen(
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

        composable(Destinations.SETTINGS) {

        }

        composable(Destinations.TAGS) {

        }
    }
}