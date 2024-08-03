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
        templates = listOf(),
        dateTime = state.localDateTime,
        onTextUpdated = onTextUpdated,
        onTagClicked = onTagClicked,
        onUseSuggestedText = { },
        onSave = onSave,
        showAddAnother = false,
        onAddAnother = { },
        onCancel = onCancel,
        onPreviousDateRequested = onPreviousDateRequested,
        onNextDateRequested = onNextDateRequested,
        onHourUpdated = onHourUpdated,
        onMinuteUpdated = onMinuteUpdated,
        onResetDateTime = onResetDateTime,
    )
}