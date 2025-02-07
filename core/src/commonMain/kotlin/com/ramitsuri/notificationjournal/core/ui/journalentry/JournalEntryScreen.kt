package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.ui.components.CountdownSnackbar
import com.ramitsuri.notificationjournal.core.ui.components.DayGroupAction
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDay
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDayConfig
import com.ramitsuri.notificationjournal.core.utils.dayMonthDate
import kotlinx.coroutines.launch
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.conflicts_format
import notificationjournal.core.generated.resources.delete_warning_message
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.search
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.stats
import notificationjournal.core.generated.resources.stats_column_days
import notificationjournal.core.generated.resources.stats_column_entries
import notificationjournal.core.generated.resources.stats_row_all
import notificationjournal.core.generated.resources.stats_row_not_uploaded_not_reconciled
import notificationjournal.core.generated.resources.stats_row_not_uploaded_reconciled
import notificationjournal.core.generated.resources.stats_row_uploaded_not_reconciled
import notificationjournal.core.generated.resources.stats_row_uploaded_reconciled
import notificationjournal.core.generated.resources.sync_down
import notificationjournal.core.generated.resources.sync_up
import notificationjournal.core.generated.resources.untagged_format
import notificationjournal.core.generated.resources.view_journal_entry_day
import notificationjournal.core.generated.resources.will_reconcile
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    state: ViewState,
    onEntryScreenAction: (EntryScreenAction) -> Unit,
    onDayGroupAction: (DayGroupAction) -> Unit,
) {
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val reconcileSnackBarMessage = stringResource(Res.string.will_reconcile)
    val reconcileSnackBarAction = stringResource(Res.string.cancel)

    // The view needs to be focussed for it to receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.contentForCopy) {
        if (state.contentForCopy.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(state.contentForCopy))
            onEntryScreenAction(EntryScreenAction.Copy)
        }
    }

    LaunchedEffect(state.snackBarType) {
        if (state.snackBarType is SnackBarType.Reconcile) {
            coroutineScope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = reconcileSnackBarMessage,
                        actionLabel = reconcileSnackBarAction,
                        duration = SnackbarDuration.Indefinite,
                    )
                when (result) {
                    SnackbarResult.Dismissed -> {
                        // Do nothing
                    }

                    SnackbarResult.ActionPerformed -> {
                        onEntryScreenAction(EntryScreenAction.CancelReconcile)
                    }
                }
            }
        }
    }

    var showAllDays by remember { mutableStateOf(false) }

    if (journalEntryForDelete != null) {
        AlertDialog(
            onDismissRequest = { journalEntryForDelete = null },
            title = {
                Text(text = stringResource(Res.string.alert))
            },
            text = {
                Text(stringResource(Res.string.delete_warning_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        journalEntryForDelete?.let { forDeletion ->
                            onDayGroupAction(DayGroupAction.DeleteEntry(forDeletion))
                        }
                        journalEntryForDelete = null
                    },
                ) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        journalEntryForDelete = null
                    },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    if (
                        it.isMetaPressed &&
                        it.key == Key.N &&
                        it.isShiftPressed &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onEntryScreenAction(EntryScreenAction.AddWithDate(null))
                        true
                    } else if (
                        it.isMetaPressed &&
                        it.key == Key.N &&
                        !it.isShiftPressed &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onEntryScreenAction(EntryScreenAction.AddWithDate(state.selectedDayGroup.date))
                        true
                    } else if (
                        it.isMetaPressed &&
                        it.key == Key.Comma &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onEntryScreenAction(EntryScreenAction.NavToSettings)
                        true
                    } else if (it.key == Key.DirectionDown &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        focusManager.moveFocus(FocusDirection.Down)
                    } else if (it.key == Key.DirectionUp &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        focusManager.moveFocus(FocusDirection.Up)
                    } else if ((it.key == Key.J || it.key == Key.DirectionLeft) &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        onDayGroupAction(DayGroupAction.ShowPreviousDay)
                        true
                    } else if ((it.key == Key.K || it.key == Key.DirectionRight) &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        onDayGroupAction(DayGroupAction.ShowNextDay)
                        true
                    } else if (it.key == Key.D &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        showAllDays = true
                        true
                    } else if (it.key == Key.S &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        onEntryScreenAction(EntryScreenAction.Sync)
                        true
                    } else {
                        false
                    }
                },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            ) { data ->
                CountdownSnackbar(data)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = { onEntryScreenAction(EntryScreenAction.AddWithDate()) },
            ) {
                Icon(
                    Icons.Filled.Add,
                    stringResource(Res.string.add_entry_content_description),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val scrollBehavior =
                TopAppBarDefaults.enterAlwaysScrollBehavior(
                    rememberTopAppBarState(),
                )

            Toolbar(
                notUploadedCount = state.notUploadedCount,
                showLogsButton = state.showLogsButton,
                onSyncClicked = { onEntryScreenAction(EntryScreenAction.Sync) },
                onSettingsClicked = { onEntryScreenAction(EntryScreenAction.NavToSettings) },
                onSearchClicked = { onEntryScreenAction(EntryScreenAction.NavToSearch) },
                onResetReceiveHelper = { onEntryScreenAction(EntryScreenAction.ResetReceiveHelper) },
                onLogsClicked = { onEntryScreenAction(EntryScreenAction.NavToLogs) },
                onStatsRequestToggled = { onEntryScreenAction(EntryScreenAction.ShowStatsToggled) },
                onViewJournalEntryDayClicked = { onEntryScreenAction(EntryScreenAction.NavToViewJournalEntryDay) },
                scrollBehavior = scrollBehavior,
            )

            if (state.dayGroups.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.no_items),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            } else {
                List(
                    showAllDays = showAllDays,
                    dayGroup = state.selectedDayGroup,
                    items = state.dayGroups,
                    conflicts = state.entryConflicts,
                    tags = state.tags,
                    dayGroupConflictCountMap = state.dayGroupConflictCountMap,
                    showEmptyTags = state.showEmptyTags,
                    showConflictDiffInline = state.showConflictDiffInline,
                    onShowDayGroupClicked = { onEntryScreenAction(EntryScreenAction.ShowDayGroup(it)) },
                    onHideAllDays = { showAllDays = false },
                    scrollConnection = scrollBehavior.nestedScrollConnection,
                    onAction = { action ->
                        when (action) {
                            is DayGroupAction.DeleteEntry -> {
                                journalEntryForDelete = action.entry
                            }

                            is DayGroupAction.ShowAllDays -> {
                                showAllDays = true
                            }

                            else -> onDayGroupAction(action)
                        }
                    },
                )
            }
        }
        if (state.stats != null) {
            StatsDialog(
                stats = state.stats,
                onStatsRequestToggled = { onEntryScreenAction(EntryScreenAction.ShowStatsToggled) },
            )
        }
    }
}

@Composable
private fun StatsDialog(
    stats: EntryStats,
    onStatsRequestToggled: () -> Unit,
) {
    Dialog(
        onDismissRequest = onStatsRequestToggled,
    ) {
        Card {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatRow()
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_uploaded_reconciled),
                    statCount = stats.uploadedAndReconciled,
                    applyBackground = true,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_uploaded_not_reconciled),
                    statCount = stats.uploadedAndNotReconciled,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_not_uploaded_reconciled),
                    statCount = stats.notUploadedAndReconciled,
                    applyBackground = true,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_not_uploaded_not_reconciled),
                    statCount = stats.notUploadedAndNotReconciled,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_all),
                    statCount = stats.all,
                    applyBackground = true,
                )
            }
        }
    }
}

@Composable
fun StatRow(
    rowTitle: String? = null,
    statCount: EntryStats.Count? = null,
    applyBackground: Boolean = false,
) {
    val background =
        if (applyBackground) {
            Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
        } else {
            Modifier
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(background)
                .padding(4.dp),
    ) {
        if (rowTitle != null) {
            Text(
                text = rowTitle,
                modifier = Modifier.weight(3.5f),
            )
        } else {
            Spacer(modifier = Modifier.weight(3.5f))
        }
        if (statCount != null) {
            Text(
                text = statCount.days,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
            Text(
                text = statCount.entries,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End,
            )
        } else {
            Text(
                text = stringResource(Res.string.stats_column_days),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
            Text(
                text = stringResource(Res.string.stats_column_entries),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    notUploadedCount: Int,
    showLogsButton: Boolean,
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onResetReceiveHelper: () -> Unit,
    onLogsClicked: () -> Unit,
    onStatsRequestToggled: () -> Unit,
    onViewJournalEntryDayClicked: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors =
            TopAppBarDefaults
                .centerAlignedTopAppBarColors()
                .copy(scrolledContainerColor = MaterialTheme.colorScheme.background),
        title = { },
        actions = {
            if (showLogsButton) {
                IconButton(
                    onClick = onLogsClicked,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                    )
                }
            }
            if (notUploadedCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .height(48.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSyncClicked),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Text("$notUploadedCount", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            imageVector = vectorResource(Res.drawable.sync_up),
                            contentDescription = null,
                        )
                    }
                }
            }
            IconButton(
                onClick = onResetReceiveHelper,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.sync_down),
                    contentDescription = null,
                )
            }
            IconButton(
                onClick = onSearchClicked,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.search),
                )
            }
            IconButton(
                onClick = onStatsRequestToggled,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(Res.string.stats),
                )
            }
            IconButton(
                onClick = onViewJournalEntryDayClicked,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = stringResource(Res.string.view_journal_entry_day),
                )
            }
            IconButton(
                onClick = onSettingsClicked,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(Res.string.settings),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun List(
    showAllDays: Boolean,
    dayGroup: DayGroup,
    items: List<DayGroup>,
    conflicts: List<EntryConflict>,
    tags: List<Tag>,
    dayGroupConflictCountMap: Map<DayGroup, Int>,
    showEmptyTags: Boolean,
    showConflictDiffInline: Boolean,
    modifier: Modifier = Modifier,
    scrollConnection: NestedScrollConnection,
    onShowDayGroupClicked: (DayGroup) -> Unit,
    onHideAllDays: () -> Unit,
    onAction: (DayGroupAction) -> Unit,
) {
    JournalEntryDay(
        dayGroup = dayGroup,
        tags = tags,
        conflictCount = dayGroupConflictCountMap[dayGroup] ?: 0,
        conflicts = conflicts,
        scrollConnection = scrollConnection,
        showEmptyTags = showEmptyTags,
        showConflictDiffInline = showConflictDiffInline,
        onAction = onAction,
        config = JournalEntryDayConfig.allEnabled,
        modifier = modifier,
    )
    ShowAllDaysDialog(
        showAllDays = showAllDays,
        dayGroups = items,
        dayGroupConflictCountMap = dayGroupConflictCountMap,
        onDismiss = onHideAllDays,
        onDayGroupSelected = onShowDayGroupClicked,
    )
}

@Composable
private fun ShowAllDaysDialog(
    showAllDays: Boolean,
    dayGroups: List<DayGroup>,
    dayGroupConflictCountMap: Map<DayGroup, Int>,
    onDismiss: () -> Unit,
    onDayGroupSelected: (DayGroup) -> Unit,
) {
    // The view needs to be focussed for it to receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showAllDays, focusRequester) {
        if (showAllDays) {
            focusRequester.requestFocus()
        }
    }
    if (showAllDays) {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = true,
                ),
        ) {
            Card {
                LazyColumn(
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth(0.9f)
                            .padding(16.dp),
                ) {
                    items(
                        count = dayGroups.size,
                        key = { index -> dayGroups[index].date.toString() },
                    ) { index ->
                        val dayGroup = dayGroups[index]
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            onClick = {
                                                onDismiss()
                                                onDayGroupSelected(dayGroup)
                                            },
                                        )
                                        .padding(8.dp),
                            ) {
                                Text(dayMonthDate(toFormat = dayGroup.date))
                                val text =
                                    buildString {
                                        if (dayGroup.untaggedCount > 0) {
                                            append(
                                                stringResource(
                                                    Res.string.untagged_format,
                                                    dayGroup.untaggedCount,
                                                ),
                                            )
                                        }
                                        dayGroupConflictCountMap[dayGroup]
                                            ?.takeIf { it > 0 }
                                            ?.let {
                                                if (isNotEmpty()) {
                                                    append(", ")
                                                }
                                                append(
                                                    stringResource(
                                                        Res.string.conflicts_format,
                                                        it,
                                                    ),
                                                )
                                            }
                                    }
                                if (text.isNotEmpty()) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            if (index != dayGroups.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
