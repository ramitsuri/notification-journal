package com.ramitsuri.notificationjournal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(
    private val repository: JournalRepository,
    private val templateDao: JournalEntryTemplateDao,
    private val wearDataSharingClient: WearDataSharingClient,
    private val longLivingCoroutineScope: CoroutineScope
) : ViewModel() {

    class Factory(
        private val repository: JournalRepository,
        private val templateDao: JournalEntryTemplateDao,
        private val wearDataSharingClient: WearDataSharingClient,
        private val coroutineScope: CoroutineScope
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                repository,
                templateDao,
                wearDataSharingClient,
                coroutineScope
            ) as T
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
            repository.getFlow().collect { entries ->
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

    fun addFromTemplate(templateId: String) {
        viewModelScope.launch {
            val entry = templateDao.getAll().firstOrNull { it.id == templateId } ?: return@launch
            add(entry.text, entry.tag, exitOnDone = true)
        }
    }

    fun add(
        value: String,
        tag: String? = null,
        exitOnDone: Boolean = false,
        time: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ) {
        longLivingCoroutineScope.launch {
            value.split(". ")
                .filter { it.isNotBlank() }
                .mapIndexed { index, entry ->
                    val entryTime = time.plus(index.times(10).milliseconds)
                    launch {
                        postToClient(
                            value = entry,
                            time = entryTime,
                            timeZone = timeZone,
                            tag = tag,
                        )
                    }
                }
                .joinAll()

            if (exitOnDone) {
                _state.update {
                    it.copy(shouldExit = true)
                }
            }
        }
    }

    fun transferLocallySaved() {
        longLivingCoroutineScope.launch {
            val onDeviceEntries = repository.getAll()
            onDeviceEntries.forEach { journalEntry ->
                val posted = wearDataSharingClient.postJournalEntry(
                    journalEntry.text,
                    journalEntry.entryTime,
                    journalEntry.timeZone,
                    journalEntry.tag
                )
                if (posted) {
                    repository.delete(journalEntry)
                }
            }
        }
    }

    fun triggerUpload() {
        longLivingCoroutineScope.launch {
            wearDataSharingClient.requestUpload()
        }
    }

    private suspend fun postToClient(
        value: String,
        time: Instant,
        timeZone: TimeZone,
        tag: String?,
    ) {
        val posted = wearDataSharingClient.postJournalEntry(value, time, timeZone, tag)
        if (posted) { // Shared with phone client, so no need to save to local db
            return
        }
        repository.insert(text = value, time = time, timeZone = timeZone)
    }
}

data class ViewState(
    val journalEntries: List<JournalEntry> = listOf(),
    val journalEntryTemplates: List<JournalEntryTemplate> = listOf(),
    val shouldExit: Boolean = false,
)