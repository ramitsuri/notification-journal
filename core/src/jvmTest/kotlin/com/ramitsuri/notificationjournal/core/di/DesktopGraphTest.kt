package com.ramitsuri.notificationjournal.core.di

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class DesktopGraphTest {
    private lateinit var tempDir: Path

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("metro_desktop_test")
        System.setProperty("test.datastore.dir", tempDir.toString())
    }

    @After
    fun tearDown() {
        System.clearProperty("test.datastore.dir")
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testDesktopGraphResolution() =
        runBlocking {
            val graph = createGraph<DesktopGraph>()
            assertNotNull(graph.repository)
            assertNotNull(graph.prefManager)
            assertNotNull(graph.lifecycleCoordinator)
            assertSame(graph.repository, graph.repository)

            // Verify ViewModels & Assisted Factories
            assertNotNull(graph.addJournalEntryViewModelFactory)
            assertNotNull(
                graph.addJournalEntryViewModelFactory.create(
                    com.ramitsuri.notificationjournal.core.ui.nav.Route.AddEntry
                        .fromDate(null),
                ),
            )
            assertNotNull(graph.editJournalEntryViewModelFactory)
            assertNotNull(graph.viewJournalEntryDayViewModelFactory)
            assertNotNull(
                graph.viewJournalEntryDayViewModelFactory.create(
                    com.ramitsuri.notificationjournal.core.ui.nav.Route
                        .ViewJournalEntryDay(),
                ),
            )
            assertNotNull(graph.journalEntryViewModelFactory)
            assertNotNull(graph.journalEntryViewModelFactory.create(kotlinx.datetime.LocalDate(2025, 1, 1)))
            assertNotNull(graph.journalEntryDaysViewModel)
            assertNotNull(graph.tagsViewModel)
            assertNotNull(graph.templatesViewModel)
            assertNotNull(graph.logScreenViewModel)
            assertNotNull(graph.inMemoryLogWriter)
            graph.initLogger()
            assertNotNull(graph.searchViewModel)
            assertNotNull(graph.importViewModel)
            assertNotNull(graph.settingsViewModel)

            graph.coroutineScope.cancel()
        }
}
