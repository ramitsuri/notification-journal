package com.ramitsuri.notificationjournal.core.ui.addjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.ui.components.AddEditEntryDialog

@Composable
fun AddJournalEntryScreen(
    state: AddJournalEntryViewState,
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
    AddEditEntryDialog(
        isLoading = state.isLoading,
        text = state.text,
        tags = state.tags,
        selectedTag = state.selectedTag,
        suggestedText = state.suggestedText,
        showAddAnother = true,
        templates = state.templates,
        dateTime = state.localDateTime,
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