package com.ramitsuri.notificationjournal.core.ui.editjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.ui.components.AddEditEntryDialog

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
        suggestedText = state.suggestedText,
        templates = listOf(),
        onTextUpdated = onTextUpdated,
        onTagClicked = onTagClicked,
        onUseSuggestedText = { },
        onSave = onSave,
        showAddAnother = false,
        onAddAnother = { },
        onCancel = onCancel,
        onTemplateClicked = { },
    )
}