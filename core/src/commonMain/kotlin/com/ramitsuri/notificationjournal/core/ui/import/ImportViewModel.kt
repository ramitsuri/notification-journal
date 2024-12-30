package com.ramitsuri.notificationjournal.core.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.reflect.KClass

class ImportViewModel(
    private val importRepository: ImportRepository,
    private val journalRepository: JournalRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val days = mutableSetOf<LocalDate>()
            importRepository
                .importedEntriesFlow
                .collect { entries ->
                    days.addAll(entries.map { it.entryTime.date })
                    journalRepository.insert(entries)
                    _state.update {
                        val existingCount = (it.importStatus as? ImportStatus.InProgress)
                            ?.importCount ?: 0
                        val newCount = existingCount + entries.size
                        it.copy(importStatus = ImportStatus.InProgress(newCount))
                    }
                }
            _state.update {
                val entriesCount = (it.importStatus as? ImportStatus.InProgress)
                    ?.importCount ?: 0
                it.copy(
                    importStatus = ImportStatus.Completed(
                        importCount = entriesCount,
                        daysCount = days.size,
                    )
                )
            }
        }
    }

    fun onImportClicked() {
        val state = _state.value
        val startDate = state.startDate ?: return
        val endDate = state.endDate ?: return

        viewModelScope.launch {
            importRepository.import(
                fromDir = state.fromDir,
                startDate = startDate,
                endDate = endDate,
            )
        }
    }

    fun onFromDirChanged(dir: String) {
        _state.update { it.copy(fromDir = dir) }
    }

    fun onStartDateChanged(date: LocalDate) {
        _state.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: LocalDate) {
        _state.update { it.copy(endDate = date) }
    }

    fun onResetStartDate() {
        _state.update { it.copy(startDate = null) }
    }

    fun onResetEndDate() {
        _state.update { it.copy(endDate = null) }
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                return ImportViewModel(
                    importRepository = ServiceLocator.importRepository,
                    journalRepository = ServiceLocator.repository,
                ) as T
            }
        }
    }
}

data class ViewState(
    val fromDir: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val importStatus: ImportStatus = ImportStatus.NotStarted,
) {
    val isImportEnabled: Boolean
        get() = fromDir.isNotBlank() &&
                startDate != null &&
                endDate != null &&
                endDate >= startDate

    val allowedStartDateSelections: ClosedRange<LocalDate>
        get() = Constants.LocalDate.IMPORT_MIN..(endDate ?: Clock.System.nowLocal().date)

    val allowedEndDateSelections: ClosedRange<LocalDate>
        get() = (startDate ?: Constants.LocalDate.IMPORT_MIN)..Clock.System.nowLocal().date
}

sealed interface ImportStatus {
    data object NotStarted : ImportStatus
    data class InProgress(val importCount: Int) : ImportStatus
    data class Completed(val importCount: Int, val daysCount: Int) : ImportStatus
}
