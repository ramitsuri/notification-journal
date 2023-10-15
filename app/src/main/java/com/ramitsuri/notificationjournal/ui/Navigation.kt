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
import com.ramitsuri.notificationjournal.ui.screens.TagsScreen
import com.ramitsuri.notificationjournal.ui.settings.SettingsScreen
import com.ramitsuri.notificationjournal.ui.settings.SettingsViewModel
import com.ramitsuri.notificationjournal.ui.tags.TagsViewModel

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
                resetReceivedText = viewModel::resetReceivedText,
                onSettingsClicked = { navController.navigate(Destinations.SETTINGS) }
            )
        }

        composable(Destinations.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
            val viewState = viewModel.state.collectAsStateWithLifecycle().value
            SettingsScreen(
                state = viewState,
                onBack = { navController.navigateUp() },
                onUploadClicked = viewModel::upload,
                onApiUrlSet = viewModel::setApiUrl,
                onSortOrderClicked = viewModel::reverseSortOrder,
                onErrorAcknowledged = viewModel::onErrorAcknowledged,
                onTagsClicked = { navController.navigate(Destinations.TAGS) },
            )
        }

        composable(Destinations.TAGS) {
            val viewModel: TagsViewModel = viewModel(factory = TagsViewModel.factory())
            val viewState = viewModel.state.collectAsStateWithLifecycle().value
            TagsScreen(
                state = viewState,
                onEditOrder = viewModel::editOrder,
                onAddRequested = viewModel::add,
                onEditRequested = viewModel::editValue,
                onDeleteRequested = viewModel::delete,
                onErrorAcknowledged = viewModel::onErrorAcknowledged,
                onBack = { navController.navigateUp() },
            )
        }
    }
}