package com.ramitsuri.notificationjournal.core.ui.tags

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.ui.components.DraggableItem
import com.ramitsuri.notificationjournal.core.ui.components.dragContainer
import com.ramitsuri.notificationjournal.core.ui.components.rememberDragDropState
import kotlinx.coroutines.delay
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.back
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.default_tag
import notificationjournal.core.generated.resources.delete
import notificationjournal.core.generated.resources.edit
import notificationjournal.core.generated.resources.menu_content_description
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.not_default_tag
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.settings_upload_title
import notificationjournal.core.generated.resources.tag_delete_fail_message
import notificationjournal.core.generated.resources.tag_info
import notificationjournal.core.generated.resources.tag_insert_fail_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagsScreen(
    state: TagsViewState,
    onTextUpdated: (String) -> Unit,
    onEditRequested: (Tag) -> Unit,
    onDeleteRequested: (Tag) -> Unit,
    onSetAsDefaultRequested: (Tag?) -> Unit,
    onAddRequested: () -> Unit,
    onAddOrEditApproved: () -> Unit,
    onAddOrEditCanceled: () -> Unit,
    onEditOrder: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onErrorAcknowledged: () -> Unit,
    onSyncRequested: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        AddEditTagDialog(
            text = state.text,
            onTextUpdated = onTextUpdated,
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
    val errorMessage = state.error.string()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorAcknowledged()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 32.dp),
                onClick = {
                    showDialog = true
                    onAddRequested()
                },
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
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    ),
            )
            TopRow(
                onBack = onBack,
                onSyncClicked = onSyncRequested,
            )
            if (state.tags.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp)
                            .padding(bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.no_items),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                HelperText()
                List(
                    tags = state.tags,
                    defaultTag = state.defaultTag,
                    onEditOrder = onEditOrder,
                    onEditRequested = { item ->
                        onEditRequested(item)
                        showDialog = true
                    },
                    onDeleteRequested = onDeleteRequested,
                    onSetAsDefaultRequested = onSetAsDefaultRequested,
                    modifier =
                        Modifier
                            .padding(start = 16.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun TopRow(
    onBack: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onBack,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back),
            )
        }
        IconButton(
            onClick = onSyncClicked,
            modifier =
                Modifier
                    .size(48.dp)
                    .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = stringResource(Res.string.settings_upload_title),
            )
        }
    }
}

@Composable
private fun HelperText() {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.tag_info),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun List(
    modifier: Modifier = Modifier,
    tags: List<Tag>,
    defaultTag: String,
    onEditOrder: (Int, Int) -> Unit,
    onEditRequested: (Tag) -> Unit,
    onDeleteRequested: (Tag) -> Unit,
    onSetAsDefaultRequested: (Tag?) -> Unit,
) {
    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(listState) { fromIndex, toIndex ->
            onEditOrder(toIndex, fromIndex)
        }
    LazyColumn(
        modifier = modifier.dragContainer(dragDropState),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(tags, key = { _, item -> item.id }) { index, item ->
            DraggableItem(dragDropState, index) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "")
                ListItem(
                    item = item,
                    isDefault = item.value == defaultTag,
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    onEditRequested = onEditRequested,
                    onDeleteRequested = onDeleteRequested,
                    onSetAsDefaultRequested = onSetAsDefaultRequested,
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ListItem(
    item: Tag,
    isDefault: Boolean,
    elevation: CardElevation,
    onEditRequested: (Tag) -> Unit,
    onDeleteRequested: (Tag) -> Unit,
    onSetAsDefaultRequested: (Tag?) -> Unit,
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
            modifier =
                Modifier
                    .padding(8.dp),
        ) {
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(8.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isDefault) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = stringResource(Res.string.default_tag),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            ItemMenu(
                showMenu = showMenu,
                isDefault = isDefault,
                onEditRequested = { onEditRequested(item) },
                onDeleteRequested = { onDeleteRequested(item) },
                onMenuButtonClicked = { showMenu = !showMenu },
                onSetAsDefaultClicked = { onSetAsDefaultRequested(item) },
                onUnsetAsDefaultClicked = { onSetAsDefaultRequested(null) },
            )
        }
    }
}

@Composable
private fun ItemMenu(
    showMenu: Boolean,
    isDefault: Boolean,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onMenuButtonClicked: () -> Unit,
    onSetAsDefaultClicked: () -> Unit,
    onUnsetAsDefaultClicked: () -> Unit,
) {
    Box {
        IconButton(
            onClick = onMenuButtonClicked,
            modifier =
                Modifier
                    .size(48.dp)
                    .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(Res.string.menu_content_description),
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
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete)) },
                onClick = {
                    onMenuButtonClicked()
                    onDeleteRequested()
                },
            )
            if (isDefault) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.not_default_tag)) },
                    onClick = {
                        onMenuButtonClicked()
                        onUnsetAsDefaultClicked()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.default_tag)) },
                    onClick = {
                        onMenuButtonClicked()
                        onSetAsDefaultClicked()
                    },
                )
            }
        }
    }
}

@Composable
private fun AddEditTagDialog(
    text: String,
    onTextUpdated: (String) -> Unit,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
) {
    var selection by remember { mutableStateOf(TextRange(text.length)) }

    val focusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(
        onDismissRequest = { },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Card {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
            ) {
                LaunchedEffect(focusRequester) {
                    if (showKeyboard) {
                        delay(100)
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                }
                BasicTextField(
                    value = TextFieldValue(text = text, selection = selection),
                    onValueChange = {
                        onTextUpdated(it.text)
                        selection = it.selection
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    textStyle =
                        MaterialTheme.typography.bodyMedium
                            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester = focusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            SolidColor(MaterialTheme.colorScheme.outline),
                                        ),
                                        RoundedCornerShape(5.dp),
                                    )
                                    .padding(8.dp),
                        ) {
                            innerTextField()
                        }
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = {
                        onNegativeClick()
                    }) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onPositiveClick) {
                        Text(text = stringResource(Res.string.ok))
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
    val resId =
        when (this) {
            TagError.DELETE_FAIL -> Res.string.tag_delete_fail_message
            TagError.INSERT_FAIL -> Res.string.tag_insert_fail_message
        }
    return stringResource(resId)
}
