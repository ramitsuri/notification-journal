package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.NotifyTime
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.bottomBorder
import com.ramitsuri.notificationjournal.core.ui.fullBorder
import com.ramitsuri.notificationjournal.core.ui.sideBorder
import com.ramitsuri.notificationjournal.core.ui.theme.green
import com.ramitsuri.notificationjournal.core.ui.theme.red
import com.ramitsuri.notificationjournal.core.ui.topBorder
import com.ramitsuri.notificationjournal.core.utils.dayMonthDate
import com.ramitsuri.notificationjournal.core.utils.getDiffAsAnnotatedText
import com.ramitsuri.notificationjournal.core.utils.hourMinute
import com.ramitsuri.notificationjournal.core.utils.monthDayHourMinute
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.conflict_this_device
import notificationjournal.core.generated.resources.conflicts
import notificationjournal.core.generated.resources.conflicts_format
import notificationjournal.core.generated.resources.copy
import notificationjournal.core.generated.resources.delete
import notificationjournal.core.generated.resources.duplicate
import notificationjournal.core.generated.resources.menu_content_description
import notificationjournal.core.generated.resources.next_day
import notificationjournal.core.generated.resources.notify
import notificationjournal.core.generated.resources.previous_day
import notificationjournal.core.generated.resources.untagged
import notificationjournal.core.generated.resources.untagged_format
import notificationjournal.core.generated.resources.upload
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue
import kotlin.time.Duration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalEntryDay(
    dayGroup: DayGroup,
    tags: List<Tag>,
    conflictCount: Int,
    conflicts: List<EntryConflict>,
    scrollConnection: NestedScrollConnection,
    showEmptyTags: Boolean,
    showConflictDiffInline: Boolean,
    config: JournalEntryDayConfig,
    entryDayHighlight: EntryDayHighlight? = null,
    allowNotify: Boolean,
    onAction: (DayGroupAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokeWidth: Dp = 1.dp
    val strokeColor: Color = MaterialTheme.colorScheme.outline
    val cornerRadius: Dp = 16.dp
    val topShape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    val bottomShape = RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)

    val lazyColumnState = rememberLazyListState(initialFirstVisibleItemIndex = entryDayHighlight?.index ?: 0)
    var highlightEntryId: String? by remember { mutableStateOf(null) }
    var showContent by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, lifecycleEvent ->
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
    LaunchedEffect(entryDayHighlight) {
        repeat(3) {
            delay(200)
            highlightEntryId = entryDayHighlight?.entryId
            delay(200)
            highlightEntryId = null
        }
    }
    HeaderItem(
        headerText = dayMonthDate(toFormat = dayGroup.date),
        untaggedCount = dayGroup.untaggedCount,
        conflictCount = conflictCount,
        allowCopy = config.allowCopy,
        allowUpload = config.allowUpload,
        allowDaySelection = config.allowDaySelection,
        onCopyRequested = { onAction(DayGroupAction.CopyDayGroup) },
        onShowAllDaysClicked = { onAction(DayGroupAction.ShowAllDays) },
        onUploadRequested = { onAction(DayGroupAction.UploadDayGroup) },
    )
    var swipeAmount by remember { mutableStateOf(0f) }
    LazyColumn(
        state = lazyColumnState,
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    enabled = !showContent,
                )
                .alpha(if (showContent) 1f else 0f)
                .nestedScroll(scrollConnection)
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            swipeAmount = 0f
                        },
                        onDragEnd = {
                            if (!config.allowDaySelection) {
                                return@detectHorizontalDragGestures
                            }
                            if (swipeAmount.absoluteValue < (size.width / 4)) {
                                swipeAmount = 0f
                                return@detectHorizontalDragGestures
                            }
                            if (swipeAmount > 0) {
                                onAction(DayGroupAction.ShowPreviousDay)
                            } else {
                                onAction(DayGroupAction.ShowNextDay)
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
            val showAddButtonItem =
                (showEmptyTags || entries.isNotEmpty()) &&
                    tagGroup.tag != Tag.NO_TAG.value &&
                    config.allowAdd
            var shape: Shape
            var borderModifier: Modifier
            if (showEmptyTags || entries.isNotEmpty()) {
                stickyHeader(key = dayGroup.date.toString().plus(tagGroup.tag)) {
                    SubHeaderItem(
                        tagGroup = tagGroup,
                        allowTagMenu = config.allowTagMenu,
                        onCopyRequested = { onAction(DayGroupAction.CopyTagGroup(it)) },
                        onDeleteRequested = { onAction(DayGroupAction.DeleteTagGroup(it)) },
                        onMoveToNextDayRequested = {
                            onAction(DayGroupAction.MoveTagGroupToNextDay(it))
                        },
                        onMoveToPreviousDayRequested = {
                            onAction(DayGroupAction.MoveTagGroupToPreviousDay(it))
                        },
                        onUploadRequested = { onAction(DayGroupAction.UploadTagGroup(it)) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.background),
                    )
                }
            }
            items(
                count = entries.size,
                key = { index -> entries[index].id },
            ) { index ->
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
                    highlight = highlightEntryId == entry.id,
                    allowEdits = config.allowEdits,
                    allowNotify = allowNotify,
                    conflicts = conflicts.filter { it.entryId == entry.id },
                    onCopyRequested = { onAction(DayGroupAction.CopyEntry(entry)) },
                    onDeleteRequested = { onAction(DayGroupAction.DeleteEntry(entry)) },
                    onEditRequested = { onAction(DayGroupAction.EditEntry(entry)) },
                    onMoveUpRequested = {
                        onAction(DayGroupAction.MoveEntryUp(entry, tagGroup))
                    },
                    onMoveToTopRequested = {
                        onAction(DayGroupAction.MoveEntryToTop(entry, tagGroup))
                    },
                    onMoveDownRequested = {
                        onAction(DayGroupAction.MoveEntryDown(entry, tagGroup))
                    },
                    onMoveToBottomRequested = {
                        onAction(DayGroupAction.MoveEntryToBottom(entry, tagGroup))
                    },
                    tags = tags,
                    selectedTag = entry.tag,
                    onTagClicked = { onAction(DayGroupAction.EditTag(entry, it)) },
                    onMoveToNextDayRequested = { onAction(DayGroupAction.MoveEntryToNextDay(entry)) },
                    onMoveToPreviousDayRequested = {
                        onAction(
                            DayGroupAction.MoveEntryToPreviousDay(
                                entry,
                            ),
                        )
                    },
                    onDuplicateRequested = { onAction(DayGroupAction.DuplicateEntry(entry)) },
                    onUploadRequested = { onAction(DayGroupAction.UploadEntry(entry)) },
                    onConflictResolved = { onAction(DayGroupAction.ResolveConflict(entry, it)) },
                    onNotifyTimePicked = { onAction(DayGroupAction.Notify(entry, it)) },
                    showConflictDiffInline = showConflictDiffInline,
                    modifier =
                        Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                            .clip(shape)
                            .then(borderModifier)
                            .onKeyEvent {
                                if (it.key == Key.E && it.type == KeyEventType.KeyUp) {
                                    onAction(DayGroupAction.EditEntry(entry))
                                    true
                                } else {
                                    false
                                }
                            },
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
                            onAction(
                                DayGroupAction.AddEntry(
                                    dayGroup.date,
                                    tagGroup.timeAfterLastEntry,
                                    tagGroup.tag,
                                ),
                            )
                        },
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
                Modifier.fullBorder(
                    strokeWidth,
                    strokeColor.copy(alpha = 0.3f),
                    cornerRadius,
                )
        } else {
            defaultBottomShape to
                Modifier.bottomBorder(strokeWidth, strokeColor, cornerRadius)
        }
    Row(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .clip(addButtonShape)
                .then(addButtonBorderModifier),
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onAddForTagButtonClick,
            modifier =
                Modifier
                    .size(48.dp)
                    .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.add_entry_content_description),
            )
        }
    }
}

@Composable
private fun HeaderItem(
    headerText: String,
    untaggedCount: Int,
    conflictCount: Int,
    allowCopy: Boolean,
    allowUpload: Boolean,
    allowDaySelection: Boolean,
    onCopyRequested: () -> Unit,
    onShowAllDaysClicked: () -> Unit,
    onUploadRequested: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .clickable(enabled = allowDaySelection, onClick = onShowAllDaysClicked)
                    .padding(8.dp)
                    .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            val text =
                buildString {
                    if (untaggedCount > 0) {
                        append(
                            stringResource(
                                Res.string.untagged_format,
                                untaggedCount,
                            ),
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
        Spacer(modifier = Modifier.width(4.dp))
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (allowUpload) {
                IconButton(
                    onClick = onUploadRequested,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                    )
                }
            }
            if (allowCopy) {
                IconButton(
                    onClick = onCopyRequested,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubHeaderItem(
    tagGroup: TagGroup,
    allowTagMenu: Boolean,
    onCopyRequested: (TagGroup) -> Unit,
    onDeleteRequested: (TagGroup) -> Unit,
    onMoveToNextDayRequested: (TagGroup) -> Unit,
    onMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onUploadRequested: (TagGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.sizeIn(minHeight = 48.dp),
    ) {
        var showMenu by remember { mutableStateOf(false) }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text =
                if (tagGroup.tag == Tag.NO_TAG.value) {
                    stringResource(Res.string.untagged)
                } else {
                    tagGroup.tag
                },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (allowTagMenu && tagGroup.entries.isNotEmpty()) {
            SubHeaderItemMenu(
                showMenu = showMenu,
                onCopyRequested = { onCopyRequested(tagGroup) },
                onDeleteRequested = { onDeleteRequested(tagGroup) },
                onMenuButtonClicked = { showMenu = !showMenu },
                onMoveToNextDayRequested = { onMoveToNextDayRequested(tagGroup) },
                onMoveToPreviousDayRequested = { onMoveToPreviousDayRequested(tagGroup) },
                onUploadRequested = { onUploadRequested(tagGroup) },
            )
        }
    }
}

@Composable
private fun ListItem(
    item: JournalEntry,
    highlight: Boolean,
    allowEdits: Boolean,
    allowNotify: Boolean,
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
    onUploadRequested: () -> Unit,
    onConflictResolved: (EntryConflict?) -> Unit,
    onNotifyTimePicked: (Duration) -> Unit,
    showConflictDiffInline: Boolean,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    var showConflictResolutionDialog by remember { mutableStateOf(false) }
    val highlightColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .applyIf(highlight) {
                    background(highlightColor)
                }
                .clickable(onClick = { if (allowEdits) showDetails = true else onCopyRequested() }),
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (allowEdits && conflicts.isNotEmpty()) {
            IconButton(
                onClick = { showConflictResolutionDialog = true },
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = stringResource(Res.string.conflicts),
                )
            }
        }
    }

    DetailsDialog(
        allowEdits = allowEdits,
        showDetails = showDetails,
        text = item.text,
        tags = tags,
        selectedTag = selectedTag,
        time = item.entryTime,
        allowNotify = allowNotify,
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
        onUploadRequested = onUploadRequested,
        onDismiss = { showDetails = false },
        onNotifyTimePicked = onNotifyTimePicked,
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
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = true,
                ),
        ) {
            Card {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    EntryConflictView(
                        device = stringResource(Res.string.conflict_this_device),
                        entryTime = entry.entryTime,
                        text = AnnotatedString(entry.text),
                        tag = entry.tag,
                        onClick = {
                            onDismiss()
                            onConflictResolved(null)
                        },
                    )
                    conflicts.forEach { conflict ->
                        Spacer(modifier = Modifier.height(16.dp))
                        EntryConflictView(
                            device = conflict.senderName,
                            entryTime =
                                if (conflict.entryTime != entry.entryTime) {
                                    conflict.entryTime
                                } else {
                                    null
                                },
                            text =
                                if (conflict.text != entry.text) {
                                    if (showDiffInline) {
                                        remember(conflict.id) {
                                            getDiffAsAnnotatedText(
                                                entry.text,
                                                conflict.text,
                                                leftColor,
                                                rightColor,
                                            )
                                        }
                                    } else {
                                        AnnotatedString(conflict.text)
                                    }
                                } else {
                                    null
                                },
                            tag =
                                if (conflict.tag != entry.tag) {
                                    conflict.tag
                                } else {
                                    null
                                },
                            showDiffInline = showDiffInline,
                            onClick = {
                                onDismiss()
                                onConflictResolved(conflict)
                            },
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
    entryTime: LocalDateTime?,
    text: AnnotatedString?,
    tag: String?,
    showDiffInline: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .border(
                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
    ) {
        Text(device, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            entryTime?.let {
                Text(monthDayHourMinute(it), style = MaterialTheme.typography.bodySmall)
            }
            tag?.let {
                Text(
                    "\u00B7",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
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
    allowEdits: Boolean,
    showDetails: Boolean,
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    time: LocalDateTime,
    allowNotify: Boolean,
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
    onUploadRequested: () -> Unit,
    onDismiss: () -> Unit,
    onNotifyTimePicked: (Duration) -> Unit,
) {
    // The view needs to be focussed for it to receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showDetails, focusRequester) {
        if (showDetails) {
            focusRequester.requestFocus()
        }
    }

    var showDate by remember { mutableStateOf(false) }
    LaunchedEffect(showDate) {
        if (showDate) {
            delay(2000)
            showDate = false
        }
    }

    if (showDetails) {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = true,
                ),
        ) {
            Card {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp)
                            .focusRequester(focusRequester)
                            .onKeyEvent {
                                if (it.key == Key.E && it.type == KeyEventType.KeyUp && allowEdits) {
                                    onDismiss()
                                    onEditRequested()
                                    true
                                } else {
                                    false
                                }
                            },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (allowEdits) {
                            OutlinedIconButton(
                                onClick = onMoveToPreviousDayRequested,
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .padding(4.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                                    contentDescription = stringResource(Res.string.previous_day),
                                )
                            }
                        }
                        Text(
                            if (showDate) {
                                dayMonthDate(toFormat = time.date)
                            } else {
                                hourMinute(toFormat = time.time)
                            },
                            modifier =
                                Modifier
                                    .clickable {
                                        showDate = !showDate
                                    }
                                    .padding(8.dp),
                        )
                        if (allowEdits) {
                            OutlinedIconButton(
                                onClick = onMoveToNextDayRequested,
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .padding(4.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = stringResource(Res.string.next_day),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = allowEdits,
                                    onClick = {
                                        onDismiss()
                                        onEditRequested()
                                    },
                                )
                                .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (allowNotify) {
                            NotifyButton(onNotifyTimePicked = onNotifyTimePicked)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach {
                            FilterChip(
                                enabled = allowEdits,
                                selected = it.value == selectedTag,
                                onClick = {
                                    onTagClicked(it.value)
                                    onDismiss()
                                },
                                label = { Text(text = it.value) },
                            )
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
                        },
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
                        onUploadRequested = {
                            onUploadRequested()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NotifyButton(onNotifyTimePicked: (Duration) -> Unit) {
    var showNotifyTimes by remember { mutableStateOf(false) }
    Spacer(modifier = Modifier.weight(1f))
    Box {
        IconButton(
            onClick = { showNotifyTimes = true },
            modifier =
                Modifier
                    .size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = stringResource(Res.string.notify),
            )
        }
        DropdownMenu(
            expanded = showNotifyTimes,
            onDismissRequest = {
                showNotifyTimes = false
            },
        ) {
            NotifyTime.entries.forEach { notifyTime ->
                DropdownMenuItem(
                    text = { Text(stringResource(notifyTime.res)) },
                    onClick = {
                        showNotifyTimes = false
                        onNotifyTimePicked(notifyTime.duration)
                    },
                )
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier =
                Modifier
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
    onUploadRequested: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            icon = Icons.Filled.ContentCopy,
            contentDescription = stringResource(Res.string.copy),
            onClick = onCopyRequested,
            modifier =
                Modifier
                    .weight(1f),
        )
        Button(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(Res.string.delete),
            onClick = onDeleteRequested,
            modifier =
                Modifier
                    .weight(1f),
        )
        Button(
            icon = Icons.Filled.CopyAll,
            contentDescription = stringResource(Res.string.duplicate),
            onClick = onDuplicateRequested,
            modifier =
                Modifier
                    .weight(1f),
        )
        Button(
            icon = Icons.Filled.Sync,
            contentDescription = stringResource(Res.string.upload),
            onClick = onUploadRequested,
            modifier =
                Modifier
                    .weight(1f),
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
    onUploadRequested: () -> Unit,
) {
    Box {
        IconButton(
            onClick = onMenuButtonClicked,
            modifier =
                Modifier
                    .size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(Res.string.menu_content_description),
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = {
                onMenuButtonClicked()
            },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.copy)) },
                onClick = {
                    onMenuButtonClicked()
                    onCopyRequested()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.next_day)) },
                onClick = {
                    onMenuButtonClicked()
                    onMoveToNextDayRequested()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.previous_day)) },
                onClick = {
                    onMenuButtonClicked()
                    onMoveToPreviousDayRequested()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete)) },
                onClick = {
                    onMenuButtonClicked()
                    onDeleteRequested()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.upload)) },
                onClick = {
                    onMenuButtonClicked()
                    onUploadRequested()
                },
            )
        }
    }
}
