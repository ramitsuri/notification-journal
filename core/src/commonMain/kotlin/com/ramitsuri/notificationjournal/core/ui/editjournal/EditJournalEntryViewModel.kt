package com.ramitsuri.notificationjournal.core.ui.editjournal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

class EditJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
) : ViewModel() {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<EditJournalEntryViewState> =
        MutableStateFlow(EditJournalEntryViewState.default())
    val state: StateFlow<EditJournalEntryViewState> = _state

    private lateinit var entry: JournalEntry

    init {
        viewModelScope.launch {
            entry = repository.get(checkNotNull(savedStateHandle[ENTRY_ID_ARG]))
            val entryTime = entry.entryTimeOverride ?: entry.entryTime
            _state.update {
                it.copy(
                    isLoading = false,
                    text = entry.text,
                    selectedTag = entry.tag,
                    dateTime = entryTime,
                    timeZone = entry.timeZone,
                )
            }
        }
        loadTags()
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

    fun save() {
        if (!::entry.isInitialized) {
            return
        }
        val currentState = _state.value
        val text = currentState.text
        if (text.isEmpty()) {
            return
        }
        val dateTime = currentState.dateTime
        _state.update { it.copy(isLoading = true) }
        val tag = currentState.selectedTag
        viewModelScope.launch {
            repository.update(entry.copy(text = text, tag = tag, entryTimeOverride = dateTime))
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
        _state.update { it.copy(dateTime = entry.entryTimeOverride ?: entry.entryTime) }
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

    private fun loadTags() {
        viewModelScope.launch {
            _state.update {
                it.copy(tags = tagsDao.getAll())
            }
        }
    }

    companion object {
        const val ENTRY_ID_ARG = "entry_id"
    }
}

data class EditJournalEntryViewState(
    val isLoading: Boolean,
    val text: String,
    val tags: List<Tag>,
    val selectedTag: String?,
    val suggestedText: String?,
    val dateTime: Instant,
    val timeZone: TimeZone,
) {

    val localDateTime: LocalDateTime
        get() = dateTime.toLocalDateTime(timeZone)

    companion object {
        fun default() = EditJournalEntryViewState(
            isLoading = true,
            text = "",
            tags = listOf(),
            selectedTag = null,
            suggestedText = null,
            dateTime = Clock.System.now(),
            timeZone = TimeZone.currentSystemDefault(),
        )
    }
}