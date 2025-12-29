package com.ramitsuri.notificationjournal.core.ui.nav

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    private lateinit var navigator: Navigator

    @Test
    fun `Initial state with default start route`() {
        initialize()

        assertEquals(1, navigator.backstack.size)
        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.backstack[0])
    }

    @Test
    fun `Initial state with custom start route`() {
        initialize(topOfBackStack = Route.Tags)

        assertEquals(2, navigator.backstack.size)
        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.backstack[0])
        assertEquals(Route.Tags, navigator.backstack[1])
    }

    @Test
    fun `Initial state with Home as start route`() {
        initialize(topOfBackStack = Route.JournalEntry(LocalDate.parse("2025-12-25")))

        assertEquals(1, navigator.backstack.size)
        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.backstack[0])
    }

    @Test
    fun `getBackstack returns correct list`() {
        initialize(topOfBackStack = Route.Tags)
        val backstack = navigator.backstack

        assertEquals(listOf(Route.JournalEntry(LocalDate.parse("2025-12-25")), Route.Tags), backstack)
    }

    @Test
    fun `getCurrentDestination initial state`() {
        initialize()

        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.currentDestination)
    }

    @Test
    fun `getCurrentDestination initial state with non null start route`() {
        initialize(topOfBackStack = Route.Tags)

        assertEquals(Route.Tags, navigator.currentDestination)
    }

    @Test
    fun `getCurrentDestination after navigation`() {
        initialize()

        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.currentDestination)
        navigator.navigate(Route.Tags)
        assertEquals(Route.Tags, navigator.currentDestination)
        navigator.goBack()
        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.currentDestination)
    }

    @Test
    fun `navigate to a new TopLevelRoute removes everything but Home`() {
        initialize()
        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Tags)
        navigator.navigate(Route.Import)

        assertEquals(
            listOf(
                Route.JournalEntry(LocalDate.parse("2025-12-25")),
                Route.Templates,
                Route.Tags,
                Route.Import,
            ),
            navigator.backstack,
        )
    }

    @Test
    fun `navigate to a new TopLevelRoute multiple times`() {
        initialize()

        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Import)
        navigator.navigate(Route.Templates)

        assertEquals(listOf(Route.JournalEntry(LocalDate.parse("2025-12-25")), Route.Templates), navigator.backstack)
    }

    @Test
    fun `navigate to an existing non TopLevelRoute`() {
        initialize()

        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Tags)
        navigator.navigate(Route.ViewJournalEntryDay(LocalDate.parse("2025-12-12"), ""))
        navigator.navigate(Route.Tags)

        assertEquals(
            listOf(Route.JournalEntry(LocalDate.parse("2025-12-25")), Route.Templates, Route.Tags),
            navigator.backstack,
        )
    }

    @Test
    fun `navigate to the current destination returns false`() {
        initialize()

        navigator.navigate(Route.Templates)
        val navigated = navigator.navigate(Route.Templates)

        assertFalse(navigated)
    }

    @Test
    fun `navigate to Home as a TopLevelRoute, everything should be cleared`() {
        initialize()

        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Tags)
        navigator.navigate(Route.ViewJournalEntryDay(LocalDate.parse("2025-12-12"), ""))
        navigator.navigate(Route.JournalEntry(LocalDate.parse("2025-12-25")))

        assertEquals(listOf(Route.JournalEntry(LocalDate.parse("2025-12-25"))), navigator.backstack)
    }

    @Test
    fun `navigate to an existing route deep in stack`() {
        // Navigate to a TopLevelRoute that is already in the backstack but not at the top.
        initialize()

        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Tags)
        navigator.navigate(Route.ViewJournalEntryDay(LocalDate.parse("2025-12-12"), ""))
        navigator.navigate(Route.AddEntry.fromDate(LocalDate.parse("2025-12-12")))
        navigator.navigate(Route.Templates)

        assertEquals(listOf(Route.JournalEntry(LocalDate.parse("2025-12-25")), Route.Templates), navigator.backstack)
    }

    @Suppress("JoinDeclarationAndAssignment")
    @Test
    fun `navigate returns true on success`() {
        initialize()

        var navigated: Boolean

        navigated = navigator.navigate(Route.Tags)
        assertTrue(navigated)

        navigated = navigator.navigate(Route.ViewJournalEntryDay(LocalDate.parse("2025-12-12"), ""))
        assertTrue(navigated)

        navigated = navigator.navigate(Route.JournalEntry(LocalDate.parse("2025-12-25")))
        assertTrue(navigated)

        navigated = navigator.navigate(Route.Templates)
        assertTrue(navigated)

        navigated = navigator.navigate(Route.Templates)
        assertFalse(navigated)
    }

    @Test
    fun `goBack from a multi entry backstack`() {
        initialize()

        navigator.navigate(Route.JournalEntry(LocalDate.parse("2025-12-25")))
        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Tags)
        navigator.navigate(Route.Settings)
        navigator.navigate(Route.ViewJournalEntryDay(LocalDate.parse("2025-12-12"), ""))
        navigator.navigate(Route.Logs)

        navigator.goBack()
        assertEquals(Route.ViewJournalEntryDay(LocalDate.parse("2025-12-12"), ""), navigator.currentDestination)
        navigator.goBack()
        assertEquals(Route.Settings, navigator.currentDestination)
        navigator.goBack()
        assertEquals(Route.Tags, navigator.currentDestination)
        navigator.goBack()
        assertEquals(Route.Templates, navigator.currentDestination)
        navigator.goBack()
        assertEquals(Route.JournalEntry(LocalDate.parse("2025-12-25")), navigator.currentDestination)
    }

    @Test
    fun `goBack not allowed when backstack has one entry`() {
        initialize()

        navigator.goBack()
        assertEquals(1, navigator.backstack.size)
    }

    @Test
    fun `goBack on an empty backstack`() {
        initialize()

        navigator.goBack()
        navigator.goBack()
        assertEquals(1, navigator.backstack.size)
    }

    @Test
    fun `Chained navigations and goBack calls`() {
        initialize()

        navigator.navigate(Route.Templates)
        navigator.navigate(Route.Tags)
        navigator.navigate(Route.Import)
        navigator.navigate(Route.Search)
        navigator.goBack()
        navigator.navigate(Route.Settings)
        navigator.goBack()
        navigator.navigate(Route.EditEntry(""))
        navigator.navigate(Route.AddEntry.fromDate(LocalDate.parse("2025-12-12")))

        assertEquals(
            listOf(
                Route.JournalEntry(LocalDate.parse("2025-12-25")),
                Route.Templates,
                Route.Tags,
                Route.Import,
                Route.EditEntry(""),
                Route.AddEntry.fromDate(LocalDate.parse("2025-12-12")),
            ),
            navigator.backstack,
        )
    }

    @Test
    fun `Navigation to same route with different args`() {
        navigator = Navigator(topOfBackStack = Route.JournalEntry(LocalDate.parse("2025-12-25")))

        navigator.navigate(Route.JournalEntry(LocalDate.parse("2025-12-26")))
        navigator.navigate(Route.JournalEntry(LocalDate.parse("2025-12-27")))
        navigator.navigate(Route.JournalEntry(LocalDate.parse("2025-12-28")))

        assertEquals(
            listOf(
                Route.JournalEntryDays,
                Route.JournalEntry(LocalDate.parse("2025-12-28")),
            ),
            navigator.backstack,
        )
    }

    private fun initialize(
        topOfBackStack: Route? = null,
        home: Route = Route.JournalEntry(LocalDate.parse("2025-12-25")),
    ) {
        navigator = Navigator(topOfBackStack = topOfBackStack, home = home)
    }
}
