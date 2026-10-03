package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

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

    /**
     * Not reconciled dates, in the order they're shown. `null` until loaded for the first time.
     */
    val dates: StateFlow<List<LocalDate>?> =
        repository
            .getNotReconciledDatesFlow()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = null,
            )

    private var initialDateSelected = false
    private val currentDestination: Route
        get() = _backstack.last()

    /**
     * Date being shown by [Route.JournalEntry]. Owned here rather than in the route so that
     * switching days doesn't change the backstack. [Route.JournalEntry.selectedDate] is only
     * the date the route was opened with.
     */
    var selectedDate: LocalDate? by mutableStateOf((topOfBackStack as? Route.JournalEntry)?.selectedDate)
        private set

    /**
     * [selectedDate] but only while [Route.JournalEntry] is at the top of the backstack.
     */
    val currentSelectedDate: LocalDate?
        get() = selectedDate.takeIf { _backstack.lastOrNull() is Route.JournalEntry }

    init {
        scope.launch {
            var previousDates = emptyList<LocalDate>()
            dates.filterNotNull().collect { dates ->
                if (dates.isEmpty()) {
                    _backstack.removeIf { it is Route.JournalEntry }
                } else {
                    val currentSelectedDate = currentSelectedDate
                    if (currentSelectedDate != null) {
                        if (currentSelectedDate !in dates) {
                            // Useful when a date is reconciled and is no longer going to be visible,
                            // then select the one that took its place
                            val previousIndex = previousDates.indexOf(currentSelectedDate)
                            val newDate =
                                if (previousIndex == -1) {
                                    dates.first()
                                } else {
                                    dates[previousIndex.coerceAtMost(dates.lastIndex)]
                                }
                            selectDate(newDate)
                        }
                    } else if (!initialDateSelected &&
                        (currentDestination is Route.JournalEntryDays || currentDestination is Route.JournalEntry)
                    ) {
                        log { "Selecting initial date" }
                        initialDateSelected = true
                        selectDate(dates.todayIfExistsOrLast())
                    }
                }
                previousDates = dates
            }
        }
    }

    fun navigate(route: Route): Boolean {
        if (route is Route.JournalEntry) {
            openJournalEntry(route)
            return true
        }
        return navigateInternal(route)
    }

    /**
     * Used for deep links. If the date is still not reconciled, show it in the pager, otherwise show
     * it read-only with the entry highlighted.
     */
    private fun openJournalEntry(route: Route.JournalEntry) {
        log { "Open journal entry $route requested" }
        scope.launch {
            val dates = dates.filterNotNull().first()
            if (route.selectedDate in dates) {
                selectDate(route.selectedDate)
            } else {
                navigateInternal(
                    Route.ViewJournalEntryDay(
                        date = route.selectedDate,
                        entryId = route.highlightEntryId,
                    ),
                )
            }
        }
    }

    private fun navigateInternal(route: Route): Boolean {
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

    fun selectDate(localDate: LocalDate?) {
        log { "Select date: $localDate requested" }
        if (localDate == null) {
            if (_backstack.last() is Route.JournalEntry) {
                goBack()
            }
            return
        }
        val dates = dates.value.orEmpty()
        if (localDate !in dates) {
            return
        }
        selectedDate = localDate
        if (_backstack.lastOrNull() !is Route.JournalEntry) {
            navigateInternal(Route.JournalEntry(localDate))
        }
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
