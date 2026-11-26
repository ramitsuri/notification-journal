package com.ramitsuri.notificationjournal.core.ui.reconcilechecks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.model.ReconcileCheck
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import kotlinx.datetime.DayOfWeek
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.day_of_week_short_names
import notificationjournal.core.generated.resources.delete
import notificationjournal.core.generated.resources.edit
import notificationjournal.core.generated.resources.menu_content_description
import notificationjournal.core.generated.resources.no_items
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.reconcile_check_add_title
import notificationjournal.core.generated.resources.reconcile_check_days_label
import notificationjournal.core.generated.resources.reconcile_check_edit_title
import notificationjournal.core.generated.resources.reconcile_check_match_any_entry
import notificationjournal.core.generated.resources.reconcile_check_match_contains_all_words
import notificationjournal.core.generated.resources.reconcile_check_match_starts_with
import notificationjournal.core.generated.resources.reconcile_check_match_type_label
import notificationjournal.core.generated.resources.reconcile_check_missing_tag
import notificationjournal.core.generated.resources.reconcile_check_no_tags_message
import notificationjournal.core.generated.resources.reconcile_check_query_phrase_label
import notificationjournal.core.generated.resources.reconcile_check_query_words_label
import notificationjournal.core.generated.resources.reconcile_check_select_at_least_one_day
import notificationjournal.core.generated.resources.reconcile_check_tag_label
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReconcileChecksScreen(
    state: ReconcileChecksViewState,
    onAddRequested: () -> Unit,
    onEditRequested: (ReconcileCheck) -> Unit,
    onDeleteRequested: (ReconcileCheck) -> Unit,
    onEnabledChanged: (ReconcileCheck, Boolean) -> Unit,
    onTagSelected: (String) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onMatchTypeSelected: (ReconcileCheckMatchType) -> Unit,
    onQueryUpdated: (String) -> Unit,
    onAddOrEditApproved: () -> Unit,
    onAddOrEditCanceled: () -> Unit,
    onBack: () -> Unit,
) {
    val editorState = state.editorState
    if (editorState != null) {
        AddEditReconcileCheckDialog(
            editorState = editorState,
            availableTags = state.availableTags,
            canSave = state.canSave,
            onTagSelected = onTagSelected,
            onDayToggled = onDayToggled,
            onMatchTypeSelected = onMatchTypeSelected,
            onQueryUpdated = onQueryUpdated,
            onPositiveClick = onAddOrEditApproved,
            onNegativeClick = onAddOrEditCanceled,
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                onClick = onAddRequested,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.add_entry_content_description),
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
            Toolbar(onBackClick = onBack)
            if (state.checks.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
                ReconcileChecksList(
                    checks = state.checks,
                    onEditRequested = onEditRequested,
                    onDeleteRequested = onDeleteRequested,
                    onEnabledChanged = onEnabledChanged,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ReconcileChecksList(
    checks: List<ReconcileCheckItemState>,
    onEditRequested: (ReconcileCheck) -> Unit,
    onDeleteRequested: (ReconcileCheck) -> Unit,
    onEnabledChanged: (ReconcileCheck, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(checks, key = { it.check.id }) { item ->
            ReconcileCheckListItem(
                item = item,
                onEditRequested = { onEditRequested(item.check) },
                onDeleteRequested = { onDeleteRequested(item.check) },
                onEnabledChanged = { enabled -> onEnabledChanged(item.check, enabled) },
            )
        }
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ReconcileCheckListItem(
    item: ReconcileCheckItemState,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        onClick = onEditRequested,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (item.isMissingTag) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.reconcile_check_missing_tag, item.check.tag),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = item.check.enabled,
                onCheckedChange = onEnabledChanged,
            )
            ItemMenu(
                showMenu = showMenu,
                onEditRequested = onEditRequested,
                onDeleteRequested = onDeleteRequested,
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
    onMenuButtonClicked: () -> Unit,
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
        }
    }
}

@Composable
private fun AddEditReconcileCheckDialog(
    editorState: ReconcileCheckEditorState,
    availableTags: List<Tag>,
    canSave: Boolean,
    onTagSelected: (String) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onMatchTypeSelected: (ReconcileCheckMatchType) -> Unit,
    onQueryUpdated: (String) -> Unit,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
) {
    val dayShortNames = stringArrayResource(Res.array.day_of_week_short_names)
    Dialog(
        onDismissRequest = onNegativeClick,
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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            if (editorState.isEditing) {
                                Res.string.reconcile_check_edit_title
                            } else {
                                Res.string.reconcile_check_add_title
                            },
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.reconcile_check_tag_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (availableTags.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.reconcile_check_no_tags_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableTags.forEach { tag ->
                            FilterChip(
                                selected = tag.value == editorState.selectedTag,
                                onClick = { onTagSelected(tag.value) },
                                label = { Text(text = tag.value) },
                            )
                        }
                    }
                    val selectedTag = editorState.selectedTag
                    if (selectedTag != null && availableTags.none { it.value == selectedTag }) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.reconcile_check_missing_tag, selectedTag),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.reconcile_check_days_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in editorState.daysOfWeek,
                            onClick = { onDayToggled(day) },
                            label = { Text(text = dayShortNames.getOrElse(day.ordinal) { day.name }) },
                        )
                    }
                }
                if (editorState.daysOfWeek.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.reconcile_check_select_at_least_one_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.reconcile_check_match_type_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReconcileCheckMatchType.entries.forEach { matchType ->
                        FilterChip(
                            selected = editorState.matchType == matchType,
                            onClick = { onMatchTypeSelected(matchType) },
                            label = { Text(text = stringResource(matchType.labelRes())) },
                        )
                    }
                }

                if (editorState.matchType != ReconcileCheckMatchType.ANY_ENTRY) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val queryLabel =
                        when (editorState.matchType) {
                            ReconcileCheckMatchType.STARTS_WITH ->
                                stringResource(Res.string.reconcile_check_query_phrase_label)

                            ReconcileCheckMatchType.CONTAINS_ALL_WORDS ->
                                stringResource(Res.string.reconcile_check_query_words_label)

                            ReconcileCheckMatchType.ANY_ENTRY -> ""
                        }
                    OutlinedTextField(
                        value = editorState.query,
                        onValueChange = onQueryUpdated,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                        textStyle =
                            MaterialTheme.typography.bodyMedium
                                .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(queryLabel) },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onNegativeClick) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        enabled = canSave,
                        onClick = onPositiveClick,
                    ) {
                        Text(text = stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

private fun ReconcileCheckMatchType.labelRes() =
    when (this) {
        ReconcileCheckMatchType.ANY_ENTRY -> Res.string.reconcile_check_match_any_entry
        ReconcileCheckMatchType.STARTS_WITH -> Res.string.reconcile_check_match_starts_with
        ReconcileCheckMatchType.CONTAINS_ALL_WORDS -> Res.string.reconcile_check_match_contains_all_words
    }
