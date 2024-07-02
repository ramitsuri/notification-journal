package com.ramitsuri.notificationjournal.core.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.delay
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.back
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.delete
import notificationjournal.core.generated.resources.display_text
import notificationjournal.core.generated.resources.edit
import notificationjournal.core.generated.resources.menu_content_description
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.settings_upload_title
import notificationjournal.core.generated.resources.short_display_text
import notificationjournal.core.generated.resources.template_info
import notificationjournal.core.generated.resources.text
import org.jetbrains.compose.resources.stringResource

@Composable
fun TemplatesScreen(
    state: TemplatesViewState,
    onTextUpdated: (String) -> Unit,
    onDisplayTextUpdated: (String) -> Unit,
    onShortDisplayTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onEditRequested: (JournalEntryTemplate) -> Unit,
    onDeleteRequested: (JournalEntryTemplate) -> Unit,
    onAddRequested: () -> Unit,
    onSyncRequested: () -> Unit,
    onAddOrEditApproved: () -> Unit,
    onAddOrEditCanceled: () -> Unit,
    onBack: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        AddEditTemplateDialog(
            text = state.templateBeingAddedOrEdited.text,
            onTextUpdated = onTextUpdated,
            displayText = state.templateBeingAddedOrEdited.displayText,
            onDisplayTextUpdated = onDisplayTextUpdated,
            shortDisplayText = state.templateBeingAddedOrEdited.shortDisplayText,
            onShortDisplayTextUpdated = onShortDisplayTextUpdated,
            selectedTag = state.templateBeingAddedOrEdited.tag,
            tags = state.tags,
            onTagClicked = onTagClicked,
            canSave = state.canSave,
            onPositiveClick = {
                onAddOrEditApproved()
                showDialog = !showDialog
            },
            onNegativeClick = {
                onAddOrEditCanceled()
                showDialog = !showDialog
            },
        )
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        floatingActionButton = {
            if (state.canAddMore) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 32.dp),
                    onClick = {
                        showDialog = true
                        onAddRequested()
                    }
                ) {
                    Icon(
                        Icons.Filled.Add,
                        stringResource(Res.string.add_entry_content_description)
                    )
                }
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
        ) {
            Spacer(
                modifier = Modifier.height(
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
            )
            TopRow(
                onBack = onBack,
                onSyncClicked = onSyncRequested
            )
            if (state.templates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp)
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
                Spacer(modifier = Modifier.height(24.dp))
                HelperText()
                List(
                    templates = state.templates,
                    onEditRequested = { item ->
                        onEditRequested(item)
                        showDialog = true
                    },
                    onDeleteRequested = onDeleteRequested,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HelperText() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.template_info),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TopRow(
    onBack: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back)
            )
        }
        IconButton(
            onClick = onSyncClicked,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = stringResource(Res.string.settings_upload_title)
            )
        }
    }
}

@Composable
private fun List(
    modifier: Modifier = Modifier,
    templates: List<JournalEntryTemplate>,
    onEditRequested: (JournalEntryTemplate) -> Unit,
    onDeleteRequested: (JournalEntryTemplate) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(templates, key = { _, item -> item.id }) { _, item ->
            ListItem(
                item = item,
                onEditRequested = onEditRequested,
                onDeleteRequested = onDeleteRequested,
            )
        }
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ListItem(
    item: JournalEntryTemplate,
    onEditRequested: (JournalEntryTemplate) -> Unit,
    onDeleteRequested: (JournalEntryTemplate) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = item.displayText.ifEmpty { item.text },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("\u00A0\u00B7\u00A0")
                    Text(
                        text = item.tag,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            ItemMenu(
                showMenu = showMenu,
                onEditRequested = { onEditRequested(item) },
                onDeleteRequested = { onDeleteRequested(item) },
                onMenuButtonClicked = { showMenu = !showMenu },
            )
        }
    }
}

@Composable
private fun ItemMenu(
    showMenu: Boolean,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMenuButtonClicked: () -> Unit
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
                text = { Text(stringResource(Res.string.edit)) },
                onClick = {
                    onMenuButtonClicked()
                    onEditRequested()
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

@OptIn(
    ExperimentalLayoutApi::class,
)
@Composable
private fun AddEditTemplateDialog(
    text: String,
    onTextUpdated: (String) -> Unit,
    selectedTag: String?,
    tags: List<Tag>,
    onTagClicked: (String) -> Unit,
    displayText: String,
    onDisplayTextUpdated: (String) -> Unit,
    shortDisplayText: String,
    onShortDisplayTextUpdated: (String) -> Unit,
    canSave: Boolean,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
) {
    var textSelection by remember { mutableStateOf(TextRange(text.length)) }
    var displayTextSelection by remember { mutableStateOf(TextRange(displayText.length)) }
    var shortDisplayTextSelection by remember { mutableStateOf(TextRange(shortDisplayText.length)) }

    val focusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                LaunchedEffect(focusRequester) {
                    if (showKeyboard) {
                        delay(100)
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                TextField(
                    value = TextFieldValue(
                        text = text,
                        selection = textSelection
                    ),
                    onValueChange = {
                        onTextUpdated(it.text)
                        textSelection = it.selection
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester = focusRequester),
                    label = stringResource(Res.string.text),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = TextFieldValue(
                        text = displayText,
                        selection = displayTextSelection
                    ),
                    onValueChange = {
                        onDisplayTextUpdated(it.text)
                        displayTextSelection = it.selection
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    label = stringResource(Res.string.display_text),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = TextFieldValue(
                        text = shortDisplayText,
                        selection = shortDisplayTextSelection
                    ),
                    onValueChange = {
                        onShortDisplayTextUpdated(it.text)
                        shortDisplayTextSelection = it.selection
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    label = stringResource(Res.string.short_display_text),
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach {
                        FilterChip(
                            selected = it.value == selectedTag,
                            onClick = {
                                onTagClicked(it.value)
                            },
                            label = { Text(text = it.value) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onNegativeClick) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        enabled = canSave,
                        onClick = onPositiveClick
                    ) {
                        Text(text = stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        textStyle = MaterialTheme.typography.bodyMedium
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        maxLines = 1,
        modifier = modifier,
        label = { Text(label) }
    )
}