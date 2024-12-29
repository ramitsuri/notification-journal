package com.ramitsuri.notificationjournal.core.ui.editjournal

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.model.template.getShortcutTemplates
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.spellcheck.SpellChecker
import com.ramitsuri.notificationjournal.core.utils.minus
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.time.Duration.Companion.days

class EditJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
    private val spellChecker: SpellChecker,
) : ViewModel() {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<EditJournalEntryViewState> =
        MutableStateFlow(EditJournalEntryViewState())
    val state: StateFlow<EditJournalEntryViewState> = _state

    private lateinit var entry: JournalEntry

    init {
        viewModelScope.launch {
            entry = repository.get(checkNotNull(savedStateHandle[ENTRY_ID_ARG])) ?: return@launch
            _state.update {
                it.textFieldState.setTextAndPlaceCursorAtEnd(entry.text)
                it.copy(
                    isLoading = false,
                    selectedTag = entry.tag,
                    dateTime = entry.entryTime,
                )
            }
        }
        loadTags()
        loadTemplates()
        loadCorrections()
    }

    fun tagClicked(tag: String) {
        _state.update {
            it.copy(selectedTag = tag)
        }
    }

    fun templateClicked(template: JournalEntryTemplate) {
        _state.value.textFieldState.edit {
            if (template.replacesExistingValues) {
                tagClicked(template.tag)
                delete(0, length)
                insert(0, template.text)
            } else {
                insert(selection.start, template.text)
            }
        }
    }

    fun save() {
        if (!::entry.isInitialized) {
            return
        }
        val currentState = _state.value
        val text = currentState.textFieldState.text.toString()
        if (text.isEmpty()) {
            return
        }
        val dateTime = currentState.dateTime
        _state.update { it.copy(isLoading = true) }
        val tag = currentState.selectedTag
        viewModelScope.launch {
            repository.updateText(entry.copy(text = text, tag = tag, entryTime = dateTime))
            _saved.update {
                true
            }
        }
    }

    fun nextDay() {
        _state.update {
            it.copy(
                dateTime = it.dateTime.plus(1.days)
            )
        }
    }

    fun previousDay() {
        _state.update {
            it.copy(
                dateTime = it.dateTime.minus(1.days)
            )
        }
    }

    fun dateSelected(date: LocalDate) {
        _state.update {
            it.copy(dateTime = date.atTime(it.dateTime.time))
        }
    }

    fun timeSelected(time: LocalTime) {
        _state.update {
            it.copy(dateTime = LocalDateTime(it.dateTime.date, time))
        }
    }

    fun resetDate() {
        val currentDateTime = _state.value.dateTime
        val originalDateTime = entry.entryTime
        val resetDateTime = LocalDateTime(date = originalDateTime.date, time = currentDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime) }
    }

    fun resetTime() {
        val currentDateTime = _state.value.dateTime
        val originalDateTime = entry.entryTime
        val resetDateTime = LocalDateTime(date = currentDateTime.date, time = originalDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime) }
    }

    fun correctionAccepted(word: String, correction: String) {
        _state.value.textFieldState.apply {
            var startIndexForSearch = 0
            var start = text.indexOf(string = word, startIndex = startIndexForSearch)
            while (start >= 0) {
                edit {
                    replace(start, start + word.length, correction)
                }
                startIndexForSearch = start + word.length
                start = text.indexOf(string = word, startIndex = startIndexForSearch)
            }
        }
    }

    fun addDictionaryWord(word: String) {
        viewModelScope.launch {
            spellChecker.addWord(word)
        }
    }

    override fun onCleared() {
        super.onCleared()
        spellChecker.reset()
    }

    private fun loadTags() {
        viewModelScope.launch {
            _state.update {
                it.copy(tags = tagsDao.getAll())
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    templates = templatesDao.getAll()
                        .plus(JournalEntryTemplate.getShortcutTemplates())
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun loadCorrections() {
        viewModelScope.launch {
            snapshotFlow {
                _state.value.textFieldState.text
            }
                .debounce(300)
                .collect { text ->
                    spellChecker.onTextUpdated(text.toString())
                }
        }
        viewModelScope.launch {
            spellChecker
                .corrections
                .collect { corrections ->
                    _state.update {
                        it.copy(corrections = corrections)
                    }
                }
        }
    }

    companion object {
        const val ENTRY_ID_ARG = "entry_id"
    }
}

data class EditJournalEntryViewState(
    val isLoading: Boolean = true,
    val textFieldState: TextFieldState = TextFieldState(),
    val tags: List<Tag> = listOf(),
    val selectedTag: String? = null,
    val suggestedText: String? = null,
    val templates: List<JournalEntryTemplate> = listOf(),
    val corrections: Map<String, List<String>> = mapOf(),
    val dateTime: LocalDateTime = Clock.System.nowLocal(),
)