@file:OptIn(ExperimentalFoundationApi::class)

package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.formatForDisplay
import com.ramitsuri.notificationjournal.core.utils.getDay
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_save
import notificationjournal.core.generated.resources.add_entry_save_and_add_another
import notificationjournal.core.generated.resources.add_from_template
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.pm
import notificationjournal.core.generated.resources.reset
import notificationjournal.core.generated.resources.tags
import notificationjournal.core.generated.resources.use
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryDialog(
    isLoading: Boolean,
    textState: TextFieldState,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    showAddAnother: Boolean,
    dateTime: LocalDateTime,
    templates: List<JournalEntryTemplate>,
    onTagClicked: (String) -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onUseSuggestedText: () -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onResetDate: () -> Unit,
    onResetTime: () -> Unit,
) {
    var showTemplatesKeyboardShortcutHints by remember { mutableStateOf(false) }
    var showTagsKeyboardShortcutHints by remember { mutableStateOf(false) }
    var showDateKeyboardShortcutHints by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        delay(500)
        focusRequester.requestFocus()
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
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
                        if (textState.text.isEmpty()) {
                            // To prevent accidental cancel
                            onCancel()
                        }
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
            }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Toolbar(onBackClick = onCancel, scrollBehavior = scrollBehavior)
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                DateTimeEntry(
                    dateTime = dateTime,
                    onDateSelected = onDateSelected,
                    onTimeSelected = onTimeSelected,
                    onResetDate = onResetDate,
                    onResetTime = onResetTime,
                )
                Content(
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .weight(1f),
                    textState = textState,
                    tags = tags,
                    selectedTag = selectedTag,
                    suggestedText = suggestedText,
                    templates = templates,
                    showTagsKeyboardShortcutHint = showTagsKeyboardShortcutHints,
                    showTemplatesKeyboardShortcutHint = showTemplatesKeyboardShortcutHints,
                    onTagClicked = onTagClicked,
                    onTemplateClicked = onTemplateClicked,
                    onUseSuggestedText = onUseSuggestedText,
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
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
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    textState: TextFieldState,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    templates: List<JournalEntryTemplate>,
    showTagsKeyboardShortcutHint: Boolean,
    showTemplatesKeyboardShortcutHint: Boolean,
    onTagClicked: (String) -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onUseSuggestedText: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LaunchedEffect(textFieldFocusRequester) {
            if (showKeyboard && keyboard != null) {
                delay(500)
                textFieldFocusRequester.requestFocus()
                keyboard.show()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            state = textState,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            textStyle = MaterialTheme.typography.bodyMedium
                .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 10),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester = textFieldFocusRequester),
            decorator = { innerTextField ->
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
        if (!suggestedText.isNullOrEmpty()) {
            SuggestedText(suggestedText, onUseSuggestedText = onUseSuggestedText)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (templates.isNotEmpty()) {
            Templates(
                templates = templates,
                showKeyboardShortcutHint = showTemplatesKeyboardShortcutHint,
                onTemplateClicked = {
                    onTemplateClicked(it)
                    textFieldFocusRequester.requestFocus()
                },
                modifier = Modifier.onKeyEvent {
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
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (tags.isNotEmpty()) {
            Tags(
                tags = tags,
                selectedTag = selectedTag,
                onTagClicked = onTagClicked,
                showKeyboardShortcutHint = showTagsKeyboardShortcutHint,
                modifier = Modifier.onKeyEvent {
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
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DateTimeEntry(
    dateTime: LocalDateTime,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onResetDate: () -> Unit,
    onResetTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            modifier = Modifier.weight(2f),
            onClick = { showDate = true }
        ) {
            Text(
                getDay(toFormat = dateTime.date),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = { showTime = true }
        ) {
            Text(
                text = formatForDisplay(
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
            onDateSelected = onDateSelected,
        )
    }

    if (showTime) {
        Time(
            selectedTime = dateTime.time,
            onTimeSelected = onTimeSelected,
            onDismiss = { showTime = false },
            onResetTime = onResetTime,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Time(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onResetTime: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            val state = rememberTimePickerState(
                initialHour = selectedTime.hour,
                initialMinute = selectedTime.minute,
                is24Hour = false,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        onResetTime()
                        onDismiss()
                    }) {
                        Text(stringResource(Res.string.reset))
                    }
                    TextButton(onClick = {
                        onTimeSelected(LocalTime(state.hour, state.minute))
                        onDismiss()
                    }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Date(
    selectedDate: LocalDate? = null,
    allowedSelections: ClosedRange<LocalDate>? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    onResetDate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            fun Long.toLocalDate(): LocalDate {
                return Instant
                    .fromEpochMilliseconds(this)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
            }

            fun LocalDate.toMillisSinceEpoch(): Long {
                return this
                    .atStartOfDayIn(TimeZone.UTC)
                    .toEpochMilliseconds()
            }

            val yearRange = if (allowedSelections == null) {
                DatePickerDefaults.YearRange
            } else {
                allowedSelections.start.year..allowedSelections.endInclusive.year
            }
            val selectedDateMillis = remember(selectedDate) { selectedDate?.toMillisSinceEpoch() }
            val initialDisplayedMonthMillis =
                selectedDateMillis ?: allowedSelections?.endInclusive?.toMillisSinceEpoch()
            val state = rememberDatePickerState(
                initialSelectedDateMillis = selectedDateMillis,
                initialDisplayedMonthMillis = initialDisplayedMonthMillis,
                yearRange = yearRange,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        if (allowedSelections == null) {
                            return true
                        }
                        val pickedDate = utcTimeMillis.toLocalDate()
                        return allowedSelections.contains(pickedDate)
                    }

                    override fun isSelectableYear(year: Int): Boolean {
                        if (allowedSelections == null) {
                            return true
                        }
                        return (allowedSelections.start.year..allowedSelections.endInclusive.year).contains(
                            year
                        )
                    }
                },
            )
            var dateSelectedOnce by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                snapshotFlow { state.selectedDateMillis }.collect { selectedDateMillis ->
                    if (selectedDateMillis != null) {
                        onDateSelected?.invoke(selectedDateMillis.toLocalDate())
                        if (dateSelectedOnce) {
                            onDismiss()
                        } else {
                            dateSelectedOnce = true
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DatePicker(
                    state = state,
                    showModeToggle = false,
                    headline = null,
                    title = null,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        onResetDate()
                        onDismiss()
                    }) {
                        Text(stringResource(Res.string.reset))
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
                        label = { Text(text = tag.value) })

                    val hint = if (index == 9) {
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
                            Text(text = template.displayText)
                        })

                    val hint = if (index == 9) {
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

@Composable
private fun SuggestedText(
    suggestedText: String,
    onUseSuggestedText: () -> Unit,
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
        TextButton(onClick = { onUseSuggestedText() }) {
            Text(text = stringResource(Res.string.use))
        }
    }
}