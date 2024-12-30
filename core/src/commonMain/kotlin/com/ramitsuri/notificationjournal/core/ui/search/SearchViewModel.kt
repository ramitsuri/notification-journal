package com.ramitsuri.notificationjournal.core.ui.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

class SearchViewModel(
    private val repository: JournalRepository,
) : ViewModel() {
    private val tagSelections: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    private val searchTextState = TextFieldState()

    private var initialSelectionForTags = true

    @OptIn(FlowPreview::class)
    val state = combine(
        repository.getEntryTags().map { tags -> tags.filter { !Tag.isNoTag(it) } },
        tagSelections,
        snapshotFlow { searchTextState.text }.debounce(300)
    ) { tags, selections, searchText ->
        if (initialSelectionForTags && selections.isEmpty()) {
            tagSelections.update { tags }
        }

        ViewState(
            searchTextState = searchTextState,
            tags = tags.map { ViewState.Tag(it, it in selections) },
            results = searchText.toString().takeIf { it.isNotEmpty() }
                ?.let { repository.search(it, selections) }
                ?: listOf()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ViewState(),
    )

    fun tagClicked(tag: String) {
        initialSelectionForTags = false
        val currentSelections = tagSelections.value
        tagSelections.update {
            if (tag in currentSelections) {
                currentSelections - tag
            } else {
                currentSelections + tag
            }
        }
    }

    fun clearSearchTerm() {
        searchTextState.clearText()
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                return SearchViewModel(
                    ServiceLocator.repository,
                ) as T
            }
        }
    }
}

data class ViewState(
    val searchTextState: TextFieldState = TextFieldState(),
    val tags: List<Tag> = listOf(),
    val results: List<JournalEntry> = listOf(),
) {
    data class Tag(val value: String, val selected: Boolean)
}