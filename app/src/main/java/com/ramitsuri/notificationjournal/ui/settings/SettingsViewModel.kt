package com.ramitsuri.notificationjournal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
) : ViewModel() {
    private val _state: MutableStateFlow<SettingsViewState>
    val state: StateFlow<SettingsViewState>

    private var collectionJob: Job? = null

    init {
        val serverUrl = getApiUrl()
        _state = MutableStateFlow(
            SettingsViewState(
                uploadLoading = false,
                sortOrder = getSortOrder(),
                serverText = serverUrl,
                error = null,
                serverState = if (serverUrl.isEmpty() || serverUrl == Constants.DEFAULT_API_URL) {
                    ServerState.SET_SERVER
                } else {
                    ServerState.SERVER_SET
                },
                sortStatus = ""
            )
        )
        state = _state
        restartCollection()
    }

    fun upload() {
        _state.update {
            it.copy(uploadLoading = true)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val error = repository.upload()
                _state.update {
                    it.copy(uploadLoading = false, error = error)
                }
            }
        }
    }

    fun setApiUrl(url: String) {
        keyValueStore.putString(Constants.PREF_KEY_API_URL, url)
        _state.update {
            it.copy(serverText = url, serverState = ServerState.RESTART)
        }
    }

    fun reverseSortOrder() {
        val currentSortOrder = getSortOrder()
        val newSortOrder = if (currentSortOrder == SortOrder.ASC) {
            SortOrder.DESC
        } else {
            SortOrder.ASC
        }
        setSortOrder(newSortOrder)
    }

    fun onErrorAcknowledged() {
        _state.update {
            it.copy(error = null)
        }
    }

    fun applySorting() {
        restartCollection(firstTime = false)
    }

    private fun restartCollection(firstTime: Boolean = true) {
        var sortByTagOrder =
            keyValueStore.getBoolean(Constants.PREF_SORT_BY_TAG_ORDER, true)
        var sortByEntryTime =
            keyValueStore.getBoolean(Constants.PREF_SORT_BY_ENTRY_TIME, true)
        if (!firstTime) {
            if (sortByEntryTime && sortByTagOrder) {
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_TAG_ORDER, false)
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_ENTRY_TIME, true)
            } else if (sortByEntryTime) {
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_TAG_ORDER, true)
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_ENTRY_TIME, false)
            } else if (sortByTagOrder) {
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_TAG_ORDER, false)
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_ENTRY_TIME, false)
            } else {
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_TAG_ORDER, true)
                keyValueStore.putBoolean(Constants.PREF_SORT_BY_ENTRY_TIME, true)
            }
        }
        sortByTagOrder = keyValueStore.getBoolean(Constants.PREF_SORT_BY_TAG_ORDER, true)
        sortByEntryTime = keyValueStore.getBoolean(Constants.PREF_SORT_BY_ENTRY_TIME, true)
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            repository.getFlow().collect { entries ->
                val tags = tagsDao.getAll()
                val dayGroups = try {
                    entries.toDayGroups(
                        tagsForSort = tags,
                        sortByEntryTime = sortByEntryTime,
                        sortByTagOrder = sortByTagOrder
                    )
                } catch (e: Exception) {
                    listOf()
                }
                _state.update { previousState ->
                    val newState =
                    previousState.copy(
                        sortStatus = "By Tag: ${sortByTagOrder}, " +
                                "By Entry Time: ${sortByEntryTime}, " +
                                "DayGroups: ${dayGroups.size}"
                    )
                    newState
                }
            }
        }
    }

    private fun setSortOrder(sortOrder: SortOrder) {
        keyValueStore.putInt(Constants.PREF_KEY_SORT_ORDER, sortOrder.key)
        _state.update {
            it.copy(sortOrder = sortOrder)
        }
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    private fun getApiUrl() = keyValueStore.getString(Constants.PREF_KEY_API_URL, "") ?: ""

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    keyValueStore = ServiceLocator.keyValueStore,
                    repository = ServiceLocator.repository,
                    tagsDao = ServiceLocator.tagsDao,
                ) as T
            }
        }
    }
}

data class SettingsViewState(
    val uploadLoading: Boolean,
    val sortOrder: SortOrder,
    val serverText: String,
    val serverState: ServerState,
    val error: String? = null,
    val sortStatus: String = "",
)

enum class ServerState {
    RESTART,
    SET_SERVER,
    SERVER_SET,
}