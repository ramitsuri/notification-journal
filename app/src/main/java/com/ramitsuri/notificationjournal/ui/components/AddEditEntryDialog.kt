package com.ramitsuri.notificationjournal.ui.components

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.delay

@Composable
fun AddEditEntryDialog(
    isLoading: Boolean,
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    showAddAnother: Boolean,
    templates: List<JournalEntryTemplate>,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
) {

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
                        onTextUpdated = onTextUpdated,
                        onTagClicked = onTagClicked,
                        onUseSuggestedText = onUseSuggestedText,
                        onTemplateClicked = onTemplateClicked,
                        onSave = onSave,
                        onAddAnother = onAddAnother,
                        onCancel = onCancel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Content(
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    showAddAnother: Boolean,
    templates: List<JournalEntryTemplate>,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    var selection by remember { mutableStateOf(TextRange(text.length)) }
    Column {
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
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            textStyle = MaterialTheme.typography.bodyMedium
                .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 10,
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
        if (!suggestedText.isNullOrEmpty()) {
            SuggestedText(suggestedText, onUseSuggestedText = onUseSuggestedText)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (tags.isNotEmpty()) {
            Tags(tags, selectedTag, onTagClicked)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (templates.isNotEmpty()) {
            Templates(templates, onTemplateClicked)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showAddAnother) {
                TextButton(onClick = onAddAnother) {
                    Text(text = stringResource(id = R.string.add_entry_save_and_add_another))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onSave) {
                Text(text = stringResource(id = R.string.add_entry_save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: List<Tag>, selectedTag: String?, onTagClicked: (String) -> Unit) {
    Column {
        Text(
            text = stringResource(id = R.string.tags),
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun Templates(
    templates: List<JournalEntryTemplate>,
    onTemplateClicked: (JournalEntryTemplate) -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.add_from_template),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEach {
                FilterChip(
                    selected = false,
                    onClick = { onTemplateClicked(it) },
                    label = {
                        Text(text = it.text)
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
            Text(text = stringResource(id = R.string.use))
        }
    }
}