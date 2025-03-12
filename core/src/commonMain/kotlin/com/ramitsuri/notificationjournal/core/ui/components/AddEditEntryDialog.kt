@file:OptIn(ExperimentalFoundationApi::class)

package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.dayMonthDate
import com.ramitsuri.notificationjournal.core.utils.hourMinute
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_save
import notificationjournal.core.generated.resources.add_entry_save_and_add_another
import notificationjournal.core.generated.resources.add_from_template
import notificationjournal.core.generated.resources.alert
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.now
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.pm
import notificationjournal.core.generated.resources.reset
import notificationjournal.core.generated.resources.tags
import notificationjournal.core.generated.resources.unsaved_warning_message
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryDialog(
    isLoading: Boolean,
    textState: TextFieldState,
    tags: List<Tag>,
    selectedTag: String?,
    showAddAnother: Boolean,
    dateTime: LocalDateTime,
    templates: List<JournalEntryTemplate>,
    textCorrections: Map<String, List<String>>,
    showWarningOnExit: Boolean,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionClicked: (String?) -> Unit,
    onSuggestionsEnabledChanged: () -> Unit,
    onTagClicked: (String) -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onResetDate: () -> Unit,
    onResetDateToToday: (() -> Unit)?,
    onResetTime: () -> Unit,
    onResetTimeToNow: (() -> Unit)?,
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
) {
    var showTemplatesKeyboardShortcutHints by remember { mutableStateOf(false) }
    var showTagsKeyboardShortcutHints by remember { mutableStateOf(false) }
    var showDateKeyboardShortcutHints by remember { mutableStateOf(false) }
    var showCancelWarningDialog by remember { mutableStateOf(false) }
    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            rememberTopAppBarState(),
        )
    val onCancelWithWarning = {
        if (showWarningOnExit) {
            showCancelWarningDialog = true
        } else {
            onCancel()
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .navigationBarsPadding()
                .focusable().onKeyEvent {
                    showTemplatesKeyboardShortcutHints = it.isMetaPressed && it.isAltPressed
                    showTagsKeyboardShortcutHints = it.isMetaPressed && it.isAltPressed.not()
                    showDateKeyboardShortcutHints = it.isMetaPressed

                    when {
                        // Meta + Shift + S -> AddAnother
                        it.isMetaPressed &&
                            it.isShiftPressed &&
                            it.key == Key.S &&
                            it.type == KeyEventType.KeyUp -> {
                            onAddAnother()
                            true
                        }

                        // Meta + S -> Save
                        it.isMetaPressed &&
                            it.key == Key.S &&
                            it.type == KeyEventType.KeyUp -> {
                            onSave()
                            true
                        }

                        // Meta + (Left or J) -> Previous day
                        it.isMetaPressed &&
                            (it.key == Key.DirectionLeft || it.key == Key.J) &&
                            it.type == KeyEventType.KeyDown -> {
                            onPreviousDateRequested()
                            true
                        }

                        // Meta + (Right or K) -> Next day
                        it.isMetaPressed &&
                            (it.key == Key.DirectionRight || it.key == Key.K) &&
                            it.type == KeyEventType.KeyDown -> {
                            onNextDateRequested()
                            true
                        }

                        // Escape -> Cancel
                        it.key == Key.Escape &&
                            it.type == KeyEventType.KeyUp -> {
                            onCancelWithWarning()
                            true
                        }

                        it.key == Key.One &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 0
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Two &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 1
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Three &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 2
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Four &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 3
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Five &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 4
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Six &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 5
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Seven &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 6
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Eight &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 7
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Nine &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 8
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Zero &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 9
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.Y &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 10
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.U &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 11
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.I &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 12
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.O &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 13
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.P &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed.not() &&
                            it.isMetaPressed -> {
                            val index = 14
                            val tag = tags.getOrNull(index) ?: tags.lastOrNull()
                            if (tag != null) {
                                onTagClicked(tag.value)
                            }
                            true
                        }

                        it.key == Key.One &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 0
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Two &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 1
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Three &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 2
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Four &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 3
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Five &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 4
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Six &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 5
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Seven &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 6
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Eight &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 7
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Nine &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 8
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        it.key == Key.Zero &&
                            it.type == KeyEventType.KeyUp &&
                            it.isAltPressed &&
                            it.isMetaPressed -> {
                            val index = 9
                            val template = templates.getOrNull(index) ?: templates.lastOrNull()
                            if (template != null) {
                                onTemplateClicked(template)
                            }
                            true
                        }

                        else -> {
                            false
                        }
                    }
                },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Toolbar(onBackClick = onCancelWithWarning, scrollBehavior = scrollBehavior)
            if (isLoading) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                )
            } else {
                DateTimeEntry(
                    dateTime = dateTime,
                    onDateSelected = onDateSelected,
                    onTimeSelected = onTimeSelected,
                    onResetDate = onResetDate,
                    onResetTime = onResetTime,
                    onResetDateToToday = onResetDateToToday,
                    onResetTimeToNow = onResetTimeToNow,
                )
                Content(
                    modifier =
                        Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .weight(1f),
                    textState = textState,
                    tags = tags,
                    selectedTag = selectedTag,
                    templates = templates,
                    textCorrections = textCorrections,
                    showTagsKeyboardShortcutHint = showTagsKeyboardShortcutHints,
                    showTemplatesKeyboardShortcutHint = showTemplatesKeyboardShortcutHints,
                    suggestions = suggestions,
                    showSuggestions = showSuggestions,
                    onSuggestionsEnabledChanged = onSuggestionsEnabledChanged,
                    onSuggestionClicked = onSuggestionClicked,
                    onTagClicked = onTagClicked,
                    onTemplateClicked = onTemplateClicked,
                    onCorrectionAccepted = onCorrectionAccepted,
                    onAddDictionaryWord = onAddDictionaryWord,
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (showAddAnother) {
                        OutlinedButton(onClick = onAddAnother, modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(Res.string.add_entry_save_and_add_another))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(Res.string.add_entry_save))
                    }
                }
            }
        }

        if (showCancelWarningDialog) {
            AlertDialog(
                onDismissRequest = { showCancelWarningDialog = false },
                title = {
                    Text(text = stringResource(Res.string.alert))
                },
                text = {
                    Text(stringResource(Res.string.unsaved_warning_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelWarningDialog = false
                            onCancel()
                        },
                    ) {
                        Text(stringResource(Res.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCancelWarningDialog = false
                        },
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    textState: TextFieldState,
    tags: List<Tag>,
    selectedTag: String?,
    textCorrections: Map<String, List<String>>,
    templates: List<JournalEntryTemplate>,
    showTagsKeyboardShortcutHint: Boolean,
    showTemplatesKeyboardShortcutHint: Boolean,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionsEnabledChanged: () -> Unit,
    onSuggestionClicked: (String?) -> Unit,
    onTagClicked: (String) -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        LaunchedEffect(Unit) {
            if (keyboard != null) {
                textFieldFocusRequester.requestFocus()
                keyboard.show()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            textState = textState,
            textFieldFocusRequester = textFieldFocusRequester,
            textCorrections = textCorrections,
            suggestions = suggestions,
            showSuggestions = showSuggestions,
            onSuggestionsEnabledChanged = onSuggestionsEnabledChanged,
            onSuggestionClicked = onSuggestionClicked,
            onCorrectionAccepted = onCorrectionAccepted,
            onAddDictionaryWord = onAddDictionaryWord,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (templates.isNotEmpty()) {
            Templates(
                templates = templates,
                showKeyboardShortcutHint = showTemplatesKeyboardShortcutHint,
                onTemplateClicked = {
                    onTemplateClicked(it)
                    textFieldFocusRequester.requestFocus()
                },
                modifier =
                    Modifier.onKeyEvent {
                        when {
                            // Shift + Tab -> Focus up
                            it.key == Key.Tab &&
                                it.type == KeyEventType.KeyDown &&
                                it.isShiftPressed -> {
                                focusManager.moveFocus(FocusDirection.Up)
                                true
                            }

                            // Tab -> Focus down
                            it.key == Key.Tab &&
                                it.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            }

                            // Left -> Focus previous
                            it.key == Key.DirectionLeft &&
                                it.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Previous)
                                true
                            }

                            // Right -> Focus next
                            it.key == Key.DirectionRight &&
                                it.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (tags.isNotEmpty()) {
            Tags(
                tags = tags,
                selectedTag = selectedTag,
                onTagClicked = onTagClicked,
                showKeyboardShortcutHint = showTagsKeyboardShortcutHint,
                modifier =
                    Modifier.onKeyEvent {
                        when {
                            // Shift + Tab -> Focus up
                            it.key == Key.Tab &&
                                it.type == KeyEventType.KeyDown &&
                                it.isShiftPressed -> {
                                focusManager.moveFocus(FocusDirection.Up)
                                true
                            }

                            // Tab -> Focus down
                            it.key == Key.Tab &&
                                it.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            }

                            // Left -> Focus previous
                            it.key == Key.DirectionLeft &&
                                it.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Previous)
                                true
                            }

                            // Right -> Focus next
                            it.key == Key.DirectionRight &&
                                it.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextField(
    textState: TextFieldState,
    textFieldFocusRequester: FocusRequester,
    textCorrections: Map<String, List<String>>,
    suggestions: List<String>,
    showSuggestions: Boolean,
    onSuggestionsEnabledChanged: () -> Unit,
    onSuggestionClicked: (String?) -> Unit,
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
) {
    var showTextCorrectionsDialog by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = showSuggestions && suggestions.isNotEmpty(),
        onExpandedChange = { },
        modifier = Modifier.focusable(false),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    state = textState,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    textStyle =
                        MaterialTheme.typography.bodyMedium
                            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 10),
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequester = textFieldFocusRequester),
                    decorator = { innerTextField ->
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
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = { showTextCorrectionsDialog = true },
                    enabled = textCorrections.isNotEmpty(),
                ) {
                    Text(text = textCorrections.size.toString())
                }
            }
            Row(
                modifier =
                    Modifier
                        .clickable(role = Role.Checkbox, onClick = {}),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Show suggestions",
                    style = MaterialTheme.typography.bodySmall,
                )
                Checkbox(showSuggestions, { onSuggestionsEnabledChanged() })
            }
        }
        ExposedDropdownMenu(
            expanded = showSuggestions && suggestions.isNotEmpty(),
            onDismissRequest = { onSuggestionClicked(null) },
            modifier = Modifier.focusable(false),
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = { onSuggestionClicked(suggestion) },
                    contentPadding = PaddingValues(8.dp),
                )
            }
        }
    }

    if (showTextCorrectionsDialog) {
        TextCorrectionsDialog(
            textCorrections = textCorrections,
            onDismiss = { showTextCorrectionsDialog = false },
            onCorrectionAccepted = { word, correction ->
                if (textCorrections.size == 1) {
                    showTextCorrectionsDialog = false
                }
                onCorrectionAccepted(word, correction)
            },
            onAddDictionaryWord = onAddDictionaryWord,
        )
    }
}

@Composable
private fun TextCorrectionsDialog(
    textCorrections: Map<String, List<String>>,
    onDismiss: () -> Unit,
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
) {
    LaunchedEffect(textCorrections) {
        if (textCorrections.isEmpty()) {
            onDismiss()
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                textCorrections.forEach { (word, corrections) ->
                    item {
                        WordCorrectionItem(
                            word = word,
                            corrections = corrections,
                            onCorrectionAccepted = onCorrectionAccepted,
                            onAddDictionaryWord = onAddDictionaryWord,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCorrectionItem(
    word: String,
    corrections: List<String>,
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(12.dp))
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(corrections) {
                FilterChip(
                    selected = false,
                    onClick = { onCorrectionAccepted(word, it) },
                    label = { Text(text = it) },
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { onAddDictionaryWord(word) },
                    label = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun DateTimeEntry(
    dateTime: LocalDateTime,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onResetDate: () -> Unit,
    onResetDateToToday: (() -> Unit)?,
    onResetTime: () -> Unit,
    onResetTimeToNow: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            modifier = Modifier.weight(2f),
            onClick = { showDate = true },
        ) {
            Text(
                dayMonthDate(toFormat = dateTime.date),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = { showTime = true },
        ) {
            Text(
                text =
                    hourMinute(
                        dateTime,
                        amString = stringResource(Res.string.am),
                        pmString = stringResource(Res.string.pm),
                    ),
                textAlign = TextAlign.Center,
            )
        }
    }
    if (showDate) {
        Date(
            selectedDate = dateTime.date,
            onDismiss = { showDate = false },
            onResetDate = onResetDate,
            onResetDateToToday = onResetDateToToday,
            onDateSelected = {
                showDate = false
                onDateSelected(it)
            },
        )
    }

    if (showTime) {
        Time(
            selectedTime = dateTime.time,
            onTimeSelected = onTimeSelected,
            onDismiss = { showTime = false },
            onResetTime = onResetTime,
            onResetTimeToNow = onResetTimeToNow,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Time(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onResetTime: () -> Unit,
    onResetTimeToNow: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            val state =
                rememberTimePickerState(
                    initialHour = selectedTime.hour,
                    initialMinute = selectedTime.minute,
                    is24Hour = false,
                )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    onResetTimeToNow?.let {
                        TextButton(
                            onClick = {
                                onResetTimeToNow()
                                onDismiss()
                            },
                        ) {
                            Text(stringResource(Res.string.now))
                        }
                    }
                    TextButton(
                        onClick = {
                            onResetTime()
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(Res.string.reset))
                    }
                    TextButton(
                        onClick = {
                            onTimeSelected(LocalTime(state.hour, state.minute))
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(
    tags: List<Tag>,
    selectedTag: String?,
    showKeyboardShortcutHint: Boolean,
    onTagClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val additionalKeys = listOf("Y", "U", "I", "O", "P")
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.tags),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEachIndexed { index, tag ->
                Box {
                    FilterChip(
                        selected = tag.value == selectedTag,
                        onClick = { onTagClicked(tag.value) },
                        label = { Text(text = tag.value) },
                    )

                    val hint =
                        if (index == 9) {
                            "0"
                        } else if (index < 9) {
                            (index + 1).toString()
                        } else if (index <= 14) {
                            additionalKeys[index - 10]
                        } else {
                            null
                        }
                    if (showKeyboardShortcutHint && hint != null) {
                        Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                            Text(hint)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Templates(
    templates: List<JournalEntryTemplate>,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    showKeyboardShortcutHint: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.add_from_template),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEachIndexed { index, template ->
                Box {
                    FilterChip(
                        selected = false,
                        onClick = { onTemplateClicked(template) },
                        label = {
                            Text(text = "${template.shortDisplayText} ${template.displayText}")
                        },
                    )

                    val hint =
                        if (index == 9) {
                            "0"
                        } else if (index < 9) {
                            (index + 1).toString()
                        } else {
                            null
                        }
                    if (showKeyboardShortcutHint && hint != null) {
                        Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                            Text(hint)
                        }
                    }
                }
            }
        }
    }
}
