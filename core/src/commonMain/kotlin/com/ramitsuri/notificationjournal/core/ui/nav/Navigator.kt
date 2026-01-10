package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class Navigator(
    repository: JournalRepository,
    private val scope: CoroutineScope,
    topOfBackStack: Route? = null,
    home: Route = Route.JournalEntryDays,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val _backstack = NavBackStack(setOfNotNull(home, topOfBackStack).toMutableStateList())
    val backstack: List<Route>
        get() = _backstack

    private val dates =
        repository
            .getNotReconciledDatesFlow()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList(),
            )

    private var initialDateSelected = false
    private val currentDestination: Route
        get() = _backstack.last()

    val currentSelectedDate: LocalDate?
        get() = (_backstack.lastOrNull() as? Route.JournalEntry)?.selectedDate

    init {
        scope.launch {
            dates.collect { dates ->
                if (dates.isEmpty()) {
                    _backstack.removeIf { it is Route.JournalEntry }
                } else {
                    if (currentSelectedDate != null) {
                        if (currentSelectedDate !in dates) {
                            // Useful when a date is reconciled and is no longer going to be visible,
                            // then select the next one
                            selectNextDate()
                        }
                    } else if (!initialDateSelected &&
                        (currentDestination is Route.JournalEntryDays || currentDestination is Route.JournalEntry)
                    ) {
                        log { "Selecting initial date" }
                        initialDateSelected = true
                        selectDate(dates.todayIfExistsOrLast())
                    }
                }
            }
        }
    }

    fun navigate(route: Route): Boolean {
        log { "Navigation to $route requested" }
        if (_backstack.last() == route) {
            log { "Already on $route" }
            return false
        }
        val index = _backstack.indexOf(route)
        if (_backstack.last()::class == route::class) {
            _backstack.removeLastOrNull()
            _backstack.add(route)
        } else if (index == -1) {
            // Doesn't exist in backstack, add it
            _backstack.add(route)
        } else {
            // Entry exists in backstack, remove all entries after it. Essentially, pop back to excluding
            val newBackstack = _backstack.take(index + 1)
            _backstack.clear()
            _backstack.addAll(newBackstack)
        }
        return true
    }

    fun goBack() {
        if (_backstack.size <= 1) {
            return
        }
        _backstack.removeLastOrNull()
    }

    fun selectNextDate() {
        val currentDate = currentSelectedDate ?: return
        val dates = dates.value
        val currentDateIndex = dates.indexOf(currentDate)
        val newDate =
            if (currentDateIndex == -1) {
                dates.firstOrNull()
            } else {
                val newIndex = currentDateIndex + 1
                if (newIndex > dates.lastIndex) {
                    dates.firstOrNull()
                } else {
                    dates.getOrNull(newIndex)
                }
            } ?: return
        selectDate(newDate)
    }

    fun selectPrevious() {
        val currentDate = currentSelectedDate ?: return
        val dates = dates.value
        val currentDateIndex = dates.indexOf(currentDate)
        val newDate =
            if (currentDateIndex == -1) {
                dates.lastOrNull()
            } else {
                val newIndex = currentDateIndex - 1
                if (newIndex < 0) {
                    dates.lastOrNull()
                } else {
                    dates.getOrNull(newIndex)
                }
            } ?: return
        selectDate(newDate)
    }

    fun selectDate(localDate: LocalDate?) {
        log { "Select date: $localDate requested" }
        if (localDate == null) {
            if (_backstack.last() is Route.JournalEntry) {
                goBack()
            }
            return
        }
        val dates = dates.value
        val indexOfDate = dates.indexOf(localDate)
        if (indexOfDate == -1) {
            return
        }
        navigate(Route.JournalEntry(localDate))
    }

    private fun List<LocalDate>.todayIfExistsOrLast(): LocalDate? {
        val today = clock.todayIn(timeZone)
        return if (today in this) {
            today
        } else {
            lastOrNull()
        }
    }

    private fun log(message: () -> String) {
        Logger.i("Navigator") {
            message()
        }
    }
}
