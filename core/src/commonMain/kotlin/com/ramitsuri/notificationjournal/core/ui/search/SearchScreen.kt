package com.ramitsuri.notificationjournal.core.ui.search

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import com.ramitsuri.notificationjournal.core.ui.fullBorder
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYear
import kotlinx.coroutines.delay
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.search_select_all
import notificationjournal.core.generated.resources.search_tip_empty_text
import notificationjournal.core.generated.resources.search_unselect_all
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: ViewState,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onTagClicked: (String) -> Unit,
    onSelectAllTagsClicked: () -> Unit,
    onUnselectAllTagsClicked: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
            )

            SearchRow(
                searchFieldState = state.searchTextState,
                tags = state.tags,
                onClearClick = onClearClick,
                onTagClicked = onTagClicked,
                onSelectAllTagsClicked = onSelectAllTagsClicked,
                onUnselectAllTagsClicked = onUnselectAllTagsClicked,
            )

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.results) {
                    SearchItem(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(
    searchFieldState: TextFieldState,
    tags: List<ViewState.Tag>,
    onTagClicked: (String) -> Unit,
    onClearClick: () -> Unit,
    onSelectAllTagsClicked: () -> Unit,
    onUnselectAllTagsClicked: () -> Unit,
) {
    val textFieldFocusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current
    var showSearchFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(textFieldFocusRequester) {
        if (showKeyboard && keyboard != null) {
            delay(500)
            textFieldFocusRequester.requestFocus()
            keyboard.show()
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            state = searchFieldState,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            textStyle =
                MaterialTheme.typography.bodyMedium
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 1),
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester = textFieldFocusRequester),
            decorator = @Composable { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = searchFieldState.text.toString(),
                    visualTransformation = VisualTransformation.None,
                    innerTextField = innerTextField,
                    trailingIcon = {
                        if (searchFieldState.text.isNotEmpty()) {
                            IconButton(
                                onClick = onClearClick,
                                modifier =
                                    Modifier
                                        .size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    enabled = true,
                    interactionSource = interactionSource,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                        )
                    },
                )
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (tags.isNotEmpty()) {
            IconButton(
                onClick = { showSearchFilterDialog = true },
                modifier =
                    Modifier
                        .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                )
            }
        }

        SearchFilterDialog(
            showSearchFilterDialog = showSearchFilterDialog,
            tags = tags,
            onDismiss = { showSearchFilterDialog = false },
            onTagClicked = onTagClicked,
            onSelectAllTagsClicked = onSelectAllTagsClicked,
            onUnselectAllTagsClicked = onUnselectAllTagsClicked,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterDialog(
    showSearchFilterDialog: Boolean,
    tags: List<ViewState.Tag>,
    onDismiss: () -> Unit,
    onTagClicked: (String) -> Unit,
    onSelectAllTagsClicked: () -> Unit,
    onUnselectAllTagsClicked: () -> Unit,
) {
    if (showSearchFilterDialog) {
        Dialog(
            onDismissRequest = onDismiss,
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxHeight(0.4f)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                tags.forEach {
                                    FilterChip(
                                        selected = it.selected,
                                        onClick = {
                                            onTagClicked(it.value)
                                        },
                                        label = { Text(text = it.value) },
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = onSelectAllTagsClicked) {
                                Text(
                                    text = stringResource(Res.string.search_select_all),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            TextButton(onClick = onUnselectAllTagsClicked) {
                                Text(
                                    text = stringResource(Res.string.search_unselect_all),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.search_tip_empty_text),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchItem(journalEntry: JournalEntry) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .fullBorder(1.dp, MaterialTheme.colorScheme.outline, 16.dp)
                .padding(16.dp),
    ) {
        val collapsedMaxLines = 2
        var isExpanded by remember { mutableStateOf(false) }
        var clickable by remember { mutableStateOf(false) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        style = MaterialTheme.typography.labelSmall,
                        text =
                            dayMonthDateWithYear(
                                toFormat = journalEntry.entryTime.date,
                            ),
                    )
                    if (!Tag.isNoTag(journalEntry.tag)) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Text(
                            text = journalEntry.tag,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                    style = MaterialTheme.typography.bodyMedium,
                    text = journalEntry.text,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
                    onTextLayout = { textLayoutResult ->
                        if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                            clickable = true
                        }
                    },
                )
            }
            if (clickable) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector =
                            if (isExpanded) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
