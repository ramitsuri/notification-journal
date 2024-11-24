package com.ramitsuri.notificationjournal.core.ui.addjournal

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
import kotlinx.datetime.atStartOfDayIn
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

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<AddJournalEntryViewState> = MutableStateFlow(
        AddJournalEntryViewState(
            text = receivedText ?: "",
            dateTime = savedStateHandle.get<String?>(DATE_ARG)
                ?.let { dateString ->
                    LocalDate.parse(dateString).atStartOfDayIn(zoneId)
                } ?: clock.now(),
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

    fun textUpdated(text: String) {
        _state.update {
            it.copy(text = text)
        }
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
                it.copy(text = suggestedText, suggestedText = null)
            }
        }
    }

    fun templateClicked(template: JournalEntryTemplate) {
        _state.update { previousState ->
            val newText = if (template.replacesExistingValues) {
                tagClicked(template.tag)
                template.text
            } else {
                previousState.text + template.text
            }
            previousState.copy(text = newText)
        }
    }

    fun save() {
        save(exitOnSave = true)
    }

    fun saveAndAddAnother() {
        save(exitOnSave = false)
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

    fun setHour(hourString: String) {
        val hour = hourString.ifEmpty { "0" }.toIntOrNull() ?: return

        val hourToSet = if ((0..23).contains(hour).not()) {
            if (hour % 10 == 0) {
                hour / 10
            } else {
                hour % 10
            }
        } else {
            hour
        }
        setHourAndMinute(hour = hourToSet)
    }

    fun setMinute(minuteString: String) {
        val minute = minuteString.ifEmpty { "0" }.toIntOrNull() ?: return

        val minuteToSet = if ((0..59).contains(minute).not()) {
            if (minute % 10 == 0) {
                minute / 10
            } else {
                minute % 10
            }
        } else {
            minute
        }
        setHourAndMinute(minute = minuteToSet)
    }

    fun resetDateTime() {
        _state.update { it.copy(dateTime = clock.now()) }
    }

    private fun setHourAndMinute(hour: Int? = null, minute: Int? = null) {
        _state.update {
            val previousDateTime = it.dateTime.toLocalDateTime(it.timeZone)
            val previousTime = previousDateTime.time

            val newTime = LocalTime(
                hour = hour ?: previousTime.hour,
                minute = minute ?: previousTime.minute,
                second = previousTime.second,
                nanosecond = previousTime.nanosecond,
            )
            it.copy(
                dateTime = LocalDateTime(previousDateTime.date, newTime).toInstant(it.timeZone)
            )
        }
    }

    private fun save(exitOnSave: Boolean) {
        val currentState = _state.value
        val text = currentState.text
        if (text.isEmpty()) {
            return
        }
        _state.update { it.copy(isLoading = true) }
        val tag = currentState.selectedTag
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
                    it.copy(isLoading = false, text = "", selectedTag = null, suggestedText = null)
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
                it.copy(text = fromEntry.text, selectedTag = fromEntry.tag)
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
    val text: String = "",
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