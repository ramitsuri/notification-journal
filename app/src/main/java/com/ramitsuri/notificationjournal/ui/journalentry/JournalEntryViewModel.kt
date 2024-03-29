package com.ramitsuri.notificationjournal.ui.journalentry

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.di.ServiceLocator
import com.ramitsuri.notificationjournal.utils.receivedText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class JournalEntryViewModel(
    receivedText: String?,
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private var collectionJob: Job? = null

    private val _receivedText: MutableStateFlow<String?> = MutableStateFlow(receivedText)
    val receivedText: StateFlow<String?> = _receivedText

    private val _state = MutableStateFlow(
        ViewState(
            dayGroups = listOf(),
            tags = listOf(),
            loading = false,
        )
    )
    val state: StateFlow<ViewState> = _state

    init {
        restartCollection()
    }

    fun setReceivedText(text: String?) {
        if (!text.isNullOrEmpty()) {
            _receivedText.update {
                text
            }
        }
    }

    fun onReceivedTextConsumed() {
        _receivedText.update {
            null
        }
    }

    fun delete(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.delete(journalEntry)
        }
    }

    fun delete(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                repository.delete(journalEntry)
            }
        }
    }

    fun editTag(journalEntry: JournalEntry, tag: String) {
        if (journalEntry.tag == tag) {
            return
        }
        viewModelScope.launch {
            repository.editTag(journalEntry.id, tag)
        }
    }

    fun moveToPreviousDay(journalEntry: JournalEntry) {
        val currentEntryTime = journalEntry.entryTimeOverride ?: journalEntry.entryTime
        val previousDayTime = currentEntryTime.minusSeconds(1.days.toJavaDuration().seconds)
        setDate(journalEntry, previousDayTime)
    }

    fun moveToNextDay(journalEntry: JournalEntry) {
        val currentEntryTime = journalEntry.entryTimeOverride ?: journalEntry.entryTime
        val nextDayTime = currentEntryTime.plusSeconds(1.days.toJavaDuration().seconds)
        setDate(journalEntry, nextDayTime)
    }

    // Move it down in the list of entries that are ordered by ascending of display time
    fun moveDown(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == tagGroup.entries.lastIndex) {
            return
        }
        val nextEntry = tagGroup.entries[indexOfEntry + 1]
        val newDateTime = (nextEntry.entryTimeOverride ?: nextEntry.entryTime).plusSeconds(1)
        setDate(journalEntry, newDateTime)
    }

    // Move it up in the list of entries that are ordered by ascending of display time
    fun moveUp(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == 0) {
            return
        }
        val previousEntry = tagGroup.entries[indexOfEntry - 1]
        val newDateTime = (previousEntry.entryTimeOverride ?: previousEntry.entryTime).minusSeconds(1)
        setDate(journalEntry, newDateTime)
    }

    fun moveToPreviousDay(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                val currentEntryTime = journalEntry.entryTimeOverride ?: journalEntry.entryTime
                val previousDayTime = currentEntryTime.minusSeconds(1.days.toJavaDuration().seconds)
                repository.editEntryTime(journalEntry.id, previousDayTime)
            }
        }
    }

    fun moveToNextDay(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                val currentEntryTime = journalEntry.entryTimeOverride ?: journalEntry.entryTime
                val nextDayTime = currentEntryTime.plusSeconds(1.days.toJavaDuration().seconds)
                repository.editEntryTime(journalEntry.id, nextDayTime)
            }
        }
    }

    private fun setDate(journalEntry: JournalEntry, entryTime: Instant) {
        viewModelScope.launch {
            repository.editEntryTime(journalEntry.id, entryTime)
        }
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    private fun restartCollection() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            repository.getFlow().collect { entries ->
                val tags = tagsDao.getAll()
                val dayGroups = try {
                    entries.toDayGroups(
                        zoneId = zoneId,
                        tagsForSort = tags,
                    )
                } catch (e: Exception) {
                    listOf()
                }
                _state.update { previousState ->
                    val sorted = when (getSortOrder()) {
                        SortOrder.ASC -> dayGroups.sortedBy { it.date }
                        SortOrder.DESC -> dayGroups.sortedByDescending { it.date }
                    }
                    previousState.copy(dayGroups = sorted, tags = tags)
                }
            }
        }
    }

    companion object {
        fun factory(activity: Activity?) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val receivedText = activity?.intent?.receivedText()
                return JournalEntryViewModel(
                    receivedText,
                    ServiceLocator.keyValueStore,
                    ServiceLocator.repository,
                    ServiceLocator.tagsDao,
                ) as T
            }
        }
    }
}

data class ViewState(
    val dayGroups: List<DayGroup> = listOf(),
    val tags: List<Tag>,
    val loading: Boolean = false,
)