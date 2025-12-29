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
import kotlinx.datetime.LocalDate

class DateSelector(
    private val lifecycleOwner: LifecycleOwner,
    repository: JournalRepository,
) : DefaultLifecycleObserver {
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate = _selectedDate.asStateFlow()

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

    fun selectNextDate() {
        _selectedDate.update { currentDate ->
            if (currentDate == null) {
                return
            }
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
            newDate
        }
    }

    fun selectPreviousDate() {
        _selectedDate.update { currentDate ->
            if (currentDate == null) {
                return
            }
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
            newDate
        }
    }

    fun selectDate(localDate: LocalDate?) {
        if (localDate == null) {
            _selectedDate.update { null }
            return
        }
        val indexOfDate = dates.value.indexOf(localDate)
        if (indexOfDate == -1) {
            return
        }
        _selectedDate.update {
            localDate
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        collectJob?.cancel()
        collectJob =
            lifecycleOwner.lifecycleScope.launch {
                dates.collect { dates ->
                    if (dates.isEmpty()) {
                        selectDate(null)
                    } else if (selectedDate.value !in dates) {
                        // Useful when a date is reconciled and is no longer going to be visible, then select the next one
                        selectNextDate()
                    }
                }
            }
    }

    override fun onPause(owner: LifecycleOwner) {
        collectJob?.cancel()
        collectJob = null
    }
}
