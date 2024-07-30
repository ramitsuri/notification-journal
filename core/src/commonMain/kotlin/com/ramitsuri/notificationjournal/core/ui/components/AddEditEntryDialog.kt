package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.getDay
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_save
import notificationjournal.core.generated.resources.add_entry_save_and_add_another
import notificationjournal.core.generated.resources.add_from_template
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.next_day
import notificationjournal.core.generated.resources.previous_day
import notificationjournal.core.generated.resources.tags
import notificationjournal.core.generated.resources.use
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddEditEntryDialog(
    isLoading: Boolean,
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    showAddAnother: Boolean,
    dateTime: LocalDateTime,
    templates: List<JournalEntryTemplate>,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onHourUpdated: (String) -> Unit,
    onMinuteUpdated: (String) -> Unit,
    onResetDateTime: () -> Unit,
) {

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(modifier = Modifier.onKeyEvent {
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

                // Escape -> Cancel
                it.key == Key.Escape &&
                        it.type == KeyEventType.KeyUp -> {
                    onCancel()
                    true
                }

                else -> {
                    false
                }
            }
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    Content(
                        text = text,
                        tags = tags,
                        selectedTag = selectedTag,
                        suggestedText = suggestedText,
                        showAddAnother = showAddAnother,
                        templates = templates,
                        dateTime = dateTime,
                        onTextUpdated = onTextUpdated,
                        onTagClicked = onTagClicked,
                        onUseSuggestedText = onUseSuggestedText,
                        onSave = onSave,
                        onAddAnother = onAddAnother,
                        onCancel = onCancel,
                        onPreviousDateRequested = onPreviousDateRequested,
                        onNextDateRequested = onNextDateRequested,
                        onHourUpdated = onHourUpdated,
                        onMinuteUpdated = onMinuteUpdated,
                        onResetDateTime = onResetDateTime,
                    )
                }
            }
        }
    }
}

@Composable
private fun Content(
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    showAddAnother: Boolean,
    dateTime: LocalDateTime,
    templates: List<JournalEntryTemplate>,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onHourUpdated: (String) -> Unit,
    onMinuteUpdated: (String) -> Unit,
    onResetDateTime: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    var selection by remember { mutableStateOf(TextRange(text.length)) }
    Column {
        LaunchedEffect(textFieldFocusRequester) {
            if (showKeyboard) {
                delay(100)
                textFieldFocusRequester.requestFocus()
                keyboard?.show()
            }
        }
        DateTimeEntry(
            dateTime = dateTime,
            onPreviousDateRequested = onPreviousDateRequested,
            onNextDateRequested = onNextDateRequested,
            onHourUpdated = onHourUpdated,
            onMinuteUpdated = onMinuteUpdated,
            onResetDateTime = onResetDateTime,
        )
        BasicTextField(
            value = TextFieldValue(text = text, selection = selection),
            onValueChange = {
                onTextUpdated(it.text)
                selection = it.selection
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            textStyle = MaterialTheme.typography.bodyMedium
                .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 10,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester = textFieldFocusRequester)
                .onKeyEvent {
                    when {
                        // Tab -> Focus next
                        it.key == Key.T &&
                                it.type == KeyEventType.KeyUp &&
                                it.isMetaPressed -> {
                            focusManager.moveFocus(FocusDirection.Next)
                        }

                        else -> {
                            false
                        }
                    }
                },
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
        if (!suggestedText.isNullOrEmpty()) {
            SuggestedText(suggestedText, onUseSuggestedText = onUseSuggestedText)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (tags.isNotEmpty()) {
            Tags(
                tags = tags,
                selectedTag = selectedTag,
                onTagClicked = onTagClicked,
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
        if (templates.isNotEmpty()) {
            Templates(
                templates = templates,
                onTemplateClicked = {
                    val newText = if (it.replacesExistingValues) {
                        onTagClicked(it.tag)
                        it.text
                    } else {
                        text + it.text
                    }
                    onTextUpdated(newText)
                    selection = TextRange(newText.length)
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
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showAddAnother) {
                TextButton(onClick = onAddAnother) {
                    Text(text = stringResource(Res.string.add_entry_save_and_add_another))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onCancel) {
                Text(text = stringResource(Res.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onSave) {
                Text(text = stringResource(Res.string.add_entry_save))
            }
        }
    }
}

@Composable
private fun DateTimeEntry(
    dateTime: LocalDateTime,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onHourUpdated: (String) -> Unit,
    onMinuteUpdated: (String) -> Unit,
    onResetDateTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedIconButton(
                onClick = onPreviousDateRequested,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                    contentDescription = stringResource(Res.string.previous_day)
                )
            }
            Text(getDay(toFormat = dateTime.date))
            OutlinedIconButton(
                onClick = onNextDateRequested,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = stringResource(Res.string.next_day)
                )
            }
        }
        /* Hiding because it doesn't get frequent use. Might remove it in the future.
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberTextField(
                text = dateTime.time.hour.toString(),
                onTextUpdated = onHourUpdated,
                modifier = Modifier.weight(1f),
            )
            Text(":", modifier = Modifier.padding(horizontal = 16.dp))
            NumberTextField(
                text = dateTime.time.minute.toString(),
                onTextUpdated = onMinuteUpdated,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedIconButton(
                onClick = onResetDateTime,
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(Res.string.reset_date_time)
                )
            }
        }*/
    }
}

@Composable
private fun NumberTextField(
    text: String,
    onTextUpdated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selection by remember { mutableStateOf(TextRange(text.length)) }
    OutlinedTextField(
        value = TextFieldValue(text = text, selection = selection),
        onValueChange = {
            onTextUpdated(it.text)
            selection = it.selection
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        textStyle = MaterialTheme.typography.bodyMedium
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        maxLines = 1,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(
    tags: List<Tag>,
    selectedTag: String?,
    onTagClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.tags),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach {
                FilterChip(
                    selected = it.value == selectedTag,
                    onClick = { onTagClicked(it.value) },
                    label = { Text(text = it.value) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Templates(
    templates: List<JournalEntryTemplate>,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.add_from_template),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEach {
                FilterChip(
                    selected = false,
                    onClick = { onTemplateClicked(it) },
                    label = {
                        Text(text = it.displayText)
                    })
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