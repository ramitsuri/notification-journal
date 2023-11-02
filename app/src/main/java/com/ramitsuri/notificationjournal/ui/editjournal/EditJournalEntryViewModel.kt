package com.ramitsuri.notificationjournal.ui.editjournal

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditJournalEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _state: MutableStateFlow<EditJournalEntryViewState> =
        MutableStateFlow(EditJournalEntryViewState.default())
    val state: StateFlow<EditJournalEntryViewState> = _state

    private lateinit var entry: JournalEntry

    init {
        viewModelScope.launch {
            entry = repository.get(checkNotNull(savedStateHandle[ENTRY_ID_ARG]))
            _state.update {
                it.copy(isLoading = false, text = entry.text, selectedTag = entry.tag)
            }
        }
        loadTags()
    }

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

    fun save() {
        if (!::entry.isInitialized) {
            return
        }
        val currentState = _state.value
        val text = currentState.text
        if (text.isEmpty()) {
            return
        }
        _state.update { it.copy(isLoading = true) }
        val tag = currentState.selectedTag
        viewModelScope.launch {
            repository.editText(
                id = entry.id,
                text = text
            )
            repository.editTag(
                id = entry.id,
                tag = tag
            )
            _saved.update {
                true
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

    companion object {
        const val ENTRY_ID_ARG = "entry_id"

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
                    return EditJournalEntryViewModel(
                        savedStateHandle = handle,
                        repository = ServiceLocator.repository,
                        tagsDao = ServiceLocator.tagsDao,
                    ) as T
                }
            }
    }
}

data class EditJournalEntryViewState(
    val isLoading: Boolean,
    val text: String,
    val tags: List<Tag>,
    val selectedTag: String?,
) {
    companion object {
        fun default() = EditJournalEntryViewState(
            isLoading = true,
            text = "",
            tags = listOf(),
            selectedTag = null,
        )
    }
}