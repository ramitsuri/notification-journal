package com.ramitsuri.notificationjournal.core.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class DateSelector(
    private val lifecycleOwner: LifecycleOwner,
    repository: JournalRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : DefaultLifecycleObserver {
    private val _state = MutableStateFlow<DateSelectorState>(DateSelectorState.Initial)
    val state = _state.asStateFlow()

    private val dates =
        repository
            .getNotReconciledDatesFlow()
            .stateIn(
                scope = lifecycleOwner.lifecycleScope,
                started = WhileSubscribed(),
                initialValue = emptyList(),
            )

    private var collectJob: Job? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun selectNext() {
        _state.update { existingState ->
            val currentDate = (existingState as? DateSelectorState.Date)?.date ?: return
            val currentDateIndex = dates.value.indexOf(currentDate)
            val newDate =
                if (currentDateIndex == -1) {
                    dates.value.firstOrNull()
                } else {
                    val newIndex = currentDateIndex + 1
                    if (newIndex > dates.value.lastIndex) {
                        dates.value.firstOrNull()
                    } else {
                        dates.value.getOrNull(newIndex)
                    }
                } ?: return
            DateSelectorState.Date(newDate)
        }
    }

    fun selectPrevious() {
        _state.update { existingState ->
            val currentDate = (existingState as? DateSelectorState.Date)?.date ?: return
            val currentDateIndex = dates.value.indexOf(currentDate)
            val newDate =
                if (currentDateIndex == -1) {
                    dates.value.lastOrNull()
                } else {
                    val newIndex = currentDateIndex - 1
                    if (newIndex < 0) {
                        dates.value.lastOrNull()
                    } else {
                        dates.value.getOrNull(newIndex)
                    }
                } ?: return
            DateSelectorState.Date(newDate)
        }
    }

    fun select(localDate: LocalDate?) {
        if (localDate == null) {
            _state.update { DateSelectorState.None }
            return
        }
        val indexOfDate = dates.value.indexOf(localDate)
        if (indexOfDate == -1) {
            return
        }
        _state.update {
            DateSelectorState.Date(localDate)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        collectJob?.cancel()
        collectJob =
            lifecycleOwner.lifecycleScope.launch {
                dates.collect { dates ->
                    if (dates.isEmpty()) {
                        if (_state.value !is DateSelectorState.Initial) {
                            _state.update { DateSelectorState.None }
                        }
                    } else {
                        val state = state.value
                        if (state is DateSelectorState.Date) {
                            if (state.date !in dates) {
                                // Useful when a date is reconciled and is no longer going to be visible,
                                // then select the next one
                                selectNext()
                            }
                        } else if (state is DateSelectorState.Initial) {
                            select(dates.todayIfExistsOrLast())
                        }
                    }
                }
            }
    }

    override fun onPause(owner: LifecycleOwner) {
        collectJob?.cancel()
        collectJob = null
    }

    private fun List<LocalDate>.todayIfExistsOrLast(): LocalDate? {
        val today = clock.todayIn(timeZone)
        return if (today in this) {
            today
        } else {
            lastOrNull()
        }
    }

    sealed interface DateSelectorState {
        data class Date(val date: LocalDate) : DateSelectorState

        data object Initial : DateSelectorState

        data object None : DateSelectorState
    }
}
