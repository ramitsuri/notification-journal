package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.model.DateWithCount
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.components.CountdownSnackbar
import com.ramitsuri.notificationjournal.core.ui.components.CyclicViewPager
import com.ramitsuri.notificationjournal.core.ui.components.DayGroupAction
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDay
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDayConfig
import com.ramitsuri.notificationjournal.core.utils.dayMonthDate
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYear
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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
                        onEntryScreenAction(EntryScreenAction.AddWithDate(state.dayGroup.date))
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
                onSyncClicked = { onEntryScreenAction(EntryScreenAction.Sync) },
                onSettingsClicked = { onEntryScreenAction(EntryScreenAction.NavToSettings) },
                onSearchClicked = { onEntryScreenAction(EntryScreenAction.NavToSearch) },
                onResetReceiveHelper = { onEntryScreenAction(EntryScreenAction.ResetReceiveHelper) },
                scrollBehavior = scrollBehavior,
            )
            if (state.dateWithCountList.isNotEmpty()) {
                ViewPagerContent(
                    state = state,
                    showAllDays = showAllDays,
                    onShowHideAllDays = { showAllDays = it },
                    onEntryScreenAction = onEntryScreenAction,
                    onDayGroupAction = onDayGroupAction,
                    scrollBehavior = scrollBehavior,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewPagerContent(
    state: ViewState,
    showAllDays: Boolean,
    onShowHideAllDays: (Boolean) -> Unit,
    onEntryScreenAction: (EntryScreenAction) -> Unit,
    onDayGroupAction: (DayGroupAction) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val initialPage =
        state
            .dateWithCountList
            .indexOfFirst { it.date == state.dayGroup.date }
            .takeIf { it > 0 }
            ?: 0
    var currentPage by remember { mutableStateOf(initialPage) }
    var isScrollInProgress by remember { mutableStateOf(false) }
    CyclicViewPager(
        initialPage = initialPage,
        pageCount = state.dateWithCountList.size,
        onActualPageChange = {
            currentPage = it
            onEntryScreenAction(EntryScreenAction.ShowDayGroup(state.dateWithCountList[it].date))
        },
        isScrollInProgress = { isScrollInProgress = it },
    ) { page ->
        AnimatedContent(isScrollInProgress) { scrollInProgress ->
            if (scrollInProgress) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            dayMonthDateWithYear(
                                toFormat = state.dateWithCountList[page].date,
                                dayDateSeparatorUsesNewLine = true,
                            ),
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    JournalEntryDayOrEmptyContent(
                        state = state,
                        showAllDays = showAllDays,
                        onEntryScreenAction = onEntryScreenAction,
                        scrollBehavior = scrollBehavior,
                        onDayGroupAction = onDayGroupAction,
                        onShowHideAllDays = onShowHideAllDays,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEntryDayOrEmptyContent(
    state: ViewState,
    showAllDays: Boolean,
    onEntryScreenAction: (EntryScreenAction) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onDayGroupAction: (DayGroupAction) -> Unit,
    onShowHideAllDays: (Boolean) -> Unit,
) {
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    if (state.dayGroup.tagGroups.isEmpty()) {
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
        JournalEntryDay(
            showAllDays = showAllDays,
            dayGroup = state.dayGroup,
            dateWithCountList = state.dateWithCountList,
            conflicts = state.entryConflicts,
            tags = state.tags,
            showEmptyTags = state.showEmptyTags,
            showConflictDiffInline = state.showConflictDiffInline,
            allowNotify = state.allowNotify,
            onShowDayGroupClicked = { onEntryScreenAction(EntryScreenAction.ShowDayGroup(it)) },
            onHideAllDays = { onShowHideAllDays(false) },
            scrollConnection = scrollBehavior.nestedScrollConnection,
            onViewByDate = { onEntryScreenAction(EntryScreenAction.NavToViewJournalEntryDay) },
            onAction = { action ->
                when (action) {
                    is DayGroupAction.DeleteEntry -> {
                        journalEntryForDelete = action.entry
                    }

                    is DayGroupAction.ShowAllDays -> {
                        onShowHideAllDays(true)
                    }

                    else -> onDayGroupAction(action)
                }
            },
        )
    }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    notUploadedCount: Int,
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onResetReceiveHelper: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors =
            TopAppBarDefaults
                .centerAlignedTopAppBarColors()
                .copy(scrolledContainerColor = MaterialTheme.colorScheme.background),
        title = { },
        actions = {
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
private fun JournalEntryDay(
    showAllDays: Boolean,
    dayGroup: DayGroup,
    dateWithCountList: List<DateWithCount>,
    conflicts: List<EntryConflict>,
    tags: List<Tag>,
    showEmptyTags: Boolean,
    showConflictDiffInline: Boolean,
    allowNotify: Boolean,
    modifier: Modifier = Modifier,
    scrollConnection: NestedScrollConnection,
    onShowDayGroupClicked: (LocalDate) -> Unit,
    onHideAllDays: () -> Unit,
    onViewByDate: () -> Unit,
    onAction: (DayGroupAction) -> Unit,
) {
    JournalEntryDay(
        dayGroup = dayGroup,
        tags = tags,
        conflictCount = dateWithCountList.firstOrNull { it.date == dayGroup.date }?.conflictCount ?: 0,
        conflicts = conflicts,
        scrollConnection = scrollConnection,
        showEmptyTags = showEmptyTags,
        showConflictDiffInline = showConflictDiffInline,
        onAction = onAction,
        config = JournalEntryDayConfig.allEnabled,
        allowNotify = allowNotify,
        modifier = modifier,
    )
    ShowAllDaysDialog(
        showAllDays = showAllDays,
        dateWithCountList = dateWithCountList,
        onDismiss = onHideAllDays,
        onDayGroupSelected = onShowDayGroupClicked,
        onViewByDate = onViewByDate,
    )
}

@Composable
private fun ShowAllDaysDialog(
    showAllDays: Boolean,
    dateWithCountList: List<DateWithCount>,
    onDismiss: () -> Unit,
    onDayGroupSelected: (LocalDate) -> Unit,
    onViewByDate: () -> Unit,
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
                    dismissOnClickOutside = true,
                ),
        ) {
            Card {
                LazyColumn(
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(
                        count = dateWithCountList.size,
                        key = { index -> dateWithCountList[index].date.toString() },
                    ) { index ->
                        val dateWithCount = dateWithCountList[index]
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
                                                onDayGroupSelected(dateWithCount.date)
                                            },
                                        )
                                        .padding(8.dp),
                            ) {
                                Text(dayMonthDate(toFormat = dateWithCount.date))
                                val text =
                                    buildString {
                                        if (dateWithCount.untaggedCount > 0) {
                                            append(
                                                stringResource(
                                                    Res.string.untagged_format,
                                                    dateWithCount.untaggedCount,
                                                ),
                                            )
                                        }
                                        dateWithCount
                                            .conflictCount
                                            .takeIf { it > 0 }
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
                            HorizontalDivider()
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = onViewByDate,
                            modifier = Modifier,
                        ) {
                            Text(text = stringResource(Res.string.view_journal_entry_day))
                        }
                    }
                }
            }
        }
    }
}
