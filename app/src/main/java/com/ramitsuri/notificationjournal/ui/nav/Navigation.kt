package com.ramitsuri.notificationjournal.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ramitsuri.notificationjournal.ui.addjournal.AddJournalEntryScreen
import com.ramitsuri.notificationjournal.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.ui.editjournal.EditJournalEntryScreen
import com.ramitsuri.notificationjournal.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.ui.journalentry.JournalEntryScreen
import com.ramitsuri.notificationjournal.ui.journalentry.JournalEntryViewModel
import com.ramitsuri.notificationjournal.ui.screens.TagsScreen
import com.ramitsuri.notificationjournal.ui.settings.SettingsScreen
import com.ramitsuri.notificationjournal.ui.settings.SettingsViewModel
import com.ramitsuri.notificationjournal.ui.tags.TagsViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    receivedText: String?,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.JOURNAL_ENTRY.route(),
        modifier = modifier
    ) {
        composable(Destination.JOURNAL_ENTRY.route()) {
            val viewModel: JournalEntryViewModel =
                viewModel(factory = JournalEntryViewModel.factory(receivedText))
            val viewState = viewModel.state.collectAsStateWithLifecycle().value
            JournalEntryScreen(
                state = viewState,
                onAddRequested = viewModel::add,
                onEditRequested = viewModel::edit,
                onDeleteRequested = viewModel::delete,
                resetReceivedText = viewModel::resetReceivedText,
                onSettingsClicked = {
                    navController.navigate(Destination.SETTINGS.routeWithArgValues())
                }
            )
        }

        composable(
            route = Destination.ADD_ENTRY.route(),
            arguments = Destination.ADD_ENTRY.navArgs()
        ) { backStackEntry ->
            val viewModel: AddJournalEntryViewModel =
                viewModel(factory = AddJournalEntryViewModel.factory(backStackEntry))
            val viewState = viewModel.state.collectAsStateWithLifecycle().value

            AddJournalEntryScreen(
                state = viewState,
                onTextUpdated = viewModel::textUpdated,
                onTagClicked = viewModel::tagClicked,
                onUseSuggestedText = viewModel::useSuggestedText,
                onSave = viewModel::save,
                onCancel = { navController.navigateUp() },
            )
        }

        composable(
            Destination.EDIT_ENTRY.route(),
            arguments = Destination.EDIT_ENTRY.navArgs()
        ) { backStackEntry ->
            val viewModel: EditJournalEntryViewModel =
                viewModel(factory = EditJournalEntryViewModel.factory(backStackEntry))
            val viewState = viewModel.state.collectAsStateWithLifecycle().value

            EditJournalEntryScreen(
                state = viewState,
                onTextUpdated = viewModel::textUpdated,
                onTagClicked = viewModel::tagClicked,
                onSave = viewModel::save,
                onCancel = { navController.navigateUp() },
            )
        }

        composable(Destination.SETTINGS.route()) {
            val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
            val viewState = viewModel.state.collectAsStateWithLifecycle().value
            SettingsScreen(
                state = viewState,
                onBack = { navController.navigateUp() },
                onUploadClicked = viewModel::upload,
                onApiUrlSet = viewModel::setApiUrl,
                onSortOrderClicked = viewModel::reverseSortOrder,
                onErrorAcknowledged = viewModel::onErrorAcknowledged,
                onTagsClicked = { navController.navigate(Destination.TAGS.routeWithArgValues()) },
            )
        }

        composable(Destination.TAGS.route()) {
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