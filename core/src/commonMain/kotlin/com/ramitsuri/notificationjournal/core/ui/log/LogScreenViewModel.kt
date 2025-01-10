package com.ramitsuri.notificationjournal.core.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.log.InMemoryLogWriter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.reflect.KClass

class LogScreenViewModel(
    private val inMemoryLogWriter: InMemoryLogWriter
) : ViewModel() {
    val logs = inMemoryLogWriter
        .logs
        .map { logs ->
            logs.sortedByDescending { log ->
                log.time
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearLogsClicked() {
        inMemoryLogWriter.clear()
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                return LogScreenViewModel(
                    ServiceLocator.inMemoryLogWriter,
                ) as T
            }
        }
    }
}
