package com.ramitsuri.notificationjournal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.data.JournalEntry
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class MainViewModel(
    private val dao: JournalEntryDao,
    private val dataSharingClient: DataSharingClient,
    private val longLivingCoroutineScope: CoroutineScope
) : ViewModel() {

    class Factory(
        private val dao: JournalEntryDao,
        private val dataSharingClient: DataSharingClient,
        private val coroutineScope: CoroutineScope
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(dao, dataSharingClient, coroutineScope) as T
        }
    }

    private val _state = MutableStateFlow(
        ViewState(
            journalEntries = listOf()
        )
    )
    val state: StateFlow<ViewState> = _state

    init {
        viewModelScope.launch {
            dao.getAllFlow().collect { entries ->
                _state.update {
                    it.copy(journalEntries = entries)
                }
            }
        }
    }

    fun add(value: String) {
        longLivingCoroutineScope.launch {
            val time = Instant.now()
            val timeZone = ZoneId.systemDefault()
            val posted = dataSharingClient.postJournalEntry(value, time, timeZone)
            if (posted) { // Shared with phone client, so no need to save to local db
                return@launch
            }
            val entry = JournalEntry(
                id = 0,
                entryTime = time,
                timeZone = timeZone,
                text = value
            )
            dao.insert(entry)
        }
    }

    fun sync() {
        longLivingCoroutineScope.launch {
            val onDeviceEntries = dao.getAll()
            onDeviceEntries.forEach { journalEntry ->
                val posted = dataSharingClient.postJournalEntry(
                    journalEntry.text,
                    journalEntry.entryTime,
                    journalEntry.timeZone
                )
                if (posted) {
                    dao.delete(listOf(journalEntry))
                }
            }
        }
    }
}

data class ViewState(
    val journalEntries: List<JournalEntry> = listOf()
)