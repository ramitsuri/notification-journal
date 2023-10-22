package com.ramitsuri.notificationjournal.ui.journalentry

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.di.ServiceLocator
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
                val dayGroups = entries.toDayGroups(
                        zoneId = zoneId,
                        tagsForSort = tags,
                    )
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
                val intent = activity?.intent
                val receivedText =
                    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                        intent.getStringExtra(Intent.EXTRA_TEXT)
                    } else {
                        null
                    }

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