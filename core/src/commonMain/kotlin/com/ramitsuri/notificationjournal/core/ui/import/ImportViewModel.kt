package com.ramitsuri.notificationjournal.core.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.asImportFileName
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass

class ImportViewModel(
    private val importRepository: ImportRepository,
    private val journalRepository: JournalRepository,
    private val prefManager: PrefManager,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            ViewState(
                startDate = defaultStartDate,
                endDate = defaultEndDate,
                lastImportDate = defaultLastImportDate,
            ),
        )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                _state.update {
                    it.copy(
                        fromDir = prefManager.getLastImportDir().first(),
                        lastImportDate =
                            prefManager
                                .getLastImportDate()
                                .first()
                                ?.fromInstant()
                                ?: defaultLastImportDate,
                    )
                }
            }
            val days = mutableSetOf<LocalDate>()
            importRepository
                .importedEntriesFlow
                .collect { entries ->
                    val modifiedDays = entries.map { it.entryTime.date }
                    days.addAll(modifiedDays)
                    journalRepository.clearDaysAndInsert(modifiedDays, entries)
                    _state.update {
                        val existingCount =
                            (it.importStatus as? ImportStatus.InProgress)
                                ?.importCount ?: 0
                        val newCount = existingCount + entries.size
                        it.copy(importStatus = ImportStatus.InProgress(newCount))
                    }
                }
            // This is okay because the collect call suspends until all of the import is done and
            // then updates the status to completed.
            _state.update {
                val entriesCount =
                    (it.importStatus as? ImportStatus.InProgress)
                        ?.importCount ?: 0
                it.copy(
                    importStatus =
                        ImportStatus.Completed(
                            importCount = entriesCount,
                            daysCount = days.size,
                        ),
                )
            }
            prefManager.setLastImportDate(clock.now())
            prefManager.setLastImportDir(_state.value.fromDir)
        }
    }

    fun onImportClicked() {
        val state = _state.value
        val startDate = state.startDate
        val endDate = state.endDate

        viewModelScope.launch {
            importRepository.import(
                fromDir = state.fromDir,
                startDate = startDate,
                endDate = endDate,
                lastImportDate =
                    if (state.useLastImportTime) {
                        state.lastImportDate.toInstant()
                    } else {
                        Instant.DISTANT_PAST
                    },
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
        _state.update { it.copy(startDate = defaultStartDate) }
    }

    fun onResetEndDate() {
        _state.update { it.copy(endDate = defaultEndDate) }
    }

    fun toggleUseLastImportTime() {
        _state.update { it.copy(useLastImportTime = !it.useLastImportTime) }
    }

    fun onLastImportDateChanged(date: LocalDate) {
        _state.update { it.copy(lastImportDate = date) }
    }

    fun resetLastImportDate() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    lastImportDate =
                        prefManager
                            .getLastImportDate()
                            .first()
                            ?.fromInstant()
                            ?: defaultLastImportDate,
                )
            }
        }
    }

    private fun Instant.fromInstant(): LocalDate {
        return toLocalDateTime(timeZone).date
    }

    private fun LocalDate.toInstant(): Instant {
        return atStartOfDayIn(timeZone)
    }

    companion object {
        private val defaultStartDate
            get() = Constants.LocalDate.IMPORT_MIN
        private val defaultEndDate
            get() = Clock.System.nowLocal().date
        private val defaultLastImportDate
            get() = LocalDate(year = 2000, monthNumber = 1, dayOfMonth = 1)

        fun factory() =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return ImportViewModel(
                        importRepository = ServiceLocator.importRepository,
                        journalRepository = ServiceLocator.repository,
                        prefManager = ServiceLocator.prefManager,
                    ) as T
                }
            }
    }
}

data class ViewState(
    val fromDir: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate,
    val importStatus: ImportStatus = ImportStatus.NotStarted,
    val lastImportDate: LocalDate,
    val useLastImportTime: Boolean = true,
) {
    val isImportEnabled: Boolean
        get() =
            fromDir.isNotBlank() &&
                endDate >= startDate

    val allowedStartDateSelections: ClosedRange<LocalDate>
        get() = Constants.LocalDate.IMPORT_MIN..endDate

    val allowedEndDateSelections: ClosedRange<LocalDate>
        get() = startDate..Clock.System.nowLocal().date

    val startDateFormattedAsFile: String
        get() = startDate.asImportFileName()

    val endDateFormattedAsFile: String
        get() = endDate.asImportFileName()
}

sealed interface ImportStatus {
    data object NotStarted : ImportStatus

    data class InProgress(val importCount: Int) : ImportStatus

    data class Completed(val importCount: Int, val daysCount: Int) : ImportStatus
}
