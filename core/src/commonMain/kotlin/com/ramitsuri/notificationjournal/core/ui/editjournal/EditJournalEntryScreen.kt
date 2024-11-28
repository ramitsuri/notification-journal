package com.ramitsuri.notificationjournal.core.ui.editjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.ui.components.AddEditEntryDialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
fun EditJournalEntryScreen(
    state: EditJournalEntryViewState,
    onTextUpdated: (String) -> Unit,
    onTagClicked: (String) -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onResetDate: () -> Unit,
    onResetTime: () -> Unit,
) {
    AddEditEntryDialog(
        isLoading = state.isLoading,
        text = state.text,
        tags = state.tags,
        selectedTag = state.selectedTag,
        suggestedText = state.suggestedText,
        templates = state.templates,
        dateTime = state.localDateTime,
        onTextUpdated = onTextUpdated,
        onTagClicked = onTagClicked,
        onTemplateClicked = onTemplateClicked,
        onUseSuggestedText = { },
        onSave = onSave,
        showAddAnother = false,
        onAddAnother = { },
        onCancel = onCancel,
        onDateSelected = onDateSelected,
        onTimeSelected = onTimeSelected,
        onPreviousDateRequested = onPreviousDateRequested,
        onNextDateRequested = onNextDateRequested,
        onResetDate = onResetDate,
        onResetTime = onResetTime,
    )
}