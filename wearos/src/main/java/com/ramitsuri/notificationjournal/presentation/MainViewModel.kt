package com.ramitsuri.notificationjournal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class MainViewModel(
    private val dao: JournalEntryDao,
    private val templateDao: JournalEntryTemplateDao,
    private val dataSharingClient: DataSharingClient,
    private val longLivingCoroutineScope: CoroutineScope
) : ViewModel() {

    class Factory(
        private val dao: JournalEntryDao,
        private val templateDao: JournalEntryTemplateDao,
        private val dataSharingClient: DataSharingClient,
        private val coroutineScope: CoroutineScope
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(dao, templateDao, dataSharingClient, coroutineScope) as T
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
        viewModelScope.launch {
            templateDao.getAllFlow().collect { templates ->
                _state.update {
                    it.copy(journalEntryTemplates = templates)
                }
            }
        }
    }

    fun addFromTemplate(templateId: Int) {
        val entry = _state.value.journalEntryTemplates.firstOrNull { it.id == templateId } ?: return
        add(entry.text, entry.tag)
    }

    fun add(value: String, tag: String? = null) {
        val entries = value.split(". ")
        longLivingCoroutineScope.launch {
            val time = Instant.now()
            val timeZone = ZoneId.systemDefault()
            for (entry in entries) {
                postToClient(value = entry, time, timeZone, tag)
            }
        }
    }

    fun transferLocallySaved() {
        longLivingCoroutineScope.launch {
            val onDeviceEntries = dao.getAll()
            onDeviceEntries.forEach { journalEntry ->
                val posted = dataSharingClient.postJournalEntry(
                    journalEntry.text,
                    journalEntry.entryTime,
                    journalEntry.timeZone,
                    journalEntry.tag
                )
                if (posted) {
                    dao.delete(listOf(journalEntry))
                }
            }
        }
    }

    fun triggerUpload() {
        longLivingCoroutineScope.launch {
            dataSharingClient.requestUpload()
        }
    }

    private suspend fun postToClient(value: String, time: Instant, timeZone: ZoneId, tag: String?) {
        val posted = dataSharingClient.postJournalEntry(value, time, timeZone, tag)
        if (posted) { // Shared with phone client, so no need to save to local db
            return
        }
        val entry = JournalEntry(
            id = 0,
            entryTime = time,
            timeZone = timeZone,
            text = value,
            tag = null,
            entryTimeOverride = null,
        )
        dao.insert(entry)
    }
}

data class ViewState(
    val journalEntries: List<JournalEntry> = listOf(),
    val journalEntryTemplates: List<JournalEntryTemplate> = listOf(),
)