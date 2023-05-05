package com.ramitsuri.notificationjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.repository.JournalRepository
import com.ramitsuri.notificationjournal.model.SortOrder
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository
) : ViewModel() {

    class Factory(
        private val keyValueStore: KeyValueStore,
        private val repository: JournalRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(keyValueStore, repository) as T
        }
    }

    private val _state = MutableStateFlow(
        ViewState(
            journalEntries = listOf(),
            loading = false,
            serverText = getApiUrl()
        )
    )
    val state: StateFlow<ViewState> = _state

    init {
        getAll()
    }

    fun getAll() {
        runOperationAndRefresh { }
    }

    fun upload() {
        _state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val error = repository.upload()
                _state.update {
                    if (error != null) {
                        it.copy(loading = false, error = error)
                    } else {
                        it.copy(loading = false, journalEntries = repository.get(getSortOrder()))
                    }
                }
            }
        }
    }

    fun delete(journalEntry: JournalEntry) {
        runOperationAndRefresh {
            repository.delete(journalEntry)
        }
    }

    fun add(text: String) {
        runOperationAndRefresh {
            repository.insert(
                text = text
            )
        }
    }

    fun edit(id: Int, text: String) {
        runOperationAndRefresh {
            repository.edit(id, text)
        }
    }

    fun setApiUrl(url: String) {
        keyValueStore.putString(Constants.PREF_KEY_API_URL, url)
        _state.update {
            it.copy(serverText = url)
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
        runOperationAndRefresh {  }
    }

    private fun setSortOrder(sortOrder: SortOrder) {
        keyValueStore.putInt(Constants.PREF_KEY_SORT_ORDER, sortOrder.key)
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    fun onErrorAcknowledged() {
        _state.update {
            it.copy(error = null)
        }
    }

    private fun runOperationAndRefresh(operation: suspend () -> Unit) {
        viewModelScope.launch {
            operation()
            _state.update {
                it.copy(journalEntries = repository.get(getSortOrder()))
            }
        }
    }

    private fun getApiUrl() = keyValueStore.getString(Constants.PREF_KEY_API_URL, "") ?: ""
}

data class ViewState(
    val journalEntries: List<JournalEntry> = listOf(),
    val serverText: String = "",
    val loading: Boolean = false,
    val error: String? = null
)