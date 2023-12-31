package com.ramitsuri.notificationjournal.ui.addjournal

import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.loadTitle
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

class AddJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val templatesDao: JournalEntryTemplateDao,
    private val loadTitle: (String, String?) -> String?,
) : ViewModel() {
    private var processUrlJob: Job? = null

    private val receivedText: String? =
        if (savedStateHandle.get<String?>(RECEIVED_TEXT_ARG).isNullOrEmpty()) {
            null
        } else {
            URLDecoder.decode(savedStateHandle[RECEIVED_TEXT_ARG], "UTF-8")
        }

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<AddJournalEntryViewState> = MutableStateFlow(
        AddJournalEntryViewState.default(receivedText = receivedText)
    )
    val state: StateFlow<AddJournalEntryViewState> = _state

    init {
        loadAdditionalDataIfUrl(receivedText)
        loadTags()
        loadTemplates()
    }

    fun textUpdated(text: String) {
        _state.update {
            it.copy(text = text)
        }
        loadAdditionalDataIfUrl(text)
    }

    fun tagClicked(tag: String) {
        _state.update {
            it.copy(selectedTag = tag)
        }
    }

    fun useSuggestedText() {
        val suggestedText = _state.value.suggestedText
        if (suggestedText != null) {
            _state.update {
                it.copy(text = suggestedText, suggestedText = null)
            }
        }
    }

    fun templateClicked(template: JournalEntryTemplate) {
        _state.update {
            it.copy(text = template.text, selectedTag = template.tag)
        }
        save(exitOnSave = true)
    }

    fun save() {
        save(exitOnSave = true)
    }

    fun saveAndAddAnother() {
        save(exitOnSave = false)
    }

    private fun save(exitOnSave: Boolean) {
        val currentState = _state.value
        val text = currentState.text
        if (text.isEmpty()) {
            return
        }
        _state.update { it.copy(isLoading = true) }
        val tag = currentState.selectedTag
        viewModelScope.launch {
            repository.insert(
                text = text,
                tag = tag,
            )
            if (exitOnSave) {
                _saved.update {
                    true
                }
            } else {
                _state.update {
                    it.copy(isLoading = false, text = "", selectedTag = null, suggestedText = null)
                }
            }
        }
    }

    private fun loadAdditionalDataIfUrl(text: String?) {
        Log.d(TAG, "Canceling existing process url job")
        processUrlJob?.cancel()
        processUrlJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            val pageTitle = loadTitle(TAG, text)
            _state.update {
                it.copy(suggestedText = pageTitle)
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            _state.update {
                it.copy(tags = tagsDao.getAll())
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _state.update {
                it.copy(templates = templatesDao.getAll())
            }
        }
    }

    companion object {
        private const val TAG = "AddJournalEntryViewModel"

        const val RECEIVED_TEXT_ARG = "received_text"

        fun factory(navBackStackEntry: NavBackStackEntry) =
            object : AbstractSavedStateViewModelFactory(
                owner = navBackStackEntry,
                defaultArgs = navBackStackEntry.arguments,
            ) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    return AddJournalEntryViewModel(
                        savedStateHandle = handle,
                        repository = ServiceLocator.repository,
                        tagsDao = ServiceLocator.tagsDao,
                        templatesDao = ServiceLocator.templatesDao,
                        loadTitle = ::loadTitle
                    ) as T
                }
            }
    }
}

data class AddJournalEntryViewState(
    val isLoading: Boolean,
    val text: String,
    val tags: List<Tag>,
    val selectedTag: String?,
    val suggestedText: String?,
    val templates: List<JournalEntryTemplate>,
) {
    companion object {
        fun default(receivedText: String?) = AddJournalEntryViewState(
            isLoading = false,
            text = receivedText ?: "",
            tags = listOf(),
            selectedTag = null,
            suggestedText = null,
            templates = listOf(),
        )
    }
}