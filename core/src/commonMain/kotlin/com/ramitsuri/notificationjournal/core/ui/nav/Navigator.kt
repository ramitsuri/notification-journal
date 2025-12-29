package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack

class Navigator(
    topOfBackStack: Route? = null,
    home: Route = Route.JournalEntryDays,
) {
    private val _backstack = NavBackStack(setOfNotNull(home, topOfBackStack).toMutableStateList())
    val backstack: List<Route>
        get() = _backstack

    val currentDestination: Route
        get() = _backstack.last()

    fun navigate(route: Route): Boolean {
        if (_backstack.last() == route) {
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
}
