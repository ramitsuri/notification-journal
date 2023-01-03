package com.ramitsuri.notificationjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.data.JournalEntryDao
import com.ramitsuri.notificationjournal.data.JournalEntryUpdate
import com.ramitsuri.notificationjournal.network.Api
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class MainViewModel(
    private val journalEntryDao: JournalEntryDao,
    private val keyValueStore: KeyValueStore,
    private val api: Api
) : ViewModel() {

    class Factory(
        private val dao: JournalEntryDao,
        private val keyValueStore: KeyValueStore,
        private val api: Api
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(dao, keyValueStore, api) as T
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
        runOperationAndRefresh {  }
    }

    fun upload() {
        if (_state.value.journalEntries.isEmpty()) {
            return
        }
        _state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val error = try {
                    api.sendData(_state.value.journalEntries)
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    e.message
                }
                _state.update {
                    it.copy(loading = false, error = error)
                }
            }
        }
    }

    fun delete(journalEntry: JournalEntry) {
        runOperationAndRefresh {
            journalEntryDao.delete(listOf(journalEntry))
        }
    }

    fun delete() {
        runOperationAndRefresh {
            journalEntryDao.deleteAll()
        }
    }

    fun add(text: String) {
        runOperationAndRefresh {
            journalEntryDao.insert(
                JournalEntry(
                    id = 0,
                    entryTime = Instant.now(),
                    timeZone = ZoneId.systemDefault(),
                    text = text
                )
            )
        }
    }

    fun edit(id: Int, text: String) {
        runOperationAndRefresh {
            journalEntryDao.update(
                JournalEntryUpdate(
                    id = id,
                    text = text
                )
            )
        }
    }

    fun setApiUrl(url: String) {
        keyValueStore.putString(Constants.PREF_KEY_API_URL, url)
        _state.update {
            it.copy(serverText = url)
        }
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
                it.copy(journalEntries = journalEntryDao.getAll())
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