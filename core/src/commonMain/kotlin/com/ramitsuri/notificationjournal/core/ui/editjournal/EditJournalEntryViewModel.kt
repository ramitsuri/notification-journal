@file:OptIn(ExperimentalFoundationApi::class)

package com.ramitsuri.notificationjournal.core.ui.editjournal

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
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
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
import kotlin.time.Duration.Companion.days

class EditJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
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
                it.copy(
                    isLoading = false,
                    textFieldState = TextFieldState(entry.text),
                    selectedTag = entry.tag,
                    dateTime = entry.entryTime,
                    timeZone = entry.timeZone,
                )
            }
        }
        loadTags()
        loadTemplates()
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
                insert(selectionInChars.start, template.text)
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
            it.copy(dateTime = date.atTime(it.localDateTime.time).toInstant(it.timeZone))
        }
    }

    fun timeSelected(time: LocalTime) {
        _state.update {
            it.copy(dateTime = LocalDateTime(it.localDateTime.date, time).toInstant(it.timeZone))
        }
    }

    fun resetDate() {
        val currentDateTime = _state.value.localDateTime
        val originalDateTime = entry.entryTime.toLocalDateTime(entry.timeZone)
        val resetDateTime = LocalDateTime(date = originalDateTime.date, time = currentDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime.toInstant(entry.timeZone)) }
    }

    fun resetTime() {
        val currentDateTime = _state.value.localDateTime
        val originalDateTime = entry.entryTime.toLocalDateTime(entry.timeZone)
        val resetDateTime = LocalDateTime(date = currentDateTime.date, time = originalDateTime.time)
        _state.update { it.copy(dateTime = resetDateTime.toInstant(entry.timeZone)) }
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
    val dateTime: Instant = Clock.System.now(),
    val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    val localDateTime: LocalDateTime
        get() = dateTime.toLocalDateTime(timeZone)
}