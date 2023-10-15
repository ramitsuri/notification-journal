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
import com.ramitsuri.notificationjournal.core.utils.loadTitle
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class JournalEntryViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    receivedText: String?,
    private val loadTitle: (String, String?) -> String?,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private var collectionJob: Job? = null

    private val _state = MutableStateFlow(
        ViewState(
            receivedText = receivedText,
            dayGroups = listOf(),
            loading = false,
        )
    )
    val state: StateFlow<ViewState> = _state

    init {
        restartCollection()
        loadAdditionalDataIfUrl()
    }

    fun delete(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.delete(journalEntry)
        }
    }

    fun add(text: String) {
        viewModelScope.launch {
            repository.insert(
                text = text
            )
        }
    }

    fun edit(id: Int, text: String) {
        viewModelScope.launch {
            repository.editText(id, text)
        }
    }

    fun editTag(id: Int, tag: String?) {
        viewModelScope.launch {
            repository.editTag(id, tag)
        }
    }

    fun editTime(id: Int, time: Instant?) {
        viewModelScope.launch {
            repository.editEntryTime(id, time)
        }
    }

    fun resetReceivedText() {
        _state.update {
            it.copy(receivedText = null, suggestedTextFromReceivedText = null)
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

    private fun loadAdditionalDataIfUrl() {
        val url = _state.value.receivedText
        viewModelScope.launch(Dispatchers.IO) {
            val pageTitle = loadTitle(TAG, url)
            if (!pageTitle.isNullOrEmpty()) {
                _state.update {
                    it.copy(suggestedTextFromReceivedText = pageTitle)
                }
            }
        }
    }

    companion object {
        private const val TAG = "PhoneViewModel"

        fun factory(receivedText: String?) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return JournalEntryViewModel(
                    ServiceLocator.keyValueStore,
                    ServiceLocator.repository,
                    receivedText,
                    ::loadTitle
                ) as T
            }
        }
    }
}

data class ViewState(
    val dayGroups: List<DayGroup> = listOf(),
    val receivedText: String? = null,
    val suggestedTextFromReceivedText: String? = null,
    val loading: Boolean = false,
)