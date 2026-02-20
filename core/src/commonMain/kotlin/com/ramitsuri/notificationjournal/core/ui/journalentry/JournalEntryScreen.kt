package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.components.DayGroupAction
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDay
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDayConfig
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.back
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.delete_warning_message
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.search
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.sync_up
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun JournalEntryScreen(
    state: ViewState,
    showBackButton: Boolean,
    onEntryScreenAction: (EntryScreenAction) -> Unit,
    onDayGroupAction: (DayGroupAction) -> Unit,
) {
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val focusManager = LocalFocusManager.current

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

    if (journalEntryForDelete != null) {
        DeleteDialog(
            journalEntryForDelete = journalEntryForDelete,
            onDismiss = { journalEntryForDelete = null },
            onDayGroupAction = onDayGroupAction,
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
                        if (state.dayGroup == null) {
                            false
                        } else {
                            onEntryScreenAction(EntryScreenAction.AddWithDate(state.dayGroup.date))
                            true
                        }
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
                    } else if (it.key == Key.S &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        onEntryScreenAction(EntryScreenAction.Sync)
                        true
                    } else {
                        false
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
                showBackButton = showBackButton,
                isConnected = state.isConnected,
                notUploadedCount = state.notUploadedCount,
                onViewByDate = { onEntryScreenAction(EntryScreenAction.NavToViewJournalEntryDay) },
                onBackClick = { onEntryScreenAction(EntryScreenAction.NavBack) },
                onSyncClicked = { onEntryScreenAction(EntryScreenAction.Sync) },
                onSettingsClicked = { onEntryScreenAction(EntryScreenAction.NavToSettings) },
                onSearchClicked = { onEntryScreenAction(EntryScreenAction.NavToSearch) },
                onResetReceiveHelper = { onEntryScreenAction(EntryScreenAction.ResetReceiveHelper) },
                scrollBehavior = scrollBehavior,
            )

            val dayGroup = state.dayGroup
            if (dayGroup == null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            } else if (dayGroup.tagGroups.isEmpty()) {
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
                    dayGroup = state.dayGroup,
                    conflictCount = state.dayGroupConflictCount,
                    conflicts = state.entryConflicts,
                    tags = state.tags,
                    showEmptyTags = state.showEmptyTags,
                    showConflictDiffInline = state.showConflictDiffInline,
                    allowNotify = state.allowNotify,
                    scrollConnection = scrollBehavior.nestedScrollConnection,
                    onAction = { action ->
                        when (action) {
                            is DayGroupAction.DeleteEntry -> {
                                journalEntryForDelete = action.entry
                            }

                            else -> onDayGroupAction(action)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DeleteDialog(
    journalEntryForDelete: JournalEntry?,
    onDismiss: () -> Unit,
    onDayGroupAction: (DayGroupAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun Toolbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean,
    isConnected: Boolean,
    notUploadedCount: Int,
    onViewByDate: () -> Unit,
    onBackClick: () -> Unit,
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
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onViewByDate,
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                )
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
                    imageVector = if (isConnected) Icons.Outlined.Link else Icons.Outlined.LinkOff,
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
private fun List(
    dayGroup: DayGroup,
    conflictCount: Int,
    conflicts: List<EntryConflict>,
    tags: List<Tag>,
    showEmptyTags: Boolean,
    showConflictDiffInline: Boolean,
    allowNotify: Boolean,
    modifier: Modifier = Modifier,
    scrollConnection: NestedScrollConnection,
    onAction: (DayGroupAction) -> Unit,
) {
    JournalEntryDay(
        dayGroup = dayGroup,
        tags = tags,
        conflictCount = conflictCount,
        conflicts = conflicts,
        scrollConnection = scrollConnection,
        showEmptyTags = showEmptyTags,
        showConflictDiffInline = showConflictDiffInline,
        onAction = onAction,
        config = JournalEntryDayConfig.allEnabled,
        allowNotify = allowNotify,
        modifier = modifier,
    )
}
