package com.ramitsuri.notificationjournal.core.ui.addjournal

import androidx.compose.runtime.Composable
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.ui.components.AddEditEntryDialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
fun AddJournalEntryScreen(
    state: AddJournalEntryViewState,
    onSuggestionsEnabledChanged: () -> Unit,
    onTagClicked: (String) -> Unit,
    onTemplateClicked: (JournalEntryTemplate) -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDateRequested: () -> Unit,
    onNextDateRequested: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onResetDate: () -> Unit,
    onResetDateToToday: (() -> Unit)?,
    onResetTime: () -> Unit,
    onResetTimeToNow: (() -> Unit)?,
    onCorrectionAccepted: (String, String) -> Unit,
    onAddDictionaryWord: (String) -> Unit,
    onSuggestionClicked: (String?) -> Unit,
) {
    AddEditEntryDialog(
        isLoading = state.isLoading,
        textState = state.textFieldState,
        tags = state.tags,
        selectedTag = state.selectedTag,
        showAddAnother = true,
        templates = state.templates,
        dateTime = state.dateTime,
        textCorrections = state.corrections,
        showWarningOnExit = state.showWarningOnExit,
        suggestions = state.suggestions,
        showSuggestions = state.suggestionsEnabled,
        onTagClicked = onTagClicked,
        onTemplateClicked = onTemplateClicked,
        onSave = onSave,
        onAddAnother = onAddAnother,
        onCancel = onCancel,
        onDateSelected = onDateSelected,
        onPreviousDateRequested = onPreviousDateRequested,
        onNextDateRequested = onNextDateRequested,
        onTimeSelected = onTimeSelected,
        onResetDate = onResetDate,
        onResetDateToToday = onResetDateToToday,
        onResetTime = onResetTime,
        onResetTimeToNow = onResetTimeToNow,
        onCorrectionAccepted = onCorrectionAccepted,
        onAddDictionaryWord = onAddDictionaryWord,
        onSuggestionClicked = onSuggestionClicked,
        onSuggestionsEnabledChanged = onSuggestionsEnabledChanged,
    )
}
