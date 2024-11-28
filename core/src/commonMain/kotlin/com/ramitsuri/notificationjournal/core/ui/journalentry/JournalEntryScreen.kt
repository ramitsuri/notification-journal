package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.bottomBorder
import com.ramitsuri.notificationjournal.core.ui.components.CountdownSnackbar
import com.ramitsuri.notificationjournal.core.ui.fullBorder
import com.ramitsuri.notificationjournal.core.ui.sideBorder
import com.ramitsuri.notificationjournal.core.ui.theme.green
import com.ramitsuri.notificationjournal.core.ui.theme.red
import com.ramitsuri.notificationjournal.core.ui.topBorder
import com.ramitsuri.notificationjournal.core.utils.getDateTime
import com.ramitsuri.notificationjournal.core.utils.getDay
import com.ramitsuri.notificationjournal.core.utils.getDiffAsAnnotatedText
import com.ramitsuri.notificationjournal.core.utils.getTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.conflict_this_device
import notificationjournal.core.generated.resources.conflicts
import notificationjournal.core.generated.resources.conflicts_format
import notificationjournal.core.generated.resources.copy
import notificationjournal.core.generated.resources.delete
import notificationjournal.core.generated.resources.delete_warning_message
import notificationjournal.core.generated.resources.duplicate
import notificationjournal.core.generated.resources.force_upload
import notificationjournal.core.generated.resources.menu_content_description
import notificationjournal.core.generated.resources.more
import notificationjournal.core.generated.resources.next_day
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.previous_day
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.sync_down
import notificationjournal.core.generated.resources.sync_up
import notificationjournal.core.generated.resources.untagged
import notificationjournal.core.generated.resources.untagged_format
import notificationjournal.core.generated.resources.will_reconcile
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    state: ViewState,
    onAddRequested: (LocalDate?) -> Unit,
    onEditRequested: (String) -> Unit,
    onDeleteRequested: (JournalEntry) -> Unit,
    onEditTagRequested: (JournalEntry, String) -> Unit,
    onMoveToNextDayRequested: (JournalEntry) -> Unit,
    onMoveToPreviousDayRequested: (JournalEntry) -> Unit,
    onDuplicateRequested: (JournalEntry) -> Unit,
    onForceUploadRequested: (JournalEntry) -> Unit,
    onMoveUpRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveToTopRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveDownRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveToBottomRequested: (JournalEntry, TagGroup) -> Unit,
    onTagGroupMoveToNextDayRequested: (TagGroup) -> Unit,
    onTagGroupMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onTagGroupDeleteRequested: (TagGroup) -> Unit,
    onTagGroupForceUploadRequested: (TagGroup) -> Unit,
    onSettingsClicked: () -> Unit,
    onConflictResolved: (JournalEntry, EntryConflict?) -> Unit,
    onSyncClicked: () -> Unit,
    onShowNextDayClicked: () -> Unit,
    onShowPreviousDayClicked: () -> Unit,
    onShowDayGroupClicked: (DayGroup) -> Unit,
    onCopyEntryRequested: (JournalEntry) -> Unit,
    onCopyTagGroupRequested: (TagGroup) -> Unit,
    onCopyDayGroupRequested: () -> Unit,
    onCopied: () -> Unit,
    onResetReceiveHelper: () -> Unit,
    onAddFromTagRequested: (LocalDate, String) -> Unit,
    onCancelReconcile: () -> Unit,
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
            onCopied()
        }
    }

    LaunchedEffect(state.snackBarType) {
        if (state.snackBarType is SnackBarType.Reconcile) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = reconcileSnackBarMessage,
                    actionLabel = reconcileSnackBarAction,
                    duration = SnackbarDuration.Indefinite
                )
                when (result) {
                    SnackbarResult.Dismissed -> {
                        // Do nothing
                    }

                    SnackbarResult.ActionPerformed -> {
                        onCancelReconcile()
                    }
                }
            }
        }
    }

    var showContent by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, lifecycleEvent ->
            if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
                showContent = true
            } else if (lifecycleEvent == Lifecycle.Event.ON_PAUSE) {
                showContent = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                            onDeleteRequested(forDeletion)
                        }
                        journalEntryForDelete = null
                    }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        journalEntryForDelete = null
                    }) {
                    Text(stringResource(Res.string.cancel))
                }
            })
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
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
                    onAddRequested(null)
                    true
                } else if (
                    it.isMetaPressed &&
                    it.key == Key.N &&
                    !it.isShiftPressed &&
                    it.type == KeyEventType.KeyUp
                ) {
                    onAddRequested(state.selectedDayGroup.date)
                    true
                } else if (
                    it.isMetaPressed &&
                    it.key == Key.Comma &&
                    it.type == KeyEventType.KeyUp
                ) {
                    onSettingsClicked()
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
                    onShowPreviousDayClicked()
                    true
                } else if ((it.key == Key.K || it.key == Key.DirectionRight) &&
                    it.type == KeyEventType.KeyDown
                ) {
                    onShowNextDayClicked()
                    true
                } else if (it.key == Key.D &&
                    it.type == KeyEventType.KeyDown
                ) {
                    showAllDays = true
                    true
                } else if (it.key == Key.S &&
                    it.type == KeyEventType.KeyDown
                ) {
                    onSyncClicked()
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
                onClick = { onAddRequested(null) }
            ) {
                Icon(
                    Icons.Filled.Add,
                    stringResource(Res.string.add_entry_content_description)
                )
            }
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
                rememberTopAppBarState()
            )

            Toolbar(
                notUploadedCount = state.notUploadedCount,
                onSyncClicked = onSyncClicked,
                onSettingsClicked = onSettingsClicked,
                onResetReceiveHelper = onResetReceiveHelper,
                scrollBehavior = scrollBehavior,
            )

            if (state.dayGroups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.no_items),
                        style = MaterialTheme.typography.displaySmall
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
                    onCopyRequested = onCopyEntryRequested,
                    onTagClicked = onEditTagRequested,
                    onTagGroupCopyRequested = onCopyTagGroupRequested,
                    onTagGroupDeleteRequested = onTagGroupDeleteRequested,
                    onMoveToNextDayRequested = onMoveToNextDayRequested,
                    onMoveToPreviousDayRequested = onMoveToPreviousDayRequested,
                    onMoveUpRequested = onMoveUpRequested,
                    onMoveToTopRequested = onMoveToTopRequested,
                    onMoveDownRequested = onMoveDownRequested,
                    onMoveToBottomRequested = onMoveToBottomRequested,
                    onEditRequested = { item ->
                        onEditRequested(item.id)
                    },
                    onDeleteRequested = { item ->
                        journalEntryForDelete = item
                    },
                    onTagGroupMoveToNextDayRequested = onTagGroupMoveToNextDayRequested,
                    onTagGroupMoveToPreviousDayRequested = onTagGroupMoveToPreviousDayRequested,
                    onTagGroupForceUploadRequested = onTagGroupForceUploadRequested,
                    onForceUploadRequested = onForceUploadRequested,
                    onDuplicateRequested = onDuplicateRequested,
                    onConflictResolved = onConflictResolved,
                    showConflictDiffInline = state.showConflictDiffInline,
                    onShowDayGroupClicked = onShowDayGroupClicked,
                    onShowHideAllDays = { showAllDays = it },
                    scrollConnection = scrollBehavior.nestedScrollConnection,
                    onDayGroupCopyRequested = onCopyDayGroupRequested,
                    onShowNextDay = onShowNextDayClicked,
                    onShowPreviousDay = onShowPreviousDayClicked,
                    onAddRequested = onAddFromTagRequested,
                    modifier = Modifier.fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}, enabled = !showContent
                        )
                        .alpha(if (showContent) 1f else 0f),
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    notUploadedCount: Int,
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onResetReceiveHelper: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults
            .centerAlignedTopAppBarColors()
            .copy(scrolledContainerColor = MaterialTheme.colorScheme.background),
        title = { },
        actions = {
            if (notUploadedCount > 0) {
                Box(
                    modifier = Modifier
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
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.sync_down),
                    contentDescription = null,
                )
            }
            IconButton(
                onClick = onSettingsClicked,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(Res.string.settings)
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun List(
    showAllDays: Boolean,
    dayGroup: DayGroup,
    items: List<DayGroup>,
    conflicts: List<EntryConflict>,
    tags: List<Tag>,
    dayGroupConflictCountMap: Map<DayGroup, Int>,
    showEmptyTags: Boolean,
    onCopyRequested: (JournalEntry) -> Unit,
    onTagClicked: (JournalEntry, String) -> Unit,
    onTagGroupCopyRequested: (TagGroup) -> Unit,
    onTagGroupDeleteRequested: (TagGroup) -> Unit,
    onTagGroupMoveToNextDayRequested: (TagGroup) -> Unit,
    onTagGroupMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onTagGroupForceUploadRequested: (TagGroup) -> Unit,
    onMoveUpRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveToTopRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveDownRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveToBottomRequested: (JournalEntry, TagGroup) -> Unit,
    onEditRequested: (JournalEntry) -> Unit,
    onDeleteRequested: (JournalEntry) -> Unit,
    onMoveToNextDayRequested: (JournalEntry) -> Unit,
    onMoveToPreviousDayRequested: (JournalEntry) -> Unit,
    onForceUploadRequested: (JournalEntry) -> Unit,
    onDuplicateRequested: (JournalEntry) -> Unit,
    onConflictResolved: (JournalEntry, EntryConflict?) -> Unit,
    showConflictDiffInline: Boolean,
    modifier: Modifier = Modifier,
    scrollConnection: NestedScrollConnection,
    onShowDayGroupClicked: (DayGroup) -> Unit,
    onShowHideAllDays: (Boolean) -> Unit,
    onDayGroupCopyRequested: () -> Unit,
    onShowNextDay: () -> Unit,
    onShowPreviousDay: () -> Unit,
    onAddRequested: (LocalDate, String) -> Unit,
) {
    val strokeWidth: Dp = 1.dp
    val strokeColor: Color = MaterialTheme.colorScheme.outline
    val cornerRadius: Dp = 16.dp
    val topShape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    val bottomShape = RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)

    HeaderItem(
        headerText = getDay(toFormat = dayGroup.date),
        untaggedCount = dayGroup.untaggedCount,
        conflictCount = dayGroupConflictCountMap[dayGroup] ?: 0,
        onCopyRequested = onDayGroupCopyRequested,
        onShowAllDaysClicked = { onShowHideAllDays(true) },
        onAddRequested = { onAddRequested(dayGroup.date, "") },
    )
    var swipeAmount by remember { mutableStateOf(0f) }
    LazyColumn(
        modifier = modifier
            .nestedScroll(scrollConnection)
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        swipeAmount = 0f
                    },
                    onDragEnd = {
                        if (swipeAmount.absoluteValue < (size.width / 4)) {
                            swipeAmount = 0f
                            return@detectHorizontalDragGestures
                        }
                        if (swipeAmount > 0) {
                            onShowPreviousDay()
                        } else {
                            onShowNextDay()
                        }
                        swipeAmount = 0f
                    },
                    onDragCancel = {
                        swipeAmount = 0f
                    },
                ) { change, dragAmount ->
                    change.consume()
                    swipeAmount += dragAmount
                }
            },
    ) {
        dayGroup.tagGroups.forEach { tagGroup ->
            val entries = tagGroup.entries
            val showAddButtonItem = (showEmptyTags || entries.isNotEmpty())
                    && tagGroup.tag != Tag.NO_TAG.value
            var shape: Shape
            var borderModifier: Modifier
            if (showEmptyTags || entries.isNotEmpty()) {
                stickyHeader(key = dayGroup.date.toString().plus(tagGroup.tag)) {
                    SubHeaderItem(
                        tagGroup = tagGroup,
                        onCopyRequested = onTagGroupCopyRequested,
                        onDeleteRequested = onTagGroupDeleteRequested,
                        onMoveToNextDayRequested = onTagGroupMoveToNextDayRequested,
                        onMoveToPreviousDayRequested = onTagGroupMoveToPreviousDayRequested,
                        onForceUploadRequested = onTagGroupForceUploadRequested,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.background)
                    )
                }
            }
            items(
                count = entries.size,
                key = { index -> entries[index].id }) { index ->
                when {
                    entries.size == 1 && !showAddButtonItem -> {
                        shape = RoundedCornerShape(16.dp)
                        borderModifier =
                            Modifier.fullBorder(strokeWidth, strokeColor, cornerRadius)
                    }

                    index == 0 -> {
                        shape = topShape
                        borderModifier =
                            Modifier.topBorder(strokeWidth, strokeColor, cornerRadius)
                    }

                    index == entries.lastIndex && !showAddButtonItem -> {
                        shape = bottomShape
                        borderModifier =
                            Modifier.bottomBorder(strokeWidth, strokeColor, cornerRadius)
                    }

                    else -> {
                        shape = RectangleShape
                        borderModifier =
                            Modifier.sideBorder(strokeWidth, strokeColor)
                    }
                }
                val entry = entries[index]
                ListItem(
                    item = entry,
                    conflicts = conflicts.filter { it.entryId == entry.id },
                    onCopyRequested = { onCopyRequested(entry) },
                    onDeleteRequested = { onDeleteRequested(entry) },
                    onEditRequested = { onEditRequested(entry) },
                    onMoveUpRequested = {
                        onMoveUpRequested(entry, tagGroup)
                    },
                    onMoveToTopRequested = {
                        onMoveToTopRequested(entry, tagGroup)
                    },
                    onMoveDownRequested = {
                        onMoveDownRequested(entry, tagGroup)
                    },
                    onMoveToBottomRequested = {
                        onMoveToBottomRequested(entry, tagGroup)
                    },
                    tags = tags,
                    selectedTag = entry.tag,
                    onTagClicked = { onTagClicked(entry, it) },
                    onMoveToNextDayRequested = { onMoveToNextDayRequested(entry) },
                    onMoveToPreviousDayRequested = { onMoveToPreviousDayRequested(entry) },
                    onDuplicateRequested = { onDuplicateRequested(entry) },
                    onForceUploadRequested = { onForceUploadRequested(entry) },
                    onConflictResolved = { onConflictResolved(entry, it) },
                    showConflictDiffInline = showConflictDiffInline,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .then(borderModifier)
                        .onKeyEvent {
                            if (it.key == Key.E && it.type == KeyEventType.KeyUp) {
                                onEditRequested(entry)
                                true
                            } else {
                                false
                            }
                        }
                )
            }
            if (showAddButtonItem) {
                item {
                    AddForTagButton(
                        isAddTheOnlyItem = entries.isEmpty(),
                        strokeWidth = strokeWidth,
                        strokeColor = strokeColor,
                        cornerRadius = cornerRadius,
                        defaultBottomShape = bottomShape,
                        onAddForTagButtonClick = {
                            onAddRequested(dayGroup.date, tagGroup.tag)
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    ShowAllDaysDialog(
        showAllDays = showAllDays,
        dayGroups = items,
        dayGroupConflictCountMap = dayGroupConflictCountMap,
        onDismiss = { onShowHideAllDays(false) },
        onDayGroupSelected = onShowDayGroupClicked,
    )
}

@Composable
private fun AddForTagButton(
    isAddTheOnlyItem: Boolean,
    strokeWidth: Dp,
    strokeColor: Color,
    cornerRadius: Dp,
    defaultBottomShape: Shape,
    onAddForTagButtonClick: () -> Unit,
) {
    val (addButtonShape, addButtonBorderModifier) =
        if (isAddTheOnlyItem) {
            RoundedCornerShape(16.dp) to
                    Modifier.fullBorder(strokeWidth, strokeColor.copy(alpha = 0.3f), cornerRadius)
        } else {
            defaultBottomShape to
                    Modifier.bottomBorder(strokeWidth, strokeColor, cornerRadius)
        }
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(addButtonShape)
            .then(addButtonBorderModifier),
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onAddForTagButtonClick,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.add_entry_content_description)
            )
        }
    }
}

@Composable
private fun HeaderItem(
    headerText: String,
    untaggedCount: Int,
    conflictCount: Int,
    onCopyRequested: () -> Unit,
    onShowAllDaysClicked: () -> Unit,
    onAddRequested: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onShowAllDaysClicked)
                .padding(8.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            val text = buildString {
                if (untaggedCount > 0) {
                    append(
                        stringResource(
                            Res.string.untagged_format,
                            untaggedCount,
                        )
                    )
                }
                if (conflictCount > 0) {
                    if (isNotEmpty()) {
                        append(", ")
                    }
                    append(
                        stringResource(
                            Res.string.conflicts_format,
                            conflictCount,
                        )
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
        Spacer(modifier = Modifier.width(4.dp))
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(
                onClick = onAddRequested,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.add_entry_content_description)
                )
            }
            IconButton(
                onClick = onCopyRequested,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(Res.string.add_entry_content_description)
                )
            }
        }
    }
}

@Composable
private fun SubHeaderItem(
    tagGroup: TagGroup,
    onCopyRequested: (TagGroup) -> Unit,
    onDeleteRequested: (TagGroup) -> Unit,
    onMoveToNextDayRequested: (TagGroup) -> Unit,
    onMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onForceUploadRequested: (TagGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.sizeIn(minHeight = 48.dp),
    ) {
        var showMenu by remember { mutableStateOf(false) }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (tagGroup.tag == Tag.NO_TAG.value) {
                stringResource(Res.string.untagged)
            } else {
                tagGroup.tag
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (tagGroup.entries.isNotEmpty()) {
            SubHeaderItemMenu(
                showMenu = showMenu,
                onCopyRequested = { onCopyRequested(tagGroup) },
                onDeleteRequested = { onDeleteRequested(tagGroup) },
                onMenuButtonClicked = { showMenu = !showMenu },
                onMoveToNextDayRequested = { onMoveToNextDayRequested(tagGroup) },
                onMoveToPreviousDayRequested = { onMoveToPreviousDayRequested(tagGroup) },
                onForceUploadRequested = { onForceUploadRequested(tagGroup) }
            )
        }
    }
}

@Composable
private fun ListItem(
    item: JournalEntry,
    conflicts: List<EntryConflict>,
    onCopyRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMoveUpRequested: () -> Unit,
    onMoveToTopRequested: () -> Unit,
    onMoveDownRequested: () -> Unit,
    onMoveToBottomRequested: () -> Unit,
    tags: List<Tag>,
    selectedTag: String?,
    onTagClicked: (String) -> Unit,
    onMoveToNextDayRequested: () -> Unit,
    onMoveToPreviousDayRequested: () -> Unit,
    onDuplicateRequested: () -> Unit,
    onForceUploadRequested: () -> Unit,
    onConflictResolved: (EntryConflict?) -> Unit,
    showConflictDiffInline: Boolean,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    var showConflictResolutionDialog by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = { showDetails = true })
    ) {
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        )
        if (conflicts.isNotEmpty()) {
            IconButton(
                onClick = { showConflictResolutionDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = stringResource(Res.string.conflicts)
                )
            }
        }
    }

    DetailsDialog(
        showDetails = showDetails,
        text = item.text,
        tags = tags,
        selectedTag = selectedTag,
        time = item.localDateTime(),
        onCopyRequested = onCopyRequested,
        onEditRequested = onEditRequested,
        onDeleteRequested = onDeleteRequested,
        onMoveUpRequested = onMoveUpRequested,
        onMoveToTopRequested = onMoveToTopRequested,
        onMoveDownRequested = onMoveDownRequested,
        onMoveToBottomRequested = onMoveToBottomRequested,
        onMoveToNextDayRequested = onMoveToNextDayRequested,
        onMoveToPreviousDayRequested = onMoveToPreviousDayRequested,
        onTagClicked = { tag -> onTagClicked(tag) },
        onDuplicateRequested = onDuplicateRequested,
        onForceUploadRequested = onForceUploadRequested,
        onDismiss = { showDetails = false }
    )

    ConflictResolutionDialog(
        showDialog = showConflictResolutionDialog,
        entry = item,
        conflicts = conflicts,
        onConflictResolved = onConflictResolved,
        showDiffInline = showConflictDiffInline,
        onDismiss = { showConflictResolutionDialog = false },
    )
}

@Composable
private fun ConflictResolutionDialog(
    showDialog: Boolean,
    entry: JournalEntry,
    conflicts: List<EntryConflict>,
    onConflictResolved: (EntryConflict?) -> Unit,
    showDiffInline: Boolean,
    onDismiss: () -> Unit,
) {
    val leftColor = MaterialTheme.colorScheme.red
    val rightColor = MaterialTheme.colorScheme.green
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true
            )
        ) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    EntryConflictView(
                        device = stringResource(Res.string.conflict_this_device),
                        entryTime = entry.entryTime,
                        timeZone = entry.timeZone,
                        text = AnnotatedString(entry.text),
                        tag = entry.tag,
                        onClick = {
                            onDismiss()
                            onConflictResolved(null)
                        }
                    )
                    conflicts.forEach { conflict ->
                        Spacer(modifier = Modifier.height(16.dp))
                        EntryConflictView(
                            device = conflict.senderName,
                            entryTime = if (conflict.entryTime != entry.entryTime) {
                                conflict.entryTime
                            } else {
                                null
                            },
                            timeZone = entry.timeZone,
                            text = if (conflict.text != entry.text) {
                                if (showDiffInline) {
                                    remember(conflict.id) {
                                        getDiffAsAnnotatedText(
                                            entry.text,
                                            conflict.text,
                                            leftColor,
                                            rightColor
                                        )
                                    }
                                } else {
                                    AnnotatedString(conflict.text)
                                }
                            } else {
                                null
                            },
                            tag = if (conflict.tag != null && conflict.tag != entry.tag) {
                                conflict.tag
                            } else {
                                null
                            },
                            showDiffInline = showDiffInline,
                            onClick = {
                                onDismiss()
                                onConflictResolved(conflict)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryConflictView(
    device: String,
    entryTime: Instant?,
    timeZone: TimeZone,
    text: AnnotatedString?,
    tag: String?,
    showDiffInline: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
    ) {
        Text(device, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            entryTime?.let {
                Text(getDateTime(it, timeZone), style = MaterialTheme.typography.bodySmall)
            }
            tag?.let {
                Text(
                    "\u00B7",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (entryTime != null || tag != null) {
            Spacer(modifier = Modifier.height(4.dp))
        }
        text?.let {
            if (showDiffInline) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsDialog(
    showDetails: Boolean,
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    time: LocalDateTime,
    onCopyRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMoveUpRequested: () -> Unit,
    onMoveToTopRequested: () -> Unit,
    onMoveDownRequested: () -> Unit,
    onMoveToBottomRequested: () -> Unit,
    onMoveToNextDayRequested: () -> Unit,
    onMoveToPreviousDayRequested: () -> Unit,
    onTagClicked: (String) -> Unit,
    onDuplicateRequested: () -> Unit,
    onForceUploadRequested: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The view needs to be focussed for it to receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showDetails, focusRequester) {
        if (showDetails) {
            focusRequester.requestFocus()
        }
    }

    var showTime by remember { mutableStateOf(false) }
    LaunchedEffect(showTime) {
        if (showTime) {
            delay(2000)
            showTime = false
        }
    }

    if (showDetails) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true
            )
        ) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .focusRequester(focusRequester)
                        .onKeyEvent {
                            if (it.key == Key.E && it.type == KeyEventType.KeyUp) {
                                onDismiss()
                                onEditRequested()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedIconButton(
                            onClick = onMoveToPreviousDayRequested,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(4.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                                contentDescription = stringResource(Res.string.previous_day)
                            )
                        }
                        Text(
                            if (showTime) {
                                getTime(toFormat = time.time)
                            } else {
                                getDay(toFormat = time.date)
                            },
                            modifier = Modifier
                                .clickable {
                                    showTime = !showTime
                                }
                                .padding(8.dp),
                        )
                        OutlinedIconButton(
                            onClick = onMoveToNextDayRequested,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(4.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                contentDescription = stringResource(Res.string.next_day)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                onDismiss()
                                onEditRequested()
                            })
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach {
                            FilterChip(
                                selected = it.value == selectedTag,
                                onClick = {
                                    onTagClicked(it.value)
                                    onDismiss()
                                },
                                label = { Text(text = it.value) })
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    TimeModifiers(
                        onMoveDownRequested = {
                            onMoveDownRequested()
                            onDismiss()
                        },
                        onMoveToBottomRequested = {
                            onMoveToBottomRequested()
                            onDismiss()
                        },
                        onMoveUpRequested = {
                            onMoveUpRequested()
                            onDismiss()
                        },
                        onMoveToTopRequested = {
                            onMoveToTopRequested()
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ButtonRow(
                        onCopyRequested = {
                            onCopyRequested()
                            onDismiss()
                        },
                        onDeleteRequested = {
                            onDeleteRequested()
                            onDismiss()
                        },
                        onDuplicateRequested = {
                            onDuplicateRequested()
                            onDismiss()
                        },
                        onForceUploadRequested = {
                            onForceUploadRequested()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
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
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true
            )
        ) {
            Card {
                LazyColumn(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                ) {
                    items(
                        count = dayGroups.size,
                        key = { index -> dayGroups[index].date.toString() }) { index ->
                        val dayGroup = dayGroups[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = {
                                        onDismiss()
                                        onDayGroupSelected(dayGroup)
                                    }
                                )
                                .padding(8.dp)
                            ) {
                                Text(getDay(toFormat = dayGroup.date))
                                val text = buildString {
                                    if (dayGroup.untaggedCount > 0) {
                                        append(
                                            stringResource(
                                                Res.string.untagged_format,
                                                dayGroup.untaggedCount,
                                            )
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
                                                )
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

@Composable
private fun TimeModifiers(
    modifier: Modifier = Modifier,
    onMoveUpRequested: () -> Unit,
    onMoveToTopRequested: () -> Unit,
    onMoveDownRequested: () -> Unit,
    onMoveToBottomRequested: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.KeyboardTab,
                rotateDegrees = 90F,
                onClick = onMoveToBottomRequested,
            )
            Button(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ArrowDownward,
                onClick = onMoveDownRequested,
            )
            Button(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ArrowUpward,
                onClick = onMoveUpRequested,
            )
            Button(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.KeyboardTab,
                rotateDegrees = 270F,
                onClick = onMoveToTopRequested,
            )
        }
    }
}

@Composable
private fun Button(
    icon: ImageVector,
    contentDescription: String? = null,
    rotateDegrees: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
                .rotate(rotateDegrees ?: 0F),
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun ButtonRow(
    onCopyRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDuplicateRequested: () -> Unit,
    onForceUploadRequested: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            icon = Icons.Filled.ContentCopy,
            contentDescription = stringResource(Res.string.copy),
            onClick = onCopyRequested,
            modifier = Modifier
                .weight(1f)
        )
        Button(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(Res.string.delete),
            onClick = onDeleteRequested,
            modifier = Modifier
                .weight(1f)
        )
        Button(
            icon = Icons.Filled.CopyAll,
            contentDescription = stringResource(Res.string.duplicate),
            onClick = onDuplicateRequested,
            modifier = Modifier
                .weight(1f)
        )
        Button(
            icon = Icons.Filled.Sync,
            contentDescription = stringResource(Res.string.force_upload),
            onClick = onForceUploadRequested,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
private fun SubHeaderItemMenu(
    showMenu: Boolean,
    onCopyRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMenuButtonClicked: () -> Unit,
    onMoveToNextDayRequested: () -> Unit,
    onMoveToPreviousDayRequested: () -> Unit,
    onForceUploadRequested: () -> Unit
) {
    var showingMoreMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = onMenuButtonClicked,
            modifier = Modifier
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(Res.string.menu_content_description)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = {
                onMenuButtonClicked()
                showingMoreMenu = false
            },
        ) {
            if (showingMoreMenu.not()) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.copy)) },
                    onClick = {
                        onMenuButtonClicked()
                        onCopyRequested()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.more)) },
                    onClick = {
                        showingMoreMenu = true
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.next_day)) },
                    onClick = {
                        onMenuButtonClicked()
                        onMoveToNextDayRequested()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.previous_day)) },
                    onClick = {
                        onMenuButtonClicked()
                        onMoveToPreviousDayRequested()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.delete)) },
                    onClick = {
                        onMenuButtonClicked()
                        onDeleteRequested()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.force_upload)) },
                    onClick = {
                        onMenuButtonClicked()
                        onForceUploadRequested()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ListItemPreview() {
    Surface {
        ListItem(
            item = JournalEntry(
                entryTime = Clock.System.now(),
                timeZone = TimeZone.currentSystemDefault(),
                text = "Test text"
            ),
            conflicts = listOf(),
            onCopyRequested = {},
            onEditRequested = {},
            onDeleteRequested = {},
            tags = listOf(),
            selectedTag = null,
            onTagClicked = { },
            onMoveToNextDayRequested = { },
            onMoveToPreviousDayRequested = { },
            onMoveUpRequested = { },
            onMoveDownRequested = { },
            onForceUploadRequested = { },
            onDuplicateRequested = { },
            onConflictResolved = { },
            onMoveToTopRequested = { },
            onMoveToBottomRequested = { },
            showConflictDiffInline = false,
        )
    }
}
