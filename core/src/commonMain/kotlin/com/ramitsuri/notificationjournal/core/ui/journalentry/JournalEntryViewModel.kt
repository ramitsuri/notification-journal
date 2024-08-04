package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class JournalEntryViewModel(
    receivedText: String?,
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val zoneId: TimeZone = TimeZone.currentSystemDefault(),
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
            repository.update(journalEntry.copy(tag = tag))
        }
    }

    fun moveToPreviousDay(journalEntry: JournalEntry) {
        val currentEntryTime = journalEntry.entryTime
        val previousDayTime = currentEntryTime.minus(1.days)
        setDate(journalEntry, previousDayTime)
    }

    fun moveToNextDay(journalEntry: JournalEntry) {
        val currentEntryTime = journalEntry.entryTime
        val nextDayTime = currentEntryTime.plus(1.days)
        setDate(journalEntry, nextDayTime)
    }

    // Move it down in the list of entries that are ordered by ascending of display time
    fun moveDown(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == tagGroup.entries.lastIndex) {
            return
        }
        val nextEntry = tagGroup.entries[indexOfEntry + 1]
        val newDateTime = nextEntry.entryTime.plus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it up in the list of entries that are ordered by ascending of display time
    fun moveUp(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == 0) {
            return
        }
        val previousEntry = tagGroup.entries[indexOfEntry - 1]
        val newDateTime = previousEntry.entryTime.minus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    fun moveToPreviousDay(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                val currentEntryTime = journalEntry.entryTime
                val previousDayTime = currentEntryTime.minus(1.days)
                repository.update(journalEntry.copy(entryTime = previousDayTime))
            }
        }
    }

    fun moveToNextDay(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                val currentEntryTime = journalEntry.entryTime
                val nextDayTime = currentEntryTime.plus(1.days)
                repository.update(journalEntry.copy(entryTime = nextDayTime))
            }
        }
    }

    fun reconcile(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                repository.update(journalEntry.copy(reconciled = true))
            }
        }
    }

    fun forceUpload(tagGroup: TagGroup) {
        repository.upload(tagGroup.entries)
    }

    fun forceUpload(entry: JournalEntry) {
        repository.upload(listOf(entry))
    }

    fun sync() {
        viewModelScope.launch {
            repository.sync()
        }
    }

    private fun setDate(journalEntry: JournalEntry, entryTime: Instant) {
        viewModelScope.launch {
            repository.update(journalEntry.copy(entryTime = entryTime))
        }
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    private fun restartCollection() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            val showReconciled = keyValueStore.getBoolean(Constants.PREF_KEY_SHOW_RECONCILED, false)
            combine(
                repository.getFlow(showReconciled = showReconciled),
                repository.getForUploadCountFlow()
            ) { entries, forUploadCount ->
                Pair(entries, forUploadCount)
            }
                .collect { (entries, forUploadCount) ->
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
                        previousState.copy(
                            dayGroups = sorted,
                            tags = tags,
                            notUploadedCount = forUploadCount
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(receivedText: String?) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
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
    val notUploadedCount: Int = 0,
)