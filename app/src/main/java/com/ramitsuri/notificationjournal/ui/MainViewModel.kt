package com.ramitsuri.notificationjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.data.JournalEntryDao
import com.ramitsuri.notificationjournal.network.Api
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val entries = journalEntryDao.getAll()
                _state.update {
                    it.copy(journalEntries = entries)
                }
            }
        }
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
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                journalEntryDao.delete(listOf(journalEntry))
                _state.update {
                    it.copy(journalEntries = journalEntryDao.getAll())
                }
            }
        }
    }

    fun setApiUrl(url: String) {
        keyValueStore.putString(Constants.PREF_KEY_API_URL, url)
        _state.update {
            it.copy(serverText = url)
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