package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import com.ramitsuri.notificationjournal.core.ui.sideBorder
import com.ramitsuri.notificationjournal.core.ui.topBorder
import com.ramitsuri.notificationjournal.core.utils.getDay
import com.ramitsuri.notificationjournal.core.utils.getTime
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.conflict_from_other_device
import notificationjournal.core.generated.resources.conflict_this_device
import notificationjournal.core.generated.resources.conflicts
import notificationjournal.core.generated.resources.copy
import notificationjournal.core.generated.resources.copy_reconcile
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
import notificationjournal.core.generated.resources.reconcile
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.settings_upload_title
import notificationjournal.core.generated.resources.untagged
import notificationjournal.core.generated.resources.untagged_format
import notificationjournal.core.generated.resources.use
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun JournalEntryScreen(
    state: ViewState,
    onAddRequested: () -> Unit,
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
    onTagGroupReconcileRequested: (TagGroup) -> Unit,
    onTagGroupForceUploadRequested: (TagGroup) -> Unit,
    onSettingsClicked: () -> Unit,
    onConflictResolved: (JournalEntry, EntryConflict?) -> Unit,
    onSyncClicked: () -> Unit,
) {
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    // The view needs to be focussed for it to receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
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
                    it.type == KeyEventType.KeyUp
                ) {
                    onAddRequested()
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
                } else {
                    false
                }
            },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = onAddRequested
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
                .padding(start = 16.dp, end = 16.dp)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(
                    modifier = Modifier.height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
                )

                Toolbar(
                    notUploadedCount = state.notUploadedCount,
                    onSyncClicked = onSyncClicked,
                    onSettingsClicked = onSettingsClicked
                )

                if (state.dayGroups.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = 64.dp),
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
                        items = state.dayGroups,
                        conflicts = state.entryConflicts,
                        tags = state.tags,
                        onCopyRequested = { item ->
                            clipboardManager.setText(AnnotatedString(item.text))
                        },
                        onTagClicked = onEditTagRequested,
                        onTagGroupCopyRequested = { tagGroup ->
                            val text = tagGroup.entries
                                .joinToString(separator = "\n") { "- ${it.text}" }
                            clipboardManager.setText(AnnotatedString(text))
                        },
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
                        onTagGroupReconcileRequested = onTagGroupReconcileRequested,
                        onTagGroupForceUploadRequested = onTagGroupForceUploadRequested,
                        onForceUploadRequested = onForceUploadRequested,
                        onDuplicateRequested = onDuplicateRequested,
                        onConflictResolved = onConflictResolved,
                        modifier = Modifier.alpha(if (showContent) 1f else 0f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Toolbar(
    notUploadedCount: Int,
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
                .clickable(onClick = onSyncClicked),
        ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = stringResource(Res.string.settings_upload_title),
                modifier = Modifier.align(Alignment.Center)
            )
            if (notUploadedCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text("$notUploadedCount")
                }
            }
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun List(
    items: List<DayGroup>,
    conflicts: List<EntryConflict>,
    tags: List<Tag>,
    onCopyRequested: (JournalEntry) -> Unit,
    onTagClicked: (JournalEntry, String) -> Unit,
    onTagGroupCopyRequested: (TagGroup) -> Unit,
    onTagGroupDeleteRequested: (TagGroup) -> Unit,
    onTagGroupMoveToNextDayRequested: (TagGroup) -> Unit,
    onTagGroupMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onTagGroupReconcileRequested: (TagGroup) -> Unit,
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
    modifier: Modifier = Modifier,
) {
    val strokeWidth: Dp = 1.dp
    val strokeColor: Color = MaterialTheme.colorScheme.outline
    val cornerRadius: Dp = 16.dp
    val topShape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    val bottomShape = RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)

    LazyColumn(modifier = modifier) {
        items.forEach { dayGroup ->
            stickyHeader(key = dayGroup.date.toString()) {
                HeaderItem(
                    headerText = getDay(toFormat = dayGroup.date),
                    untaggedCount = dayGroup.untaggedCount,
                )
            }
            dayGroup.tagGroups.forEach { tagGroup ->
                val entries = tagGroup.entries
                var shape: Shape
                var borderModifier: Modifier
                item(key = dayGroup.date.toString().plus(tagGroup.tag)) {
                    SubHeaderItem(
                        tagGroup = tagGroup,
                        onCopyRequested = onTagGroupCopyRequested,
                        onDeleteRequested = onTagGroupDeleteRequested,
                        onMoveToNextDayRequested = onTagGroupMoveToNextDayRequested,
                        onMoveToPreviousDayRequested = onTagGroupMoveToPreviousDayRequested,
                        onReconcileRequested = onTagGroupReconcileRequested,
                        onForceUploadRequested = onTagGroupForceUploadRequested,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(topShape)
                            .background(color = MaterialTheme.colorScheme.background)
                            .then(Modifier.topBorder(strokeWidth, strokeColor, cornerRadius))
                            .padding(8.dp)
                    )
                }
                items(
                    count = entries.size,
                    key = { index -> entries[index].id }) { index ->
                    when (index) {
                        entries.size - 1 -> {
                            shape = bottomShape
                            borderModifier =
                                Modifier.bottomBorder(strokeWidth, strokeColor, cornerRadius)
                        }

                        else -> {
                            shape = RectangleShape
                            borderModifier =
                                Modifier.sideBorder(strokeWidth, strokeColor, cornerRadius)
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
                        modifier = Modifier
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
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun HeaderItem(headerText: String, untaggedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    ) {
        val text = buildString {
            append(headerText)
            if (untaggedCount > 0) {
                append(stringResource(Res.string.untagged_format, untaggedCount))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SubHeaderItem(
    tagGroup: TagGroup,
    onCopyRequested: (TagGroup) -> Unit,
    onDeleteRequested: (TagGroup) -> Unit,
    onMoveToNextDayRequested: (TagGroup) -> Unit,
    onMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onReconcileRequested: (TagGroup) -> Unit,
    onForceUploadRequested: (TagGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        var showMenu by remember { mutableStateOf(false) }
        Text(
            text = if (tagGroup.tag == Tag.NO_TAG.value) {
                stringResource(Res.string.untagged)
            } else {
                tagGroup.tag
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SubHeaderItemMenu(
            showMenu = showMenu,
            onCopyRequested = { onCopyRequested(tagGroup) },
            onDeleteRequested = { onDeleteRequested(tagGroup) },
            onMenuButtonClicked = { showMenu = !showMenu },
            onMoveToNextDayRequested = { onMoveToNextDayRequested(tagGroup) },
            onMoveToPreviousDayRequested = { onMoveToPreviousDayRequested(tagGroup) },
            onReconcileRequested = { onReconcileRequested(tagGroup) },
            onForceUploadRequested = { onForceUploadRequested(tagGroup) }
        )
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
        onDismiss = { showConflictResolutionDialog = false },
    )
}

@Composable
private fun ConflictResolutionDialog(
    showDialog: Boolean,
    entry: JournalEntry,
    conflicts: List<EntryConflict>,
    onConflictResolved: (EntryConflict?) -> Unit,
    onDismiss: () -> Unit,
) {
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
                    Text(
                        stringResource(Res.string.conflict_this_device),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    entry.tag?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        onDismiss()
                        onConflictResolved(null)
                    }) {
                        Text(stringResource(Res.string.use))
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    conflicts.forEach { conflict ->
                        Text(
                            stringResource(
                                Res.string.conflict_from_other_device,
                                conflict.senderName
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        conflict.tag?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(conflict.text, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            onDismiss()
                            onConflictResolved(conflict)
                        }) {
                            Text(stringResource(Res.string.use))
                        }
                    }
                }
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
    onReconcileRequested: () -> Unit,
    onForceUploadRequested: () -> Unit
) {
    var showingMoreMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = onMenuButtonClicked,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
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
                    text = { Text(stringResource(Res.string.copy_reconcile)) },
                    onClick = {
                        onMenuButtonClicked()
                        onCopyRequested()
                        onReconcileRequested()
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
                    text = { Text(stringResource(Res.string.copy)) },
                    onClick = {
                        onMenuButtonClicked()
                        onCopyRequested()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.reconcile)) },
                    onClick = {
                        onMenuButtonClicked()
                        onReconcileRequested()
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
        )
    }
}
