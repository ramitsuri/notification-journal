@file:OptIn(ExperimentalFoundationApi::class)

package com.ramitsuri.notificationjournal.core.ui.addjournal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.insert
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.model.template.getShortcutTemplates
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.net.URLDecoder
import kotlin.time.Duration.Companion.days

class AddJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
    private val clock: Clock = Clock.System,
    private val zoneId: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val receivedText: String? =
        if (savedStateHandle.get<String?>(RECEIVED_TEXT_ARG).isNullOrEmpty()) {
            null
        } else {
            URLDecoder.decode(savedStateHandle[RECEIVED_TEXT_ARG], "UTF-8")
        }
    private val duplicateFromEntryId: String? = savedStateHandle[DUPLICATE_FROM_ENTRY_ID_ARG]

    private val dateTime = savedStateHandle.get<String?>(DATE_ARG)
        ?.let { dateString ->
            val currentDateTime = clock.now().toLocalDateTime(zoneId)
            LocalDate.parse(dateString).atTime(currentDateTime.time).toInstant(zoneId)
        } ?: clock.now()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<AddJournalEntryViewState> = MutableStateFlow(
        AddJournalEntryViewState(
            textFieldState = TextFieldState(receivedText ?: ""),
            dateTime = dateTime,
            timeZone = zoneId,
            selectedTag = savedStateHandle[TAG_ARG],
        )
    )
    val state: StateFlow<AddJournalEntryViewState> = _state

    init {
        loadTags()
        loadTemplates()
        loadFromDuplicateEntryId()
    }

    fun tagClicked(tag: String) {
        _state.update {
            it.copy(selectedTag = tag)
        }
    }

    fun useSuggestedText() {
        val suggestedText = _state.value.suggestedText
        if (suggestedText != null) {
            _state.update {
                it.copy(textFieldState = TextFieldState(suggestedText), suggestedText = null)
            }
        }
    }

    fun templateClicked(template: JournalEntryTemplate) {
        _state.value.textFieldState.edit {
            if (template.replacesExistingValues) {
                tagClicked(template.tag)
                delete(0, length)
                insert(0, template.text)
            } else {
                insert(selectionInChars.start, template.text)
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
            it.copy(dateTime = date.atTime(it.localDateTime.time).toInstant(it.timeZone))
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

    fun timeSelected(time: LocalTime) {
        _state.update {
            it.copy(dateTime = LocalDateTime(it.localDateTime.date, time).toInstant(it.timeZone))
        }
    }

    fun resetDate() {
        val currentDateTime = _state.value.localDateTime
        val originalDateTime = dateTime.toLocalDateTime(zoneId)
        val resetDateTime = LocalDateTime(date = originalDateTime.date, time = currentDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime.toInstant(zoneId)) }
    }

    fun resetTime() {
        val currentDateTime = _state.value.localDateTime
        val originalDateTime = dateTime.toLocalDateTime(zoneId)
        val resetDateTime = LocalDateTime(date = currentDateTime.date, time = originalDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime.toInstant(zoneId)) }
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
                time = currentState.dateTime
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
                        suggestedText = null,
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

    companion object {
        const val RECEIVED_TEXT_ARG = "received_text"
        const val DUPLICATE_FROM_ENTRY_ID_ARG = "duplicate_from_entry_id"
        const val DATE_ARG = "date_arg"
        const val TAG_ARG = "tag_arg"
    }
}

data class AddJournalEntryViewState(
    val isLoading: Boolean = false,
    val textFieldState: TextFieldState = TextFieldState(),
    val tags: List<Tag> = listOf(),
    val selectedTag: String? = null,
    val suggestedText: String? = null,
    val templates: List<JournalEntryTemplate> = listOf(),
    val dateTime: Instant,
    val timeZone: TimeZone,
) {

    val localDateTime: LocalDateTime
        get() = dateTime.toLocalDateTime(timeZone)
}