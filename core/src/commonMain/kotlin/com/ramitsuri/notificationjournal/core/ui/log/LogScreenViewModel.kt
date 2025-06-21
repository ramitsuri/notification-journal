package com.ramitsuri.notificationjournal.core.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.log.InMemoryLogWriter
import com.ramitsuri.notificationjournal.core.model.logs.LogsViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlin.reflect.KClass

class LogScreenViewModel(
    private val inMemoryLogWriter: InMemoryLogWriter,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val tagSelections: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    private var initialSelectionForTags = true
    val viewState =
        combine(
            inMemoryLogWriter.getTags(),
            tagSelections,
            inMemoryLogWriter.logs,
        ) { tags, selections, logs ->
            if (initialSelectionForTags && selections.isEmpty()) {
                tagSelections.update { tags }
            }
            LogsViewState(
                timeZone = timeZone,
                tags = tags.map { LogsViewState.Tag(it, it in selections) },
                logs = logs.filter { it.tag in selections },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogsViewState(timeZone = timeZone),
        )

    fun tagClicked(tag: String) {
        initialSelectionForTags = false
        tagSelections.update { currentSelections ->
            if (tag in currentSelections) {
                currentSelections - tag
            } else {
                currentSelections + tag
            }
        }
    }

    fun selectAllTagsClicked() {
        initialSelectionForTags = false
        tagSelections.update {
            viewState.value.tags.map { it.value }
        }
    }

    fun unselectAllTagsClicked() {
        initialSelectionForTags = false
        tagSelections.update { listOf() }
    }

    fun clearLogsClicked() {
        inMemoryLogWriter.clear()
    }

    companion object {
        fun factory() =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return LogScreenViewModel(
                        ServiceLocator.inMemoryLogWriter,
                    ) as T
                }
            }
    }
}
