package com.ramitsuri.notificationjournal.ui.addjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.ui.components.AddEditEntryDialog

@Composable
fun AddJournalEntryScreen(
    state: AddJournalEntryViewState,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onUseSuggestedText: () -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
) {
    AddEditEntryDialog(
        isLoading = state.isLoading,
        text = state.text,
        tags = state.tags,
        selectedTag = state.selectedTag,
        suggestedText = state.suggestedText,
        showAddAnother = true,
        templates = state.templates,
        onTextUpdated = onTextUpdated,
        onTagClicked = onTagClicked,
        onUseSuggestedText = onUseSuggestedText,
        onTemplateClicked = onTemplateClicked,
        onSave = onSave,
        onAddAnother = onAddAnother,
        onCancel = onCancel,
    )
}