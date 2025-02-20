package com.ramitsuri.notificationjournal.core.ui.editjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.ui.components.AddEditEntryDialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
fun EditJournalEntryScreen(
    state: EditJournalEntryViewState,
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
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
    onSuggestionClicked: (String?) -> Unit,
) {
    AddEditEntryDialog(
        isLoading = state.isLoading,
        textState = state.textFieldState,
        tags = state.tags,
        selectedTag = state.selectedTag,
        templates = state.templates,
        dateTime = state.dateTime,
        textCorrections = state.corrections,
        showWarningOnExit = state.showWarningOnExit,
        suggestions = state.suggestions,
        onTagClicked = onTagClicked,
        onTemplateClicked = onTemplateClicked,
        onSave = onSave,
        showAddAnother = false,
        onAddAnother = { },
        onCancel = onCancel,
        onDateSelected = onDateSelected,
        onTimeSelected = onTimeSelected,
        onPreviousDateRequested = onPreviousDateRequested,
        onNextDateRequested = onNextDateRequested,
        onResetDate = onResetDate,
        onResetDateToToday = null,
        onResetTime = onResetTime,
        onResetTimeToNow = null,
        onCorrectionAccepted = onCorrectionAccepted,
        onAddDictionaryWord = onAddDictionaryWord,
        onSuggestionClicked = onSuggestionClicked,
    )
}
