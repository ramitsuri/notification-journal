package com.ramitsuri.notificationjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.Tag
import kotlinx.coroutines.delay


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddEditEntryDialog(
    text: String,
    tags: List<Tag>,
    selectedTag: String?,
    suggestedText: String?,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
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
                    onValueChange = onTextUpdated,
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
                    SuggestedText(suggestedText, onUseSuggestedText = onUseSuggestedText)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (tags.isNotEmpty()) {
                    Tags(tags, selectedTag, onTagClicked)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = onSave) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tags(tags: List<Tag>, selectedTag: String?, onTagClicked: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags) {
            FilterChip(
                selected = it.value == selectedTag,
                onClick = { onTagClicked(it.value) },
                label = { Text(text = it.value, modifier = Modifier.padding(8.dp)) })
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