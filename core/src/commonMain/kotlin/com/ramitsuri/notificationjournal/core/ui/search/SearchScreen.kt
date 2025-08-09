package com.ramitsuri.notificationjournal.core.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.components.Date
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import com.ramitsuri.notificationjournal.core.ui.fullBorder
import com.ramitsuri.notificationjournal.core.utils.dayMonthDate
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYear
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.search_end_date
import notificationjournal.core.generated.resources.search_exact_match
import notificationjournal.core.generated.resources.search_results_count
import notificationjournal.core.generated.resources.search_select_all
import notificationjournal.core.generated.resources.search_start_date
import notificationjournal.core.generated.resources.search_tip_empty_text
import notificationjournal.core.generated.resources.search_unselect_all
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: ViewState,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onTagClicked: (String) -> Unit,
    onSelectAllTagsClicked: () -> Unit,
    onUnselectAllTagsClicked: () -> Unit,
    onNavToViewJournalEntryDay: (JournalEntry) -> Unit,
    onStartDateSelected: (LocalDate?) -> Unit,
    onEndDateSelected: (LocalDate?) -> Unit,
    onExactMatchToggled: (Boolean) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }

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
                onClearClick = onClearClick,
                onToggleFilters = { showFilters = !showFilters },
                hasTags = state.tags.isNotEmpty(),
            )

            AnimatedVisibility(visible = showFilters) {
                FilterControlsRow(
                    startDate = state.startDate,
                    endDate = state.endDate,
                    isExactMatch = state.isExactMatch,
                    sortOrder = state.sortOrder,
                    onStartDateSelected = onStartDateSelected,
                    onEndDateSelected = onEndDateSelected,
                    onExactMatchToggled = onExactMatchToggled,
                    onSortOrderChanged = onSortOrderChanged,
                    tags = state.tags,
                    onTagClicked = onTagClicked,
                    onSelectAllTagsClicked = onSelectAllTagsClicked,
                    onUnselectAllTagsClicked = onUnselectAllTagsClicked,
                )
            }

            if (state.resultCount > 0) {
                Text(
                    text = pluralStringResource(Res.plurals.search_results_count, state.resultCount, state.resultCount),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.results) { journalEntry ->
                    SwipeableSearchItem(
                        journalEntry = journalEntry,
                        onEntrySwiped = { onNavToViewJournalEntryDay(journalEntry) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterControlsRow(
    startDate: LocalDate?,
    endDate: LocalDate?,
    isExactMatch: Boolean,
    sortOrder: SortOrder,
    onStartDateSelected: (LocalDate?) -> Unit,
    onEndDateSelected: (LocalDate?) -> Unit,
    onExactMatchToggled: (Boolean) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    tags: List<ViewState.Tag>,
    onTagClicked: (String) -> Unit,
    onSelectAllTagsClicked: () -> Unit,
    onUnselectAllTagsClicked: () -> Unit,
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showSortOrderMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { showStartDatePicker = true }) {
                Text(startDate?.let { dayMonthDate(it) } ?: stringResource(Res.string.search_start_date))
            }
            TextButton(onClick = { showEndDatePicker = true }) {
                Text(endDate?.let { dayMonthDate(it) } ?: stringResource(Res.string.search_end_date))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.search_exact_match), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isExactMatch,
                    onCheckedChange = onExactMatchToggled,
                )
            }

            Box {
                TextButton(onClick = { showSortOrderMenu = true }) {
                    Text(sortOrder.toString())
                    Icon(
                        imageVector = if (showSortOrderMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Sort Order",
                    )
                }
                DropdownMenu(
                    expanded = showSortOrderMenu,
                    onDismissRequest = { showSortOrderMenu = false },
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.toString()) },
                            onClick = {
                                onSortOrderChanged(order)
                                showSortOrderMenu = false
                            },
                        )
                    }
                }
            }
        }

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxHeight(0.3f) // Limit height for scrollable tags area
                        .verticalScroll(rememberScrollState()),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag.selected,
                            onClick = { onTagClicked(tag.value) },
                            label = { Text(text = tag.value) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.search_tip_empty_text),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    if (showStartDatePicker) {
        Date(
            selectedDate = startDate,
            onDateSelected = { localDate ->
                onStartDateSelected(localDate)
                showStartDatePicker = false
            },
            onResetDate = {
                onStartDateSelected(null)
                showStartDatePicker = false
            },
            onResetDateToToday = null,
            onDismiss = { showStartDatePicker = false },
        )
    }

    if (showEndDatePicker) {
        Date(
            selectedDate = endDate,
            onDateSelected = { localDate ->
                onEndDateSelected(localDate)
                showEndDatePicker = false
            },
            onResetDate = {
                onEndDateSelected(null)
                showEndDatePicker = false
            },
            onResetDateToToday = null,
            onDismiss = { showEndDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(
    searchFieldState: TextFieldState,
    onClearClick: () -> Unit,
    onToggleFilters: () -> Unit,
    hasTags: Boolean,
) {
    val textFieldFocusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                modifier = Modifier.size(48.dp),
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
        IconButton(
            onClick = onToggleFilters,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Toggle Filters",
            )
        }
    }
}

@Composable
private fun SwipeableSearchItem(
    journalEntry: JournalEntry,
    onEntrySwiped: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val swiped by remember {
        derivedStateOf { dismissState.currentValue == SwipeToDismissBoxValue.EndToStart }
    }
    LaunchedEffect(swiped) {
        if (swiped) {
            dismissState.reset()
            onEntrySwiped()
        }
    }
    SwipeToDismissBox(
        modifier = Modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        dayMonthDateWithYear(
                            toFormat = journalEntry.entryTime.date,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            SearchItem(journalEntry = journalEntry)
        }
    }
}

@Composable
private fun SearchItem(journalEntry: JournalEntry) {
    val interactionSource = remember { MutableInteractionSource() }
    var isExpanded by remember { mutableStateOf(false) }
    val clickable =
        remember(journalEntry.text) {
            journalEntry.text.length > 100
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .fullBorder(1.dp, MaterialTheme.colorScheme.outline, 16.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    if (clickable) isExpanded = !isExpanded
                }
                .padding(16.dp),
    ) {
        val collapsedMaxLines = 2

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
                            text = " • ",
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
                    onTextLayout = {
                        // Simplified clickable logic above
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
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }
        }
    }
}
