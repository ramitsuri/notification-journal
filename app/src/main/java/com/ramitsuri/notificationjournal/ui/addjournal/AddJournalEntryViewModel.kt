package com.ramitsuri.notificationjournal.ui.addjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.loadTitle
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddJournalEntryViewModel(
    private val repository: JournalRepository,
    receivedText: String?,
    private val loadTitle: (String, String?) -> String?,
) : ViewModel() {
    private val _state: MutableStateFlow<AddJournalEntryViewState> =
        MutableStateFlow(AddJournalEntryViewState.default(receivedText = receivedText))
    val state: StateFlow<AddJournalEntryViewState> = _state

    fun textUpdated(text: String) {
        _state.update {
            it.copy(text = text)
        }
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

    fun save() {
        val text = _state.value.text
        if (text.isEmpty()) {
            return
        }
        viewModelScope.launch {
            repository.insert(
                text = text
            )
        }
    }

    private fun loadAdditionalDataIfUrl(receivedText: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val pageTitle = loadTitle(TAG, receivedText)
            if (!pageTitle.isNullOrEmpty()) {
                _state.update {
                    it.copy(suggestedText = pageTitle)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AddJournalEntryViewModel"

        fun factory(receivedText: String?) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddJournalEntryViewModel(
                    repository = ServiceLocator.repository,
                    receivedText = receivedText,
                    loadTitle = ::loadTitle
                ) as T
            }
        }
    }
}

data class AddJournalEntryViewState(
    val text: String,
    val tags: List<Tag>,
    val selectedTag: String?,
    val suggestedText: String?,
) {
    companion object {
        fun default(receivedText: String?) = AddJournalEntryViewState(
            text = receivedText ?: "",
            tags = listOf(),
            selectedTag = null,
            suggestedText = null
        )
    }
}