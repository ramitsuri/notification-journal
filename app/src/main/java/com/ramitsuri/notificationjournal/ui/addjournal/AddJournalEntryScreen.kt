package com.ramitsuri.notificationjournal.ui.addjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.ui.components.AddEditEntryDialog

@Composable
fun AddJournalEntryScreen(
    state: AddJournalEntryViewState,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AddEditEntryDialog(
        isLoading = state.isLoading,
        text = state.text,
        tags = state.tags,
        selectedTag = state.selectedTag,
        suggestedText = state.suggestedText,
        onTextUpdated = onTextUpdated,
        onTagClicked = onTagClicked,
        onUseSuggestedText = onUseSuggestedText,
        onSave = onSave,
        onCancel = onCancel,
    )
}