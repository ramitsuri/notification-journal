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
import com.ramitsuri.notificationjournal.core.ui.components.DayGroupAction
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.import.ImportScreen
import com.ramitsuri.notificationjournal.core.ui.import.ImportViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentry.EntryScreenAction
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryScreen
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentryday.ViewJournalEntryDayScreen
import com.ramitsuri.notificationjournal.core.ui.journalentryday.ViewJournalEntryDayViewModel
import com.ramitsuri.notificationjournal.core.ui.log.LogScreen
import com.ramitsuri.notificationjournal.core.ui.log.LogScreenViewModel
import com.ramitsuri.notificationjournal.core.ui.search.SearchScreen
import com.ramitsuri.notificationjournal.core.ui.search.SearchViewModel
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
                animationSpec = tween(300),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                animationSpec = tween(300),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                animationSpec = tween(300),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                animationSpec = tween(300),
            )
        },
    ) {
        composable(
            route = Destination.JOURNAL_ENTRY.route(),
            // Getting from ServiceLocator because deep links not supported on multiplatform
            deepLinks = ServiceLocator.getJournalEntryScreenDeepLinks(),
        ) {
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
                                    URLEncoder.encode(receivedTextState, "UTF-8"),
                            ),
                        ),
                    )
                    viewModel.onReceivedTextConsumed()
                }
            }

            val viewState by viewModel.state.collectAsStateWithLifecycle()
            JournalEntryScreen(
                state = viewState,
                onEntryScreenAction = { action ->
                    when (action) {
                        is EntryScreenAction.AddWithDate -> {
                            navController.navigate(
                                Destination.ADD_ENTRY.routeWithArgValues(
                                    mapOf(AddJournalEntryViewModel.DATE_ARG to action.date?.toString()),
                                ),
                            )
                        }

                        is EntryScreenAction.CancelReconcile -> {
                            viewModel.cancelReconcile()
                        }

                        is EntryScreenAction.Copy -> {
                            viewModel.onContentCopied()
                        }

                        is EntryScreenAction.NavToSearch -> {
                            navController.navigate(Destination.SEARCH.route())
                        }

                        is EntryScreenAction.NavToSettings -> {
                            navController.navigate(Destination.SETTINGS.routeWithArgValues())
                        }

                        is EntryScreenAction.ResetReceiveHelper -> {
                            viewModel.resetReceiveHelper()
                        }

                        is EntryScreenAction.ShowDayGroup -> {
                            viewModel.showDayGroupClicked(action.date)
                        }

                        is EntryScreenAction.Sync -> {
                            viewModel.sync()
                        }

                        is EntryScreenAction.NavToViewJournalEntryDay -> {
                            navController.navigate(Destination.VIEW_JOURNAL_ENTRY_DAY.routeWithArgValues())
                        }

                        is EntryScreenAction.ReconcileAll -> {
                            viewModel.onReconcileAll(action.uploadOnSuccess)
                        }

                        is EntryScreenAction.ExportDirectorySet -> {
                            viewModel.onExportDirectorySet(action.directory)
                        }
                    }
                },
                onDayGroupAction = { action ->
                    when (action) {
                        is DayGroupAction.AddEntry -> {
                            navController.navigate(
                                Destination.ADD_ENTRY.routeWithArgValues(
                                    mapOf(
                                        AddJournalEntryViewModel.DATE_ARG to action.date.toString(),
                                        AddJournalEntryViewModel.TIME_ARG to action.time?.toString(),
                                        AddJournalEntryViewModel.TAG_ARG to action.tag,
                                    ),
                                ),
                            )
                        }

                        is DayGroupAction.CopyDayGroup -> {
                            viewModel.onCopy()
                        }

                        is DayGroupAction.CopyEntry -> {
                            viewModel.onCopy(action.entry)
                        }

                        is DayGroupAction.CopyTagGroup -> {
                            viewModel.onCopy(action.tagGroup)
                        }

                        is DayGroupAction.DeleteEntry -> {
                            viewModel.delete(action.entry)
                        }

                        is DayGroupAction.DeleteTagGroup -> {
                            viewModel.delete(action.tagGroup)
                        }

                        is DayGroupAction.DuplicateEntry -> {
                            navController.navigate(
                                Destination.ADD_ENTRY.routeWithArgValues(
                                    mapOf(AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG to action.entry.id),
                                ),
                            )
                        }

                        is DayGroupAction.EditEntry -> {
                            navController.navigate(
                                Destination.EDIT_ENTRY.routeWithArgValues(
                                    mapOf(
                                        EditJournalEntryViewModel.ENTRY_ID_ARG to action.entry.id,
                                    ),
                                ),
                            )
                        }

                        is DayGroupAction.EditTag -> {
                            viewModel.editTag(action.entry, action.tag)
                        }

                        is DayGroupAction.ForceUploadEntry -> {
                            viewModel.forceUpload(action.entry)
                        }

                        is DayGroupAction.ForceUploadTagGroup -> {
                            viewModel.forceUpload(action.tagGroup)
                        }

                        is DayGroupAction.MoveEntryDown -> {
                            viewModel.moveDown(action.entry, action.tagGroup)
                        }

                        is DayGroupAction.MoveEntryToBottom -> {
                            viewModel.moveToBottom(action.entry, action.tagGroup)
                        }

                        is DayGroupAction.MoveEntryToNextDay -> {
                            viewModel.moveToNextDay(action.entry)
                        }

                        is DayGroupAction.MoveEntryToPreviousDay -> {
                            viewModel.moveToPreviousDay(action.entry)
                        }

                        is DayGroupAction.MoveEntryToTop -> {
                            viewModel.moveToTop(action.entry, action.tagGroup)
                        }

                        is DayGroupAction.MoveEntryUp -> {
                            viewModel.moveUp(action.entry, action.tagGroup)
                        }

                        is DayGroupAction.MoveTagGroupToNextDay -> {
                            viewModel.moveToNextDay(action.tagGroup)
                        }

                        is DayGroupAction.MoveTagGroupToPreviousDay -> {
                            viewModel.moveToPreviousDay(action.tagGroup)
                        }

                        is DayGroupAction.ResolveConflict -> {
                            viewModel.resolveConflict(action.entry, action.conflict)
                        }

                        is DayGroupAction.ShowPreviousDay -> {
                            viewModel.goToPreviousDay()
                        }

                        is DayGroupAction.ShowNextDay -> {
                            viewModel.goToNextDay()
                        }

                        is DayGroupAction.Notify -> {
                            viewModel.notify(action.entry, action.inTime)
                        }

                        is DayGroupAction.ToggleVerifyEntries -> {
                            viewModel.onVerifyEntriesRequested(action.verify)
                        }

                        is DayGroupAction.ShowAllDays -> {
                            error("Should already be managed in the screen and not reach here")
                        }
                    }
                },
            )
        }

        composable(
            route = Destination.ADD_ENTRY.route(),
            arguments = Destination.ADD_ENTRY.navArgs(),
            deepLinks = ServiceLocator.getAddEntryScreenDeepLinks(),
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
                onCorrectionAccepted = viewModel::correctionAccepted,
                onAddDictionaryWord = viewModel::addDictionaryWord,
                onSuggestionClicked = viewModel::onSuggestionClicked,
                onSuggestionsEnabledChanged = viewModel::onSuggestionEnabledChanged,
            )
        }

        composable(
            Destination.EDIT_ENTRY.route(),
            arguments = Destination.EDIT_ENTRY.navArgs(),
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
                onCorrectionAccepted = viewModel::correctionAccepted,
                onAddDictionaryWord = viewModel::addDictionaryWord,
                onSuggestionClicked = viewModel::onSuggestionClicked,
                onSuggestionsEnabledChanged = viewModel::onSuggestionEnabledChanged,
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
                onToggleShowConflictDiffInline = viewModel::toggleShowConflictDiffInline,
                onToggleCopyWithEmptyTags = viewModel::toggleCopyWithEmptyTags,
                onToggleShowEmptyTags = viewModel::toggleShowEmptyTags,
                onLogsClicked = {
                    navController.navigate(
                        Destination.LOGS.routeWithArgValues(),
                    )
                },
                onJournalImportClicked = {
                    navController.navigate(Destination.IMPORT.routeWithArgValues())
                },
                onDeleteAll = viewModel::deleteAll,
                onShowStatsToggled = viewModel::onStatsRequestToggled,
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
                onSetAsDefaultRequested = viewModel::setDefaultTag,
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
                onClearLogsClick = viewModel::clearLogsClicked,
            )
        }

        composable(Destination.SEARCH.route()) {
            val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory())

            val state by viewModel.state.collectAsStateWithLifecycle()
            SearchScreen(
                state = state,
                onBackClick = {
                    navController.navigateUp()
                },
                onClearClick = viewModel::clearSearchTerm,
                onTagClicked = viewModel::tagClicked,
                onSelectAllTagsClicked = viewModel::selectAllTagsClicked,
                onUnselectAllTagsClicked = viewModel::unselectAllTagsClicked,
                onNavToViewJournalEntryDay = { entry ->
                    navController.navigate(
                        Destination.VIEW_JOURNAL_ENTRY_DAY.routeWithArgValues(
                            mapOf(
                                ViewJournalEntryDayViewModel.DATE_ARG to entry.entryTime.date.toString(),
                                ViewJournalEntryDayViewModel.ENTRY_ID_ARG to entry.id,
                            ),
                        ),
                    )
                },
            )
        }

        composable(Destination.IMPORT.route()) {
            val viewModel: ImportViewModel = viewModel(factory = ImportViewModel.factory())

            val state by viewModel.state.collectAsStateWithLifecycle()
            ImportScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onImportClick = viewModel::onImportClicked,
                onFromDirChanged = viewModel::onFromDirChanged,
                onStartDateChanged = viewModel::onStartDateChanged,
                onResetStartDate = viewModel::onResetStartDate,
                onEndDateChanged = viewModel::onEndDateChanged,
                onResetEndDate = viewModel::onResetEndDate,
                onLastImportDateChanged = viewModel::onLastImportDateChanged,
                onResetLastImportTime = viewModel::resetLastImportDate,
                onToggleUseLastImportTime = viewModel::toggleUseLastImportTime,
            )
        }

        composable(
            route = Destination.VIEW_JOURNAL_ENTRY_DAY.route(),
            arguments = Destination.VIEW_JOURNAL_ENTRY_DAY.navArgs(),
        ) { backStackEntry ->
            val viewModel: ViewJournalEntryDayViewModel =
                viewModel(factory = ServiceLocator.getViewJournalEntryDayVMFactory(backStackEntry))

            val viewState by viewModel.state.collectAsStateWithLifecycle()
            ViewJournalEntryDayScreen(
                state = viewState,
                onBackClick = { navController.navigateUp() },
                onDateSelected = viewModel::onDateSelected,
                onAction = { action ->
                    when (action) {
                        is DayGroupAction.CopyEntry -> {
                            viewModel.onCopy(action.entry)
                        }

                        else -> {}
                    }
                },
                onContentCopied = viewModel::onContentCopied,
            )
        }
    }
}
