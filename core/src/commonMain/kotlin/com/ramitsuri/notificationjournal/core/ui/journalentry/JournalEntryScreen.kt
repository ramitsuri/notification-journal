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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.bottomBorder
import com.ramitsuri.notificationjournal.core.ui.sideBorder
import com.ramitsuri.notificationjournal.core.ui.topBorder
import com.ramitsuri.notificationjournal.core.utils.getDay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.copy
import notificationjournal.core.generated.resources.copy_reconcile
import notificationjournal.core.generated.resources.delete
import notificationjournal.core.generated.resources.delete_warning_message
import notificationjournal.core.generated.resources.edit
import notificationjournal.core.generated.resources.menu_content_description
import notificationjournal.core.generated.resources.move_down
import notificationjournal.core.generated.resources.move_up
import notificationjournal.core.generated.resources.next_day
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.pm
import notificationjournal.core.generated.resources.previous_day
import notificationjournal.core.generated.resources.reconcile
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.settings_upload_title
import notificationjournal.core.generated.resources.untagged
import notificationjournal.core.generated.resources.untagged_format
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
    onMoveUpRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveDownRequested: (JournalEntry, TagGroup) -> Unit,
    onTagGroupMoveToNextDayRequested: (TagGroup) -> Unit,
    onTagGroupMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onTagGroupDeleteRequested: (TagGroup) -> Unit,
    onTagGroupReconcileRequested: (TagGroup) -> Unit,
    onSettingsClicked: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

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
                        onMoveDownRequested = onMoveDownRequested,
                        onEditRequested = { item ->
                            onEditRequested(item.id)
                        },
                        onDeleteRequested = { item ->
                            journalEntryForDelete = item
                        },
                        onTagGroupMoveToNextDayRequested = onTagGroupMoveToNextDayRequested,
                        onTagGroupMoveToPreviousDayRequested = onTagGroupMoveToPreviousDayRequested,
                        onTagGroupReconcileRequested = onTagGroupReconcileRequested,
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
    tags: List<Tag>,
    onCopyRequested: (JournalEntry) -> Unit,
    onTagClicked: (JournalEntry, String) -> Unit,
    onTagGroupCopyRequested: (TagGroup) -> Unit,
    onTagGroupDeleteRequested: (TagGroup) -> Unit,
    onTagGroupMoveToNextDayRequested: (TagGroup) -> Unit,
    onTagGroupMoveToPreviousDayRequested: (TagGroup) -> Unit,
    onTagGroupReconcileRequested: (TagGroup) -> Unit,
    onMoveUpRequested: (JournalEntry, TagGroup) -> Unit,
    onMoveDownRequested: (JournalEntry, TagGroup) -> Unit,
    onEditRequested: (JournalEntry) -> Unit,
    onDeleteRequested: (JournalEntry) -> Unit,
    onMoveToNextDayRequested: (JournalEntry) -> Unit,
    onMoveToPreviousDayRequested: (JournalEntry) -> Unit,
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
                        onCopyRequested = { onCopyRequested(entry) },
                        onDeleteRequested = { onDeleteRequested(entry) },
                        onEditRequested = { onEditRequested(entry) },
                        onMoveUpRequested = {
                            onMoveUpRequested(entry, tagGroup)
                        },
                        onMoveDownRequested = {
                            onMoveDownRequested(entry, tagGroup)
                        },
                        tags = tags,
                        selectedTag = entry.tag,
                        onTagClicked = { onTagClicked(entry, it) },
                        onMoveToNextDayRequested = { onMoveToNextDayRequested(entry) },
                        onMoveToPreviousDayRequested = { onMoveToPreviousDayRequested(entry) },
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
            onReconcileRequested = { onReconcileRequested(tagGroup) }
        )
    }
}

@Composable
private fun ListItem(
    item: JournalEntry,
    onCopyRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMoveUpRequested: () -> Unit,
    onMoveDownRequested: () -> Unit,
    tags: List<Tag>,
    selectedTag: String?,
    onTagClicked: (String) -> Unit,
    onMoveToNextDayRequested: () -> Unit,
    onMoveToPreviousDayRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }

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
    }

    DetailsDialog(
        showDetails = showDetails,
        tags = tags,
        selectedTag = selectedTag,
        time = item.formattedTime(
            am = stringResource(Res.string.am),
            pm = stringResource(Res.string.pm)
        ),
        onCopyRequested = onCopyRequested,
        onEditRequested = onEditRequested,
        onDeleteRequested = onDeleteRequested,
        onMoveUpRequested = onMoveUpRequested,
        onMoveDownRequested = onMoveDownRequested,
        onMoveToNextDayRequested = onMoveToNextDayRequested,
        onMoveToPreviousDayRequested = onMoveToPreviousDayRequested,
        onTagClicked = { tag -> onTagClicked(tag) },
        onDismiss = { showDetails = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsDialog(
    showDetails: Boolean,
    tags: List<Tag>,
    selectedTag: String?,
    time: String,
    onCopyRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMoveUpRequested: () -> Unit,
    onMoveDownRequested: () -> Unit,
    onMoveToNextDayRequested: () -> Unit,
    onMoveToPreviousDayRequested: () -> Unit,
    onTagClicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
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
                ) {
                    Text(time)
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(24.dp))
                    TimeModifiers(
                        onMoveToNextDayRequested = {
                            onMoveToNextDayRequested()
                            onDismiss()
                        },
                        onMoveToPreviousDayRequested = {
                            onMoveToPreviousDayRequested()
                            onDismiss()
                        },
                        onMoveDownRequested = {
                            onMoveDownRequested()
                            onDismiss()
                        },
                        onMoveUpRequested = {
                            onMoveUpRequested()
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ButtonRow(
                        onCopyRequested = {
                            onCopyRequested()
                            onDismiss()
                        },
                        onEditRequested = {
                            onEditRequested()
                            onDismiss()
                        },
                        onDeleteRequested = {
                            onDeleteRequested()
                            onDismiss()
                        })
                }
            }
        }
    }
}

@Composable
private fun TimeModifiers(
    modifier: Modifier = Modifier,
    onMoveToNextDayRequested: () -> Unit,
    onMoveToPreviousDayRequested: () -> Unit,
    onMoveUpRequested: () -> Unit,
    onMoveDownRequested: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TimeModifierActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ArrowDownward,
                text = stringResource(Res.string.move_down),
                onClick = onMoveDownRequested,
            )
            TimeModifierActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ArrowUpward,
                text = stringResource(Res.string.move_up),
                onClick = onMoveUpRequested,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TimeModifierActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                text = stringResource(Res.string.previous_day),
                onClick = onMoveToPreviousDayRequested,
            )
            TimeModifierActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                text = stringResource(Res.string.next_day),
                onClick = onMoveToNextDayRequested,
            )
        }
    }
}

@Composable
private fun TimeModifierActionButton(
    icon: ImageVector,
    text: String,
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
                .padding(4.dp),
            imageVector = icon,
            contentDescription = text
        )
        Text(text)
    }
}

@Composable
private fun ButtonRow(
    onCopyRequested: () -> Unit,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ActionButton(
            icon = Icons.Filled.ContentCopy,
            contentDescription = stringResource(Res.string.copy),
            onClick = onCopyRequested,
            modifier = Modifier
                .weight(1f)
        )
        ActionButton(
            icon = Icons.Filled.Edit,
            contentDescription = stringResource(Res.string.edit),
            onClick = onEditRequested,
            modifier = Modifier
                .weight(1f)
        )
        ActionButton(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(Res.string.delete),
            onClick = onDeleteRequested,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
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
) {
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
            onDismissRequest = onMenuButtonClicked,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.copy_reconcile)) },
                onClick = {
                    onMenuButtonClicked()
                    onCopyRequested()
                    onReconcileRequested()
                }
            )
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
        )
    }
}
