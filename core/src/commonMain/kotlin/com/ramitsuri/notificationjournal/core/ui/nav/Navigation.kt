package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.settings.SettingsScreen
import com.ramitsuri.notificationjournal.core.ui.settings.SettingsViewModel
import com.ramitsuri.notificationjournal.core.ui.tags.TagsScreen
import com.ramitsuri.notificationjournal.core.ui.tags.TagsViewModel
import com.ramitsuri.notificationjournal.core.ui.templates.TemplatesScreen
import com.ramitsuri.notificationjournal.core.ui.templates.TemplatesViewModel
import com.ramitsuri.notificationjournal.core.utils.ReceivedTextListener
import java.net.URLEncoder

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    receivedText: String? = null,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.JOURNAL_ENTRY.route(),
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Destination.JOURNAL_ENTRY.route()) {
            val viewModel: JournalEntryViewModel =
                viewModel(factory = JournalEntryViewModel.factory(receivedText))

            ReceivedTextListener(navController = navController) {
                viewModel.setReceivedText(it)
            }

            val receivedTextState by viewModel.receivedText.collectAsStateWithLifecycle()
            LaunchedEffect(key1 = receivedTextState) {
                if (!receivedTextState.isNullOrEmpty()) {
                    navController.navigate(
                        Destination.ADD_ENTRY.routeWithArgValues(
                            mapOf(
                                AddJournalEntryViewModel.RECEIVED_TEXT_ARG to
                                        URLEncoder.encode(receivedTextState, "UTF-8")
                            )
                        )
                    )
                    viewModel.onReceivedTextConsumed()
                }
            }

            val viewState by viewModel.state.collectAsStateWithLifecycle()
            JournalEntryScreen(
                state = viewState,
                onAddRequested = {
                    navController.navigate(
                        Destination.ADD_ENTRY.routeWithArgValues()
                    )
                },
                onEditRequested = { entryId ->
                    navController.navigate(
                        Destination.EDIT_ENTRY.routeWithArgValues(
                            mapOf(
                                EditJournalEntryViewModel.ENTRY_ID_ARG to entryId
                            )
                        )
                    )
                },
                onDeleteRequested = viewModel::delete,
                onEditTagRequested = viewModel::editTag,
                onMoveToNextDayRequested = viewModel::moveToNextDay,
                onMoveToPreviousDayRequested = viewModel::moveToPreviousDay,
                onMoveUpRequested = viewModel::moveUp,
                onMoveDownRequested = viewModel::moveDown,
                onTagGroupMoveToNextDayRequested = viewModel::moveToNextDay,
                onTagGroupMoveToPreviousDayRequested = viewModel::moveToPreviousDay,
                onTagGroupDeleteRequested = viewModel::delete,
                onSettingsClicked = {
                    navController.navigate(Destination.SETTINGS.routeWithArgValues())
                },
            )
        }

        dialog(
            route = Destination.ADD_ENTRY.route(),
            arguments = Destination.ADD_ENTRY.navArgs()
        ) { backStackEntry ->
            val viewModel: AddJournalEntryViewModel =
                viewModel(factory = ServiceLocator.getAddJournalEntryVMFactory(backStackEntry))

            val saved by viewModel.saved.collectAsStateWithLifecycle()
            LaunchedEffect(saved) {
                if (saved) {
                    navController.navigateUp()
                }
            }

            val viewState by viewModel.state.collectAsStateWithLifecycle()

            AddJournalEntryScreen(
                state = viewState,
                onTextUpdated = viewModel::textUpdated,
                onTagClicked = viewModel::tagClicked,
                onUseSuggestedText = viewModel::useSuggestedText,
                onTemplateClicked = viewModel::templateClicked,
                onSave = viewModel::save,
                onAddAnother = viewModel::saveAndAddAnother,
                onCancel = { navController.navigateUp() },
            )
        }

        dialog(
            Destination.EDIT_ENTRY.route(),
            arguments = Destination.EDIT_ENTRY.navArgs()
        ) { backStackEntry ->
            val viewModel: EditJournalEntryViewModel =
                viewModel(factory = ServiceLocator.getEditJournalEntryVMFactory(backStackEntry))

            val saved by viewModel.saved.collectAsStateWithLifecycle()
            LaunchedEffect(saved) {
                if (saved) {
                    navController.navigateUp()
                }
            }

            val viewState by viewModel.state.collectAsStateWithLifecycle()

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
            val viewState by viewModel.state.collectAsStateWithLifecycle()
            SettingsScreen(
                state = viewState,
                onBack = { navController.navigateUp() },
                onUploadClicked = viewModel::upload,
                onDataSharingPropertiesSet = viewModel::setDataSharingProperties,
                onSortOrderClicked = viewModel::reverseSortOrder,
                onErrorAcknowledged = viewModel::onErrorAcknowledged,
                onTagsClicked = {
                    navController.navigate(Destination.TAGS.routeWithArgValues())
                },
                onTemplatesClicked = {
                    navController.navigate(Destination.TEMPLATES.routeWithArgValues())
                },
            )
        }

        composable(Destination.TAGS.route()) {
            val viewModel: TagsViewModel = viewModel(factory = TagsViewModel.factory())
            val viewState by viewModel.state.collectAsStateWithLifecycle()
            TagsScreen(
                state = viewState,
                onTextUpdated = viewModel::textUpdated,
                onEditOrder = viewModel::editOrder,
                onAddRequested = viewModel::addClicked,
                onEditRequested = viewModel::editClicked,
                onDeleteRequested = viewModel::delete,
                onErrorAcknowledged = viewModel::onErrorAcknowledged,
                onBack = { navController.navigateUp() },
                onAddOrEditApproved = viewModel::save,
                onAddOrEditCanceled = viewModel::onAddOrEditCanceled,
            )
        }

        composable(Destination.TEMPLATES.route()) {
            val viewModel: TemplatesViewModel = viewModel(factory = TemplatesViewModel.factory())
            val viewState by viewModel.state.collectAsStateWithLifecycle()
            TemplatesScreen(
                state = viewState,
                onTextUpdated = viewModel::textUpdated,
                onTagClicked = viewModel::tagClicked,
                onEditRequested = viewModel::editClicked,
                onDeleteRequested = viewModel::delete,
                onAddRequested = viewModel::addClicked,
                onSyncWithWearRequested = viewModel::syncWithWear,
                onAddOrEditApproved = viewModel::save,
                onAddOrEditCanceled = viewModel::onAddOrEditCanceled,
                onBack = { navController.navigateUp() },
            )
        }
    }
}