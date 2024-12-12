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
import androidx.navigation.compose.rememberNavController
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.log.LogScreen
import com.ramitsuri.notificationjournal.core.ui.log.LogScreenViewModel
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
                onAddRequested = { date ->
                    navController.navigate(
                        Destination.ADD_ENTRY.routeWithArgValues(
                            mapOf(AddJournalEntryViewModel.DATE_ARG to date?.toString())
                        )
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
                onMoveToTopRequested = viewModel::moveToTop,
                onMoveDownRequested = viewModel::moveDown,
                onMoveToBottomRequested = viewModel::moveToBottom,
                onTagGroupMoveToNextDayRequested = viewModel::moveToNextDay,
                onTagGroupMoveToPreviousDayRequested = viewModel::moveToPreviousDay,
                onTagGroupDeleteRequested = viewModel::delete,
                onSettingsClicked = {
                    navController.navigate(Destination.SETTINGS.routeWithArgValues())
                },
                onSyncClicked = viewModel::sync,
                onTagGroupForceUploadRequested = viewModel::forceUpload,
                onForceUploadRequested = viewModel::forceUpload,
                onConflictResolved = viewModel::resolveConflict,
                onDuplicateRequested = {
                    navController.navigate(
                        Destination.ADD_ENTRY.routeWithArgValues(
                            mapOf(AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG to it.id),
                        )
                    )
                },
                onShowPreviousDayClicked = viewModel::goToPreviousDay,
                onShowNextDayClicked = viewModel::goToNextDay,
                onShowDayGroupClicked = viewModel::showDayGroupClicked,
                onCopyEntryRequested = viewModel::onCopy,
                onCopyTagGroupRequested = viewModel::onCopy,
                onCopyDayGroupRequested = viewModel::onCopy,
                onCopied = viewModel::onContentCopied,
                onResetReceiveHelper = viewModel::resetReceiveHelper,
                onAddFromTagRequested = { date, time, tag ->
                    navController.navigate(
                        Destination.ADD_ENTRY.routeWithArgValues(
                            mapOf(
                                AddJournalEntryViewModel.DATE_ARG to date.toString(),
                                AddJournalEntryViewModel.TIME_ARG to time?.toString(),
                                AddJournalEntryViewModel.TAG_ARG to tag,
                            )
                        )
                    )
                },
                onCancelReconcile = viewModel::cancelReconcile,
                onLogsClicked = {
                    navController.navigate(
                        Destination.LOGS.routeWithArgValues()
                    )
                },
            )
        }

        composable(
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
                onTagClicked = viewModel::tagClicked,
                onTemplateClicked = viewModel::templateClicked,
                onUseSuggestedText = viewModel::useSuggestedText,
                onSave = viewModel::save,
                onAddAnother = viewModel::saveAndAddAnother,
                onCancel = { navController.navigateUp() },
                onDateSelected = viewModel::dateSelected,
                onPreviousDateRequested = viewModel::previousDay,
                onNextDateRequested = viewModel::nextDay,
                onTimeSelected = viewModel::timeSelected,
                onResetDate = viewModel::resetDate,
                onResetDateToToday = viewModel::resetDateToToday,
                onResetTime = viewModel::resetTime,
                onResetTimeToNow = viewModel::resetTimeToNow,
            )
        }

        composable(
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
                onTagClicked = viewModel::tagClicked,
                onTemplateClicked = viewModel::templateClicked,
                onSave = viewModel::save,
                onCancel = { navController.navigateUp() },
                onDateSelected = viewModel::dateSelected,
                onPreviousDateRequested = viewModel::previousDay,
                onNextDateRequested = viewModel::nextDay,
                onTimeSelected = viewModel::timeSelected,
                onResetDate = viewModel::resetDate,
                onResetTime = viewModel::resetTime,
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
                onTagsClicked = {
                    navController.navigate(Destination.TAGS.routeWithArgValues())
                },
                onTemplatesClicked = {
                    navController.navigate(Destination.TEMPLATES.routeWithArgValues())
                },
                onToggleShowReconciled = viewModel::toggleShowReconciled,
                onToggleShowConflictDiffInline = viewModel::toggleShowConflictDiffInline,
                onToggleCopyWithEmptyTags = viewModel::toggleCopyWithEmptyTags,
                onToggleShowEmptyTags = viewModel::toggleShowEmptyTags,
                onToggleShowLogsButton = viewModel::toggleShowLogsButton,
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
                onSyncRequested = viewModel::sync,
            )
        }

        composable(Destination.TEMPLATES.route()) {
            val viewModel: TemplatesViewModel = viewModel(factory = TemplatesViewModel.factory())
            val viewState by viewModel.state.collectAsStateWithLifecycle()
            TemplatesScreen(
                state = viewState,
                onTextUpdated = viewModel::textUpdated,
                onDisplayTextUpdated = viewModel::displayTextUpdated,
                onShortDisplayTextUpdated = viewModel::shortDisplayTextUpdated,
                onTagClicked = viewModel::tagClicked,
                onEditRequested = viewModel::editClicked,
                onDeleteRequested = viewModel::delete,
                onAddRequested = viewModel::addClicked,
                onSyncRequested = viewModel::sync,
                onAddOrEditApproved = viewModel::save,
                onAddOrEditCanceled = viewModel::onAddOrEditCanceled,
                onBack = { navController.navigateUp() },
            )
        }

        composable(Destination.LOGS.route()) {
            val viewModel: LogScreenViewModel =
                viewModel(factory = LogScreenViewModel.factory())

            val logs by viewModel.logs.collectAsStateWithLifecycle()

            LogScreen(
                logs = logs,
                onBackClick = { navController.navigateUp() },
            )
        }
    }
}