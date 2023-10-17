package com.ramitsuri.notificationjournal.ui.journalentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

class JournalEntryViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private var collectionJob: Job? = null

    private val _state = MutableStateFlow(
        ViewState(
            dayGroups = listOf(),
            loading = false,
        )
    )
    val state: StateFlow<ViewState> = _state

    init {
        restartCollection()
    }

    fun delete(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.delete(journalEntry)
        }
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    private fun restartCollection() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            repository.getFlow(getSortOrder()).collect { entries ->
                val dayGroups = entries
                    .groupBy { getLocalDate(it.entryTime, zoneId) }
                    .map { (date, entriesByDate) ->
                        val byTag = entriesByDate
                            .groupBy { it.tag }
                            .map { (tag, entriesByTag) ->
                                TagGroup(tag, entriesByTag)
                            }
                        DayGroup(date, byTag)
                    }
                _state.update {
                    it.copy(dayGroups = dayGroups)
                }
            }
        }
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return JournalEntryViewModel(
                    ServiceLocator.keyValueStore,
                    ServiceLocator.repository,
                ) as T
            }
        }
    }
}

data class ViewState(
    val dayGroups: List<DayGroup> = listOf(),
    val loading: Boolean = false,
)