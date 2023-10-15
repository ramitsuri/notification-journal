package com.ramitsuri.notificationjournal.ui.journalentry

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.JournalMenuItem
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.getDay
import com.ramitsuri.notificationjournal.ui.bottomBorder
import com.ramitsuri.notificationjournal.ui.sideBorder
import com.ramitsuri.notificationjournal.ui.string
import com.ramitsuri.notificationjournal.ui.topBorder
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalEntryScreen(
    state: ViewState,
    onAddRequested: (String) -> Unit,
    onEditRequested: (Int, String) -> Unit,
    onDeleteRequested: (JournalEntry) -> Unit,
    resetReceivedText: () -> Unit,
    onSettingsClicked: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(state.receivedText != null) }
    var initialDialogText by rememberSaveable { mutableStateOf(state.receivedText ?: "") }
    var journalEntryId by rememberSaveable { mutableIntStateOf(-1) }
    var journalEntryForDelete: JournalEntry? by rememberSaveable { mutableStateOf(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    if (showDialog) {
        AddEditEntryDialog(
            initialText = initialDialogText,
            suggestedText = state.suggestedTextFromReceivedText,
            onPositiveClick = { value ->
                showDialog = !showDialog
                if (journalEntryId == -1) {
                    onAddRequested(value)
                } else {
                    onEditRequested(journalEntryId, value)
                }
            },
            onNegativeClick = {
                showDialog = !showDialog
            },
            resetReceivedText = resetReceivedText
        )
    }

    if (journalEntryForDelete != null) {
        AlertDialog(
            onDismissRequest = { journalEntryForDelete = null },
            title = {
                Text(text = stringResource(id = R.string.alert))
            },
            text = {
                Text(stringResource(id = R.string.delete_warning_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        journalEntryForDelete?.let { forDeletion ->
                            onDeleteRequested(forDeletion)
                        }
                        journalEntryForDelete = null
                    }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        journalEntryForDelete = null
                    }) {
                    Text(stringResource(id = R.string.cancel))
                }
            })
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = {
                    showDialog = true
                    journalEntryId = -1
                    initialDialogText = ""
                }
            ) {
                Icon(
                    Icons.Filled.Add,
                    stringResource(id = R.string.add_entry_content_description)
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

                MoreMenu(onSettingsClicked = onSettingsClicked)

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
                            text = "No items",
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                } else {
                    List(state.dayGroups,
                        onCopyRequested = { item ->
                            clipboardManager.setText(AnnotatedString(item.text))
                        },
                        onEditRequested = { item ->
                            journalEntryId = item.id
                            initialDialogText = item.text
                            showDialog = true
                        },
                        onDeleteRequested = { item ->
                            journalEntryForDelete = item
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun MoreMenu(
    onSettingsClicked: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box {
            IconButton(
                onClick = {
                    expanded = !expanded
                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.menu_content_description)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {

                DropdownMenuItem(
                    text = { Text(stringResource(id = JournalMenuItem.SETTINGS.textResId)) },
                    onClick = {
                        expanded = false
                        onSettingsClicked()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun List(
    items: List<DayGroup>,
    onCopyRequested: (JournalEntry) -> Unit,
    onEditRequested: (JournalEntry) -> Unit,
    onDeleteRequested: (JournalEntry) -> Unit
) {
    val strokeWidth: Dp = 1.dp
    val strokeColor: Color = MaterialTheme.colorScheme.outline
    val cornerRadius: Dp = 16.dp
    val topShape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    val bottomShape = RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)

    LazyColumn {
        items.forEach { (date, tagGroups) ->
            stickyHeader {
                HeaderItem(text = getDay(date).string())
            }
            tagGroups.forEach { (tag, entries) ->
                var shape: Shape
                var borderModifier: Modifier
                item {
                    SubHeaderItem(
                        text = tag ?: "Untagged",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(topShape)
                            .background(color = MaterialTheme.colorScheme.background)
                            .then(Modifier.topBorder(strokeWidth, strokeColor, cornerRadius))
                            .padding(16.dp)
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
                    ListItem(
                        item = entries[index],
                        onCopyRequested = onCopyRequested,
                        onDeleteRequested = onDeleteRequested,
                        onEditRequested = onEditRequested,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .then(borderModifier)
                            .padding(8.dp)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun HeaderItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SubHeaderItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ListItem(
    item: JournalEntry,
    onCopyRequested: (JournalEntry) -> Unit,
    onEditRequested: (JournalEntry) -> Unit,
    onDeleteRequested: (JournalEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        ItemMenu(
            showMenu = showMenu,
            onCopyRequested = { onCopyRequested(item) },
            onEditRequested = { onEditRequested(item) },
            onDeleteRequested = { onDeleteRequested(item) },
            onMenuButtonClicked = { showMenu = !showMenu },
        )
    }
}

@Composable
private fun ItemMenu(
    showMenu: Boolean,
    onCopyRequested: () -> Unit,
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
                contentDescription = stringResource(id = R.string.menu_content_description)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onMenuButtonClicked,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.copy)) },
                onClick = {
                    onMenuButtonClicked()
                    onCopyRequested()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.edit)) },
                onClick = {
                    onMenuButtonClicked()
                    onEditRequested()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.delete)) },
                onClick = {
                    onMenuButtonClicked()
                    onDeleteRequested()
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AddEditEntryDialog(
    initialText: String,
    suggestedText: String?,
    onPositiveClick: (String) -> Unit,
    onNegativeClick: () -> Unit,
    resetReceivedText: () -> Unit
) {
    var text by remember {
        mutableStateOf(
            TextFieldValue(
                initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = { }) {
        Card(modifier = Modifier.height(320.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                LaunchedEffect(focusRequester) {
                    if (showKeyboard) {
                        delay(100)
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester = focusRequester)
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!suggestedText.isNullOrEmpty()) {
                    SuggestedText(suggestedText, onUseSuggestedText = {
                        resetReceivedText()
                        text = TextFieldValue(it, selection = text.selection)
                    })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        resetReceivedText()
                        onNegativeClick()
                    }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = {
                        resetReceivedText()
                        onPositiveClick(text.text)
                    }) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedText(
    suggestedText: String,
    onUseSuggestedText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = suggestedText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = {
            onUseSuggestedText(suggestedText)
        }) {
            Text(text = stringResource(id = R.string.use))
        }
    }
}

@Preview
@Composable
fun ListItemPreview() {
    Surface {
        ListItem(
            item = JournalEntry(
                id = 0,
                entryTime = Instant.now(),
                timeZone = ZoneId.systemDefault(),
                text = "Test text"
            ),
            onCopyRequested = {},
            onEditRequested = {},
            onDeleteRequested = {}
        )
    }
}
