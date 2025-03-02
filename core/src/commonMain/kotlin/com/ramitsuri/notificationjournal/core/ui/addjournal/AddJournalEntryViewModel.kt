package com.ramitsuri.notificationjournal.core.ui.addjournal

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.model.template.getShortcutTemplates
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.spellcheck.SpellChecker
import com.ramitsuri.notificationjournal.core.utils.PrefManager
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
import java.net.URLDecoder
import kotlin.time.Duration.Companion.days

class AddJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
    private val spellChecker: SpellChecker,
    private val clock: Clock = Clock.System,
    private val prefManager: PrefManager,
) : ViewModel() {
    private val receivedText: String? =
        if (savedStateHandle.get<String?>(RECEIVED_TEXT_ARG).isNullOrEmpty()) {
            null
        } else {
            URLDecoder.decode(savedStateHandle[RECEIVED_TEXT_ARG], "UTF-8")
        }
    private val duplicateFromEntryId: String? = savedStateHandle[DUPLICATE_FROM_ENTRY_ID_ARG]

    private val dateTime =
        savedStateHandle.get<String?>(DATE_ARG)
            ?.let { dateString ->
                val currentDateTime = clock.nowLocal()
                val timeString = savedStateHandle.get<String?>(TIME_ARG)
                val time =
                    if (timeString == null) {
                        currentDateTime.time
                    } else {
                        LocalTime.parse(timeString)
                    }
                LocalDate.parse(dateString).atTime(time)
            } ?: clock.nowLocal()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<AddJournalEntryViewState> =
        MutableStateFlow(
            AddJournalEntryViewState(
                textFieldState = TextFieldState(receivedText ?: ""),
                dateTime = dateTime,
                selectedTag = savedStateHandle[TAG_ARG],
            ),
        )
    val state: StateFlow<AddJournalEntryViewState> = _state
    private var enableGettingSuggestions = true

    init {
        loadTags()
        loadTemplates()
        loadFromDuplicateEntryId()
        loadCorrections()
        loadSuggestions()
    }

    fun tagClicked(tag: String) {
        _state.update {
            it.copy(selectedTag = tag)
        }
        viewModelScope.launch {
            val text = _state.value.textFieldState.text
            _state.update {
                it.copy(suggestions = getSuggestions(text = text, tag = tag))
            }
        }
    }

    fun templateClicked(template: JournalEntryTemplate) {
        // When entry is added by tapping the add button at the bottom of a tag, the time is derived
        // from the last entry in that tag but when adding via template, it's most likely not the
        // intention to use that time. So reset it.
        _state.update {
            it.copy(dateTime = it.dateTime.date.atTime(clock.nowLocal().time))
        }
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
        save(exitOnSave = true)
    }

    fun saveAndAddAnother() {
        save(exitOnSave = false)
    }

    fun dateSelected(date: LocalDate) {
        _state.update {
            it.copy(dateTime = date.atTime(it.dateTime.time))
        }
    }

    fun nextDay() {
        _state.update {
            it.copy(
                dateTime = it.dateTime.plus(1.days),
            )
        }
    }

    fun previousDay() {
        _state.update {
            it.copy(
                dateTime = it.dateTime.minus(1.days),
            )
        }
    }

    fun timeSelected(time: LocalTime) {
        _state.update {
            it.copy(dateTime = LocalDateTime(it.dateTime.date, time))
        }
    }

    fun resetDate() {
        val currentDateTime = _state.value.dateTime
        val originalDateTime = dateTime
        val resetDateTime = LocalDateTime(date = originalDateTime.date, time = currentDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime) }
    }

    fun resetDateToToday() {
        val currentDateTime = _state.value.dateTime
        val resetDateTime =
            LocalDateTime(
                date = clock.nowLocal().date,
                time = currentDateTime.time,
            )
        _state.update { it.copy(dateTime = resetDateTime) }
    }

    fun resetTime() {
        val currentDateTime = _state.value.dateTime
        val originalDateTime = dateTime
        val resetDateTime = LocalDateTime(date = currentDateTime.date, time = originalDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime) }
    }

    fun resetTimeToNow() {
        val currentDateTime = _state.value.dateTime
        val resetDateTime =
            LocalDateTime(
                date = currentDateTime.date,
                time = clock.nowLocal().time,
            )
        _state.update { it.copy(dateTime = resetDateTime) }
    }

    fun correctionAccepted(
        word: String,
        correction: String,
    ) {
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

    fun onSuggestionClicked(suggestion: String?) {
        if (suggestion != null) {
            enableGettingSuggestions = false
            _state.value.textFieldState.edit {
                delete(0, length)
                insert(0, suggestion)
            }
        }
        _state.update { it.copy(suggestions = listOf()) }
    }

    override fun onCleared() {
        super.onCleared()
        spellChecker.reset()
    }

    private fun save(exitOnSave: Boolean) {
        val currentState = _state.value
        val text = currentState.textFieldState.text.toString()
        if (text.isEmpty()) {
            return
        }
        _state.update { it.copy(isLoading = true) }
        val tag = currentState.tags.firstOrNull { it.value == currentState.selectedTag }?.value
        viewModelScope.launch {
            repository.insert(
                text = text,
                tag = tag,
                time = currentState.dateTime,
            )
            if (exitOnSave) {
                _saved.update {
                    true
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        textFieldState = TextFieldState(),
                        selectedTag = null,
                    )
                }
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            _state.update {
                it.copy(tags = tagsDao.getAll())
            }
        }
        viewModelScope.launch {
            _state.update {
                val defaultTag = prefManager.getDefaultTag()
                if (it.selectedTag == null || Tag.isNoTag(it.selectedTag) && Tag.isNoTag(defaultTag)) {
                    it.copy(selectedTag = defaultTag)
                } else {
                    it
                }
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    templates =
                        templatesDao.getAll()
                            .plus(JournalEntryTemplate.getShortcutTemplates()),
                )
            }
        }
    }

    private fun loadFromDuplicateEntryId() {
        val id = duplicateFromEntryId ?: return
        viewModelScope.launch {
            val fromEntry = repository.get(id) ?: return@launch
            _state.update {
                it.copy(
                    textFieldState = TextFieldState(fromEntry.text),
                    selectedTag = fromEntry.tag,
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

    @OptIn(FlowPreview::class)
    private fun loadSuggestions() {
        viewModelScope.launch {
            snapshotFlow {
                _state.value.textFieldState.text
            }.debounce(300)
                .collect { text ->
                    val tag = _state.value.selectedTag
                    _state.update {
                        it.copy(suggestions = getSuggestions(text = text, tag = tag))
                    }
                }
        }
    }

    private suspend fun getSuggestions(
        text: CharSequence,
        tag: String?,
    ): List<String> {
        // Disable so that we don't get the same suggestion again from text field changing from selected suggestion
        if (!enableGettingSuggestions) {
            enableGettingSuggestions = true
            return listOf()
        }
        if (tag == null || Tag.isNoTag(tag)) {
            return listOf()
        }
        if (text.length < 2) {
            return listOf()
        }
        return repository.search(text.toString(), listOf(tag)).take(10).map { it.text }.distinct()
    }

    companion object {
        const val RECEIVED_TEXT_ARG = "received_text"
        const val DUPLICATE_FROM_ENTRY_ID_ARG = "duplicate_from_entry_id"
        const val DATE_ARG = "date_arg"
        const val TAG_ARG = "tag_arg"
        const val TIME_ARG = "time_arg"
    }
}

data class AddJournalEntryViewState(
    val isLoading: Boolean = false,
    val textFieldState: TextFieldState = TextFieldState(),
    val tags: List<Tag> = listOf(),
    val selectedTag: String? = null,
    val templates: List<JournalEntryTemplate> = listOf(),
    val corrections: Map<String, List<String>> = mapOf(),
    val dateTime: LocalDateTime,
    val suggestions: List<String> = listOf(),
) {
    val showWarningOnExit
        get() = textFieldState.text.toString().isNotEmpty()
}
