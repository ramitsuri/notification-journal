package com.ramitsuri.notificationjournal.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.ui.components.DraggableItem
import com.ramitsuri.notificationjournal.ui.components.dragContainer
import com.ramitsuri.notificationjournal.ui.components.rememberDragDropState
import com.ramitsuri.notificationjournal.ui.tags.TagError
import com.ramitsuri.notificationjournal.ui.tags.TagsViewState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsScreen(
    state: TagsViewState,
    onEditOrder: (Int, Int) -> Unit,
    onAddRequested: (String) -> Unit,
    onEditRequested: (Int, String) -> Unit,
    onDeleteRequested: (Tag) -> Unit,
    onBack: () -> Unit,
    onErrorAcknowledged: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var initialDialogText by rememberSaveable { mutableStateOf("") }
    var tagId by rememberSaveable { mutableIntStateOf(-1) }
    var tagForDelete: Tag? by rememberSaveable { mutableStateOf(null) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        AddEditTagDialog(
            initialText = initialDialogText,
            onPositiveClick = { value ->
                showDialog = !showDialog
                if (tagId == -1) {
                    onAddRequested(value)
                } else {
                    onEditRequested(tagId, value)
                }
            },
            onNegativeClick = {
                showDialog = !showDialog
            }
        )
    }
    if (tagForDelete != null) {
        AlertDialog(
            onDismissRequest = { tagForDelete = null },
            title = {
                Text(text = stringResource(id = R.string.alert))
            },
            text = {
                Text(stringResource(id = R.string.delete_warning_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tagForDelete?.let { forDeletion ->
                            onDeleteRequested(forDeletion)
                        }
                        tagForDelete = null
                    }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tagForDelete = null
                    }) {
                    Text(stringResource(id = R.string.cancel))
                }
            })
    }
    val errorMessage = state.error.string()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorAcknowledged()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = {
                    showDialog = true
                    tagId = -1
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
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
            if (state.tags.isEmpty()) {
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
                        text = "No items",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            } else {
                List(
                    tags = state.tags,
                    onEditOrder = onEditOrder,
                    onEditRequested = { item ->
                        tagId = item.id
                        initialDialogText = item.value
                        showDialog = true
                    },
                    onDeleteRequested = { item ->
                        tagForDelete = item
                    },
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun List(
    modifier: Modifier = Modifier,
    tags: List<Tag>,
    onEditOrder: (Int, Int) -> Unit,
    onEditRequested: (Tag) -> Unit,
    onDeleteRequested: (Tag) -> Unit,
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        onEditOrder(toIndex, fromIndex)
    }
    LazyColumn(
        modifier = modifier.dragContainer(dragDropState),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(tags, key = { _, item -> item.id }) { index, item ->
            DraggableItem(dragDropState, index) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "")
                ListItem(
                    item = item,
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    onEditRequested = onEditRequested,
                    onDeleteRequested = onDeleteRequested,
                )
            }
        }
    }
}

@Composable
private fun ListItem(
    item: Tag,
    elevation: CardElevation,
    onEditRequested: (Tag) -> Unit,
    onDeleteRequested: (Tag) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        elevation = elevation,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )
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
                contentDescription = stringResource(id = R.string.menu_content_description)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onMenuButtonClicked,
        ) {
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
private fun AddEditTagDialog(
    initialText: String,
    onPositiveClick: (String) -> Unit,
    onNegativeClick: () -> Unit,
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
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester = focusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        SolidColor(MaterialTheme.colorScheme.outline)
                                    ),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(8.dp)
                        ) {
                            innerTextField()
                        }
                    })
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        onNegativeClick()
                    }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = {
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
private fun TagError?.string(): String? {
    if (this == null) {
        return null
    }
    val resId = when (this) {
        TagError.DELETE_FAIL -> R.string.tag_delete_fail_message
        TagError.INSERT_FAIL -> R.string.tag_insert_fail_message
    }
    return stringResource(id = resId)
}