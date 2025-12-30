package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.ui.DateSelector
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
import com.ramitsuri.notificationjournal.core.ui.journalentrylist.JournalEntryDaysScreen
import com.ramitsuri.notificationjournal.core.ui.journalentrylist.JournalEntryDaysViewModel
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

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navigator: Navigator = remember { Navigator() },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val dateSelector = remember { DateSelector(lifecycleOwner, ServiceLocator.repository) }
    val selectedDate by dateSelector.selectedDate.collectAsStateWithLifecycle()
    LaunchedEffect(selectedDate) {
        val date = selectedDate
        if (date == null) {
            navigator.goBack()
        } else {
            navigator.navigate(Route.JournalEntry(date))
        }
    }
    val entryProvider: (Route) -> NavEntry<Route> =
        entryProvider {
            entry<Route.JournalEntryDays> {
                val viewModel: JournalEntryDaysViewModel =
                    viewModel(factory = JournalEntryDaysViewModel.factory())
                val viewState by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(selectedDate) {
                    viewModel.onDateSelected(selectedDate)
                }
                LaunchedEffect(Unit) {
                    viewModel.onVerifyEntriesRequested()
                }
                JournalEntryDaysScreen(
                    state = viewState,
                    onDateSelected = {
                        dateSelector.selectDate(it)
                    },
                    onResetReceiveHelper = viewModel::resetReceiveHelper,
                    onViewByDate = {
                        navigator.navigate(Route.ViewJournalEntryDay())
                    },
                    onReconcileAll = viewModel::onReconcileAll,
                    onSettingsClicked = {
                        navigator.navigate(Route.Settings)
                    },
                    onSearchClicked = {
                        navigator.navigate(Route.Search)
                    },
                    onSyncClicked = viewModel::sync,
                    onAddClicked = {
                        navigator.navigate(Route.AddEntry.fromDate(null))
                    },
                )
            }

            entry<Route.JournalEntry> { arg ->
                LaunchedEffect(arg) {
                    // If coming from a deep link, this screen will be opened automatically, so let date selector know
                    // what date is selected
                    dateSelector.selectDate(arg.selectedDate)
                }
                val viewModel: JournalEntryViewModel =
                    viewModel(factory = JournalEntryViewModel.factory(selectedDate = arg.selectedDate))
                val state =
                    rememberNavigationEventState(
                        currentInfo = NavigationEventInfo.None,
                    )
                NavigationBackHandler(
                    state = state,
                    onBackCompleted = {
                        navigator.goBack()
                        dateSelector.selectDate(null)
                    },
                )

                val viewState by viewModel.state.collectAsStateWithLifecycle()
                JournalEntryScreen(
                    state = viewState,
                    onEntryScreenAction = { action ->
                        when (action) {
                            is EntryScreenAction.AddWithDate -> {
                                navigator.navigate(
                                    Route.AddEntry.fromDate(
                                        action.date,
                                    ),
                                )
                            }

                            is EntryScreenAction.Copy -> {
                                viewModel.onContentCopied()
                            }

                            is EntryScreenAction.NavToSearch -> {
                                navigator.navigate(Route.Search)
                            }

                            is EntryScreenAction.NavToSettings -> {
                                navigator.navigate(Route.Settings)
                            }

                            is EntryScreenAction.ResetReceiveHelper -> {
                                viewModel.resetReceiveHelper()
                            }

                            is EntryScreenAction.Sync -> {
                                viewModel.sync()
                            }

                            is EntryScreenAction.NavToViewJournalEntryDay -> {
                                navigator.navigate(Route.ViewJournalEntryDay())
                            }

                            is EntryScreenAction.NavBack -> {
                                dateSelector.selectDate(null)
                                navigator.goBack()
                            }
                        }
                    },
                    onDayGroupAction = { action ->
                        when (action) {
                            is DayGroupAction.AddEntry -> {
                                navigator.navigate(
                                    Route.AddEntry.fromDateTimeTag(
                                        date = action.date,
                                        time = action.time,
                                        tag = action.tag,
                                    ),
                                )
                            }

                            is DayGroupAction.ReconcileDayGroup -> {
                                viewModel.onReconcile()
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
                                navigator.navigate(
                                    Route.AddEntry.fromDuplicateEntryId(
                                        entryId = action.entry.id,
                                    ),
                                )
                            }

                            is DayGroupAction.EditEntry -> {
                                navigator.navigate(
                                    Route.EditEntry(
                                        entryId = action.entry.id,
                                    ),
                                )
                            }

                            is DayGroupAction.EditTag -> {
                                viewModel.editTag(action.entry, action.tag)
                            }

                            is DayGroupAction.UploadEntry -> {
                                viewModel.upload(action.entry)
                            }

                            is DayGroupAction.UploadTagGroup -> {
                                viewModel.upload(action.tagGroup)
                            }

                            is DayGroupAction.UploadDayGroup -> {
                                viewModel.upload()
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
                                dateSelector.selectPreviousDate()
                            }

                            is DayGroupAction.ShowNextDay -> {
                                dateSelector.selectNextDate()
                            }

                            is DayGroupAction.Notify -> {
                                viewModel.notify(action.entry, action.inTime)
                            }
                        }
                    },
                )
            }

            entry<Route.AddEntry> { arg ->
                val viewModel: AddJournalEntryViewModel =
                    viewModel(factory = ServiceLocator.getAddJournalEntryVMFactory(arg))

                val saved by viewModel.saved.collectAsStateWithLifecycle()
                LaunchedEffect(saved) {
                    if (saved) {
                        navigator.goBack()
                    }
                }

                val viewState by viewModel.state.collectAsStateWithLifecycle()

                AddJournalEntryScreen(
                    state = viewState,
                    onTagClicked = viewModel::tagClicked,
                    onTemplateClicked = viewModel::templateClicked,
                    onSave = viewModel::save,
                    onAddAnother = viewModel::saveAndAddAnother,
                    onCancel = { navigator.goBack() },
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

            entry<Route.EditEntry> { arg ->
                val viewModel: EditJournalEntryViewModel =
                    viewModel(factory = ServiceLocator.getEditJournalEntryVMFactory(arg))

                val saved by viewModel.saved.collectAsStateWithLifecycle()
                LaunchedEffect(saved) {
                    if (saved) {
                        navigator.goBack()
                    }
                }

                val viewState by viewModel.state.collectAsStateWithLifecycle()

                EditJournalEntryScreen(
                    state = viewState,
                    onTagClicked = viewModel::tagClicked,
                    onTemplateClicked = viewModel::templateClicked,
                    onSave = viewModel::save,
                    onCancel = { navigator.goBack() },
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

            entry<Route.Settings> {
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
                val viewState by viewModel.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = viewState,
                    onBack = { navigator.goBack() },
                    onForceUploadAllClicked = viewModel::forceUploadAll,
                    onDataSharingPropertiesSet = viewModel::setDataSharingProperties,
                    onTagsClicked = {
                        navigator.navigate(Route.Tags)
                    },
                    onTemplatesClicked = {
                        navigator.navigate(Route.Templates)
                    },
                    onToggleShowConflictDiffInline = viewModel::toggleShowConflictDiffInline,
                    onToggleCopyWithEmptyTags = viewModel::toggleCopyWithEmptyTags,
                    onToggleShowEmptyTags = viewModel::toggleShowEmptyTags,
                    onLogsClicked = {
                        navigator.navigate(
                            Route.Logs,
                        )
                    },
                    onJournalImportClicked = {
                        navigator.navigate(Route.Import)
                    },
                    onDeleteAll = viewModel::deleteAll,
                    onShowStatsToggled = viewModel::onStatsRequestToggled,
                    onExportDirectorySet = viewModel::setExportDirectory,
                )
            }

            entry<Route.Tags> {
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
                    onBack = { navigator.goBack() },
                    onAddOrEditApproved = viewModel::save,
                    onAddOrEditCanceled = viewModel::onAddOrEditCanceled,
                    onSyncRequested = viewModel::sync,
                )
            }

            entry<Route.Templates> {
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
                    onBack = { navigator.goBack() },
                )
            }

            entry<Route.Logs> {
                val viewModel: LogScreenViewModel =
                    viewModel(factory = LogScreenViewModel.factory())

                val viewState by viewModel.viewState.collectAsStateWithLifecycle()

                LogScreen(
                    viewState = viewState,
                    onBackClick = { navigator.goBack() },
                    onClearLogsClick = viewModel::clearLogsClicked,
                    onTagClick = viewModel::tagClicked,
                    onSelectAllTags = viewModel::selectAllTagsClicked,
                    onUnselectAllTags = viewModel::unselectAllTagsClicked,
                )
            }

            entry<Route.Search> {
                val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory())

                val state by viewModel.state.collectAsStateWithLifecycle()
                SearchScreen(
                    state = state,
                    onBackClick = {
                        navigator.goBack()
                    },
                    onClearClick = viewModel::clearSearchTerm,
                    onTagClicked = viewModel::tagClicked,
                    onSelectAllTagsClicked = viewModel::selectAllTagsClicked,
                    onUnselectAllTagsClicked = viewModel::unselectAllTagsClicked,
                    onEndDateSelected = viewModel::onEndDateSelected,
                    onStartDateSelected = viewModel::onStartDateSelected,
                    onSortOrderChanged = viewModel::onSortOrderChanged,
                    onExactMatchToggled = viewModel::onExactMatchToggled,
                    onNavToViewJournalEntryDay = { entry ->
                        navigator.navigate(
                            Route.ViewJournalEntryDay(
                                date = entry.entryTime.date,
                                entryId = entry.id,
                            ),
                        )
                    },
                )
            }

            entry<Route.Import> {
                val viewModel: ImportViewModel = viewModel(factory = ImportViewModel.factory())

                val state by viewModel.state.collectAsStateWithLifecycle()
                ImportScreen(
                    state = state,
                    onBackClick = { navigator.goBack() },
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

            entry<Route.ViewJournalEntryDay> { arg ->
                val viewModel: ViewJournalEntryDayViewModel =
                    viewModel(factory = ServiceLocator.getViewJournalEntryDayVMFactory(arg))

                val viewState by viewModel.state.collectAsStateWithLifecycle()
                ViewJournalEntryDayScreen(
                    state = viewState,
                    onBackClick = { navigator.goBack() },
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

    NavDisplay(
        backStack = navigator.backstack,
        entryProvider = entryProvider,
        onBack = { navigator.goBack() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        modifier = modifier,
    )
}
