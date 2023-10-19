package com.ramitsuri.notificationjournal.ui.editjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.ui.components.AddEditEntryDialog

@Composable
fun EditJournalEntryScreen(
    state: EditJournalEntryViewState,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AddEditEntryDialog(
        isLoading = state.isLoading,
        text = state.text,
        tags = state.tags,
        selectedTag = state.selectedTag,
        suggestedText = null,
        onTextUpdated = onTextUpdated,
        onTagClicked = onTagClicked,
        onUseSuggestedText = { },
        onSave = onSave,
        showAddAnother = false,
        onAddAnother = { },
        onCancel = onCancel,
    )
}